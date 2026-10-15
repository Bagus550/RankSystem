package id.bagus.rank;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AutoRankService implements Listener {

    private final RankPlugin plugin;
    private final RankManager manager;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<UUID, Long> expEarned = new HashMap<>();
    private final File file;
    private boolean dirty;
    private BukkitTask task;

    public AutoRankService(RankPlugin plugin, RankManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.file = new File(plugin.getDataFolder(), "progress.yml");
        load();
    }

    /** Jalankan (atau jadwalkan ulang setelah /rank reload) pengecekan berkala. */
    public void start() {
        if (task != null) task.cancel();
        task = null;
        if (!plugin.getConfig().getBoolean("auto-rankup.enabled", true)) return;

        long seconds = Math.max(10, plugin.getConfig().getLong("auto-rankup.check-interval-seconds", 60));
        long period = 20L * seconds;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::checkAll, period, period);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        task = null;
        save();
    }

    // ------------------------------------------------------------ pencatatan exp

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExp(PlayerExpChangeEvent e) {
        int amount = e.getAmount();
        if (amount <= 0) return;
        expEarned.merge(e.getPlayer().getUniqueId(), (long) amount, Long::sum);
        dirty = true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (dirty) save();
    }

    // ------------------------------------------------------------ pengecekan rank

    private double playtimeHours(Player p) {
        // Statistik bawaan Minecraft, satuannya tick (20 tick = 1 detik)
        return p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000.0;
    }

    private long expOf(Player p) {
        return expEarned.getOrDefault(p.getUniqueId(), 0L);
    }

    private void checkAll() {
        for (Player p : plugin.getServer().getOnlinePlayers()) check(p);
        if (dirty) save();
    }

    private void check(Player p) {
        Rank current = manager.getRank(p.getUniqueId());
        int currentWeight = current == null ? 0 : current.weight();
        double hours = playtimeHours(p);
        long exp = expOf(p);

        Rank best = null;
        for (Rank r : manager.getRanks()) {
            if (!r.isAutoRank() || r.weight() <= currentWeight) continue;
            if (hours < r.playtimeHours() || exp < r.exp()) continue;
            if (best == null || r.weight() > best.weight()) best = r;
        }
        if (best != null) promote(p, best);
    }

    private void promote(Player p, Rank rank) {
        manager.setRank(p.getUniqueId(), rank);
        manager.apply(p);

        var player = Placeholder.unparsed("player", p.getName());
        var rankName = Placeholder.unparsed("rank", rank.displayName());

        String msg = plugin.getConfig().getString("auto-rankup.message",
                "<green>Selamat! Rank kamu naik menjadi <rank>.");
        p.sendMessage(mm.deserialize(msg, player, rankName));

        if (plugin.getConfig().getBoolean("auto-rankup.broadcast-enabled", true)) {
            String bc = plugin.getConfig().getString("auto-rankup.broadcast",
                    "<player> naik rank menjadi <rank>!");
            plugin.getServer().broadcast(mm.deserialize(bc, player, rankName));
        }
    }

    // ------------------------------------------------------------ /progress

    public void sendProgress(Player p) {
        Rank current = manager.getRank(p.getUniqueId());
        int currentWeight = current == null ? 0 : current.weight();

        Rank next = manager.getRanks().stream()
                .filter(r -> r.isAutoRank() && r.weight() > currentWeight)
                .min(Comparator.comparingInt(Rank::weight))
                .orElse(null);

        if (next == null) {
            p.sendMessage(mm.deserialize("<gray>Tidak ada rank berikutnya yang bisa dicapai otomatis."));
            return;
        }

        String hoursNow = String.format(Locale.ROOT, "%.1f", playtimeHours(p));
        String hoursNeed = String.format(Locale.ROOT, "%.1f", next.playtimeHours());

        p.sendMessage(mm.deserialize("<yellow>Progres menuju <white><rank></white>:",
                Placeholder.unparsed("rank", next.displayName())));
        p.sendMessage(mm.deserialize("<gray> Waktu bermain: <white><now></white> / <need> jam",
                Placeholder.unparsed("now", hoursNow), Placeholder.unparsed("need", hoursNeed)));
        p.sendMessage(mm.deserialize("<gray> Exp diperoleh: <white><now></white> / <need>",
                Placeholder.unparsed("now", String.valueOf(expOf(p))),
                Placeholder.unparsed("need", String.valueOf(next.exp()))));
    }

    // ------------------------------------------------------------ penyimpanan

    private void load() {
        expEarned.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        for (String key : y.getKeys(false)) {
            try {
                expEarned.put(UUID.fromString(key), y.getLong(key));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        expEarned.forEach((uuid, value) -> y.set(uuid.toString(), value));
        try {
            y.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan progress.yml: " + e.getMessage());
        }
    }
}