package id.bagus.rank;

import org.bukkit.plugin.java.JavaPlugin;

public class RankPlugin extends JavaPlugin {

    private RankManager manager;
    private AutoRankService autoRank;
    private Messages messages;

    public AutoRankService getAutoRank() { return autoRank; }
    public Messages getMessages() { return messages; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new RankManager(this);
        manager.load();
        messages = new Messages(this);
        messages.load();

        getServer().getPluginManager().registerEvents(new RankListener(this, manager), this);

        RankCommand command = new RankCommand(this, manager);
        getCommand("rank").setExecutor(command);
        getCommand("rank").setTabCompleter(command);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new RankExpansion(this, manager).register();
            getLogger().info("PlaceholderAPI detected, %ranksystem_*% placeholders enabled.");
        }
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            VaultHook.register(this, manager);
            getLogger().info("Vault detected, Chat/Permission providers registered.");
        }
        autoRank = new AutoRankService(this, manager);
        getServer().getPluginManager().registerEvents(autoRank, this);
        autoRank.start();

        getCommand("progress").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player p) {
                autoRank.sendProgress(p);
            } else {
                messages.send(sender, "command.player-only");
            }
            return true;
        });
        manager.applyAll(); // untuk kasus /reload
    }

    @Override
    public void onDisable() {
        getServer().getOnlinePlayers().forEach(p -> manager.remove(p)); autoRank.shutdown();
    }
}