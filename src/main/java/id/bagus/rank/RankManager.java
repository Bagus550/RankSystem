package id.bagus.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RankManager {

    private final RankPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, Rank> ranks = new LinkedHashMap<>();
    private final Map<UUID, String> playerRanks = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final File dataFile;
    private String defaultRank;

    public RankManager(RankPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        plugin.reloadConfig();
        ranks.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("ranks");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(id);
                if (s == null) continue;
                String key = id.toLowerCase();
                ranks.put(key, new Rank(key,
                        s.getString("display-name", id),
                        s.getString("prefix", ""),
                        s.getInt("weight", 0),
                        s.getStringList("permissions"),
                        s.getStringList("inherits")));
            }
        }
        defaultRank = plugin.getConfig().getString("default-rank", "member").toLowerCase();
        if (!ranks.containsKey(defaultRank) && !ranks.isEmpty()) {
            defaultRank = ranks.keySet().iterator().next();
        }

        playerRanks.clear();
        if (dataFile.exists()) {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            for (String uuid : data.getKeys(false)) {
                try {
                    playerRanks.put(UUID.fromString(uuid), data.getString(uuid));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        playerRanks.forEach((uuid, rank) -> data.set(uuid.toString(), rank));
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan data.yml: " + e.getMessage());
        }
    }

    public Collection<Rank> getRanks() { return ranks.values(); }

    public Rank getRank(String id) { return ranks.get(id.toLowerCase()); }

    public Rank getRank(UUID uuid) {
        Rank rank = ranks.get(playerRanks.getOrDefault(uuid, defaultRank));
        return rank != null ? rank : ranks.get(defaultRank);
    }

    public void setRank(UUID uuid, Rank rank) {
        playerRanks.put(uuid, rank.id());
        save();
    }

    public void resetRank(UUID uuid) {
        playerRanks.remove(uuid);
        save();
    }

    /** Gabungkan permission rank beserta inherit-nya (inherit dulu, lalu milik sendiri). */
    private Map<String, Boolean> resolvePermissions(Rank rank, Set<String> visited) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        if (rank == null || !visited.add(rank.id())) return result; // cegah loop
        for (String parent : rank.inherits()) {
            result.putAll(resolvePermissions(ranks.get(parent.toLowerCase()), visited));
        }
        for (String perm : rank.permissions()) {
            if (perm.startsWith("-")) result.put(perm.substring(1), false);
            else result.put(perm, true);
        }
        return result;
    }

    public void apply(Player player) {
        remove(player);
        Rank rank = getRank(player.getUniqueId());
        if (rank == null) return;

        PermissionAttachment attachment = player.addAttachment(plugin);
        resolvePermissions(rank, new HashSet<>()).forEach(attachment::setPermission);
        attachments.put(player.getUniqueId(), attachment);

        if (plugin.getConfig().getBoolean("tablist.enabled", true)) {
            player.playerListName(mm.deserialize(rank.prefix())
                    .append(Component.space())
                    .append(Component.text(player.getName())));
        }
    }

    public void remove(Player player) {
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) player.removeAttachment(old);
    }

    public void applyAll() {
        plugin.getServer().getOnlinePlayers().forEach(this::apply);
    }
}