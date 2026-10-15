package id.bagus.rank;

import org.bukkit.plugin.java.JavaPlugin;

public class RankPlugin extends JavaPlugin {

    private RankManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new RankManager(this);
        manager.load();

        getServer().getPluginManager().registerEvents(new RankListener(this, manager), this);

        RankCommand command = new RankCommand(this, manager);
        getCommand("rank").setExecutor(command);
        getCommand("rank").setTabCompleter(command);

        manager.applyAll(); // untuk kasus /reload
    }

    @Override
    public void onDisable() {
        getServer().getOnlinePlayers().forEach(p -> manager.remove(p));
    }
}