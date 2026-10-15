package id.bagus.rank;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

import java.util.Arrays;

public class VaultHook {

    public static void register(RankPlugin plugin, RankManager manager) {
        RankPermission perms = new RankPermission(plugin, manager);
        RankChat chat = new RankChat(perms, manager);
        var services = plugin.getServer().getServicesManager();
        services.register(Permission.class, perms, plugin, ServicePriority.Normal);
        services.register(Chat.class, chat, plugin, ServicePriority.High);
    }

    // ---------------------------------------------------------------- Permission
    static class RankPermission extends Permission {
        private final RankManager manager;

        RankPermission(RankPlugin plugin, RankManager manager) {
            this.plugin = plugin;
            this.manager = manager;
        }

        @Override public String getName() { return "RankSystem"; }
        @Override public boolean isEnabled() { return true; }
        @Override public boolean hasSuperPermsCompat() { return true; }
        @Override public boolean hasGroupSupport() { return true; }

        @Override public boolean playerHas(String world, String player, String perm) {
            Player p = Bukkit.getPlayerExact(player);
            return p != null && p.hasPermission(perm);
        }

        // Pengubahan permission dikelola lewat config.yml, bukan lewat Vault
        @Override public boolean playerAdd(String w, String p, String perm) { return false; }
        @Override public boolean playerRemove(String w, String p, String perm) { return false; }
        @Override public boolean groupHas(String w, String g, String perm) { return false; }
        @Override public boolean groupAdd(String w, String g, String perm) { return false; }
        @Override public boolean groupRemove(String w, String g, String perm) { return false; }
        @Override public boolean playerAddGroup(String w, String p, String g) { return false; }
        @Override public boolean playerRemoveGroup(String w, String p, String g) { return false; }

        @Override public boolean playerInGroup(String world, String player, String group) {
            Rank rank = manager.getRankByName(player);
            return rank != null && rank.id().equalsIgnoreCase(group);
        }

        @Override public String[] getPlayerGroups(String world, String player) {
            Rank rank = manager.getRankByName(player);
            return rank == null ? new String[0] : new String[]{rank.id()};
        }

        @Override public String getPrimaryGroup(String world, String player) {
            Rank rank = manager.getRankByName(player);
            return rank == null ? null : rank.id();
        }

        @Override public String[] getGroups() {
            return manager.getRanks().stream().map(Rank::id).toArray(String[]::new);
        }
    }

    // ---------------------------------------------------------------- Chat
    static class RankChat extends Chat {
        private final RankManager manager;

        RankChat(Permission perms, RankManager manager) {
            super(perms);
            this.manager = manager;
        }

        @Override public String getName() { return "RankSystem"; }
        @Override public boolean isEnabled() { return true; }

        @Override public String getPlayerPrefix(String world, String player) {
            Rank rank = manager.getRankByName(player);
            return rank == null ? "" : manager.legacyPrefix(rank) + " ";
        }

        @Override public String getGroupPrefix(String world, String group) {
            Rank rank = manager.getRank(group);
            return rank == null ? "" : manager.legacyPrefix(rank) + " ";
        }

        @Override public String getPlayerSuffix(String world, String player) { return ""; }
        @Override public String getGroupSuffix(String world, String group) { return ""; }

        // Setter tidak didukung: semua diatur lewat config.yml
        @Override public void setPlayerPrefix(String w, String p, String v) {}
        @Override public void setPlayerSuffix(String w, String p, String v) {}
        @Override public void setGroupPrefix(String w, String g, String v) {}
        @Override public void setGroupSuffix(String w, String g, String v) {}

        // Info node tidak dipakai, kembalikan nilai default
        @Override public int getPlayerInfoInteger(String w, String p, String n, int d) { return d; }
        @Override public void setPlayerInfoInteger(String w, String p, String n, int v) {}
        @Override public int getGroupInfoInteger(String w, String g, String n, int d) { return d; }
        @Override public void setGroupInfoInteger(String w, String g, String n, int v) {}
        @Override public double getPlayerInfoDouble(String w, String p, String n, double d) { return d; }
        @Override public void setPlayerInfoDouble(String w, String p, String n, double v) {}
        @Override public double getGroupInfoDouble(String w, String g, String n, double d) { return d; }
        @Override public void setGroupInfoDouble(String w, String g, String n, double v) {}
        @Override public boolean getPlayerInfoBoolean(String w, String p, String n, boolean d) { return d; }
        @Override public void setPlayerInfoBoolean(String w, String p, String n, boolean v) {}
        @Override public boolean getGroupInfoBoolean(String w, String g, String n, boolean d) { return d; }
        @Override public void setGroupInfoBoolean(String w, String g, String n, boolean v) {}
        @Override public String getPlayerInfoString(String w, String p, String n, String d) { return d; }
        @Override public void setPlayerInfoString(String w, String p, String n, String v) {}
        @Override public String getGroupInfoString(String w, String g, String n, String d) { return d; }
        @Override public void setGroupInfoString(String w, String g, String n, String v) {}
    }
}