package id.bagus.rank;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final RankPlugin plugin;
    private final RankManager manager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public RankCommand(RankPlugin plugin, RankManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private void msg(CommandSender to, String key, String playerName, String rankName) {
        String raw = plugin.getConfig().getString("messages." + key, key);
        to.sendMessage(mm.deserialize(raw,
                Placeholder.unparsed("player", playerName == null ? "" : playerName),
                Placeholder.unparsed("rank", rankName == null ? "" : rankName)));
    }

    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayerIfCached(name);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ranks.admin")) {
            msg(sender, "no-permission", null, null);
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("/rank <set|remove|info|list|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                manager.load();
                manager.applyAll();
                plugin.getAutoRank().start();
                msg(sender, "reloaded", null, null);
            }
            case "list" -> {
                sender.sendMessage("Rank tersedia:");
                manager.getRanks().stream()
                        .sorted(Comparator.comparingInt(Rank::weight).reversed())
                        .forEach(r -> sender.sendMessage(mm.deserialize(
                                r.prefix() + " <gray>(" + r.id() + ", weight " + r.weight() + ")")));
            }
            case "set" -> {
                if (args.length < 3) { sender.sendMessage("/rank set <pemain> <rank>"); return true; }
                OfflinePlayer target = findPlayer(args[1]);
                Rank rank = manager.getRank(args[2]);
                if (target == null) { msg(sender, "unknown-player", args[1], null); return true; }
                if (rank == null) { msg(sender, "unknown-rank", args[1], args[2]); return true; }
                manager.setRank(target.getUniqueId(), rank);
                if (target.getPlayer() != null) manager.apply(target.getPlayer());
                msg(sender, "rank-set", args[1], rank.displayName());
            }
            case "remove" -> {
                if (args.length < 2) { sender.sendMessage("/rank remove <pemain>"); return true; }
                OfflinePlayer target = findPlayer(args[1]);
                if (target == null) { msg(sender, "unknown-player", args[1], null); return true; }
                manager.resetRank(target.getUniqueId());
                if (target.getPlayer() != null) manager.apply(target.getPlayer());
                msg(sender, "rank-removed", args[1], null);
            }
            case "info" -> {
                if (args.length < 2) { sender.sendMessage("/rank info <pemain>"); return true; }
                OfflinePlayer target = findPlayer(args[1]);
                if (target == null) { msg(sender, "unknown-player", args[1], null); return true; }
                Rank rank = manager.getRank(target.getUniqueId());
                sender.sendMessage(mm.deserialize("<gray>" + args[1] + ": </gray>" + rank.prefix()));
            }
            case "preview" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage("/rank preview <rank>  (hanya untuk pemain)");
                    return true;
                }
                Rank rank = manager.getRank(args[1]);
                if (rank == null) { msg(sender, "unknown-rank", null, args[1]); return true; }
                plugin.getAutoRank().celebrate(player, rank);
            }
            default -> sender.sendMessage("/rank <set|remove|info|list|reload>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return List.of("set", "remove", "info", "list", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("reload")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return manager.getRanks().stream().map(Rank::id)
                    .filter(id -> id.startsWith(args[2].toLowerCase())).toList();
        }
        return List.of();
    }
}