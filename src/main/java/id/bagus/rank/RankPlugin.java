package id.bagus.rank;

import org.bukkit.plugin.java.JavaPlugin;

public class RankPlugin extends JavaPlugin {

    private RankManager manager;
    private AutoRankService autoRank;

    public AutoRankService getAutoRank() { return autoRank; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new RankManager(this);
        manager.load();

        getServer().getPluginManager().registerEvents(new RankListener(this, manager), this);

        RankCommand command = new RankCommand(this, manager);
        getCommand("rank").setExecutor(command);
        getCommand("rank").setTabCompleter(command);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new RankExpansion(this, manager).register();
            getLogger().info("PlaceholderAPI terdeteksi, placeholder %ranksystem_*% aktif.");
        }
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            VaultHook.register(this, manager);
            getLogger().info("Vault terdeteksi, provider Chat/Permission terdaftar.");
        }
        autoRank = new AutoRankService(this, manager);
        getServer().getPluginManager().registerEvents(autoRank, this);
        autoRank.start();

        getCommand("progress").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player p) {
                autoRank.sendProgress(p);
            } else {
                sender.sendMessage("Command ini hanya untuk pemain.");
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