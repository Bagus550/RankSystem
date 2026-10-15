package id.bagus.rank;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class RankExpansion extends PlaceholderExpansion {

    private final RankPlugin plugin;
    private final RankManager manager;

    public RankExpansion(RankPlugin plugin, RankManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override public String getIdentifier() { return "ranksystem"; }
    @Override public String getAuthor() { return String.join(", ", plugin.getPluginMeta().getAuthors()); }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; } // tetap terdaftar saat /papi reload

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";
        Rank rank = manager.getRank(player.getUniqueId());
        if (rank == null) return "";

        return switch (params.toLowerCase()) {
            case "prefix" -> manager.legacyPrefix(rank);   // format &-code
            case "prefix_mm" -> rank.prefix();             // MiniMessage mentah
            case "rank" -> rank.displayName();
            case "rank_id" -> rank.id();
            case "weight" -> String.valueOf(rank.weight());
            default -> null;
        };
    }
}