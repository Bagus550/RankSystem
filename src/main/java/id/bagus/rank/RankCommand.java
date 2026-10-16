package id.bagus.rank;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

public class RankCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            List.of("set", "remove", "info", "list", "reload", "preview");

    private final RankPlugin plugin;
    private final RankManager manager;

    public RankCommand(RankPlugin plugin, RankManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private Messages lang() { return plugin.getMessages(); }

    private static TagResolver playerTag(String name) { return Placeholder.unparsed("player", name); }
    private static TagResolver rankTag(String name) { return Placeholder.unparsed("rank", name); }

    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayerIfCached(name);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("ranks.admin")) {
            lang().send(sender, "command.no-permission");
            return true;
        }
        if (args.length == 0) {
            lang().send(sender, "command.usage.main");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                manager.load();
                lang().load();
                manager.applyAll();
                plugin.getAutoRank().start();
                lang().send(sender, "command.reloaded");
            }
            case "list" -> {
                lang().send(sender, "command.list.header");
                manager.getRanks().stream()
                        .sorted(Comparator.comparingInt(Rank::weight).reversed())
                        .forEach(r -> lang().send(sender, "command.list.entry",
                                Placeholder.parsed("prefix", manager.prefixNow(r)),
                                Placeholder.unparsed("id", r.id()),
                                Placeholder.unparsed("weight", String.valueOf(r.weight()))));
            }
            case "set" -> {
                if (args.length < 3) { lang().send(sender, "command.usage.set"); return true; }
                OfflinePlayer target = findPlayer(args[1]);
                Rank rank = manager.getRank(args[2]);
                if (target == null) {
                    lang().send(sender, "command.unknown-player", playerTag(args[1]));
                    return true;
                }
                if (rank == null) {
                    lang().send(sender, "command.unknown-rank", rankTag(args[2]));
                    return true;
                }
                manager.setRank(target.getUniqueId(), rank);
                if (target.getPlayer() != null) manager.apply(target.getPlayer());
                lang().send(sender, "command.rank-set", playerTag(args[1]), rankTag(rank.displayName()));
            }
            case "remove" -> {
                if (args.length < 2) { lang().send(sender, "command.usage.remove"); return true; }
                OfflinePlayer target = findPlayer(args[1]);
                if (target == null) {
                    lang().send(sender, "command.unknown-player", playerTag(args[1]));
                    return true;
                }
                manager.resetRank(target.getUniqueId());
                if (target.getPlayer() != null) manager.apply(target.getPlayer());
                lang().send(sender, "command.rank-removed", playerTag(args[1]));
            }
            case "info" -> {
                if (args.length < 2) { lang().send(sender, "command.usage.info"); return true; }
                OfflinePlayer target = findPlayer(args[1]);
                if (target == null) {
                    lang().send(sender, "command.unknown-player", playerTag(args[1]));
                    return true;
                }
                Rank rank = manager.getRank(target.getUniqueId());
                lang().send(sender, "command.info.line", playerTag(args[1]),
                        Placeholder.parsed("prefix", manager.prefixNow(rank)));
            }
            case "preview" -> {
                if (!(sender instanceof Player player)) {
                    lang().send(sender, "command.player-only");
                    return true;
                }
                if (args.length < 2) { lang().send(sender, "command.usage.preview"); return true; }
                Rank rank = manager.getRank(args[1]);
                if (rank == null) {
                    lang().send(sender, "command.unknown-rank", rankTag(args[1]));
                    return true;
                }
                plugin.getAutoRank().celebrate(player, rank);
            }
            default -> lang().send(sender, "command.usage.main");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2 && List.of("set", "remove", "info").contains(sub)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).toList();
        }
        boolean rankArg = (sub.equals("set") && args.length == 3) || (sub.equals("preview") && args.length == 2);
        if (rankArg) {
            String typed = args[args.length - 1].toLowerCase();
            return manager.getRanks().stream().map(Rank::id)
                    .filter(id -> id.startsWith(typed)).toList();
        }
        return List.of();
    }
}