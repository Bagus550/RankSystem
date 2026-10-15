package id.bagus.rank;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class RankListener implements Listener {

    private final RankPlugin plugin;
    private final RankManager manager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public RankListener(RankPlugin plugin, RankManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        manager.apply(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.remove(e.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        if (!plugin.getConfig().getBoolean("chat.enabled", true)) return;
        Rank rank = manager.getRank(e.getPlayer().getUniqueId());
        if (rank == null) return;

        String format = plugin.getConfig().getString("chat.format",
                "<prefix> <player>: <message>");

        e.renderer((source, displayName, message, viewer) -> mm.deserialize(format,
                Placeholder.parsed("prefix", manager.prefixNow(rank)),
                Placeholder.component("player", displayName),
                Placeholder.component("message", message)));
    }
}