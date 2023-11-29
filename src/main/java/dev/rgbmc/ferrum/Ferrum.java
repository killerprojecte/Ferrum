package dev.rgbmc.ferrum;

import dev.rgbmc.ferrum.commands.FerrumCommand;
import dev.rgbmc.ferrum.tasks.TaskManager;
import dev.rgbmc.ferrum.utils.CronUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class Ferrum extends JavaPlugin {

    public static Ferrum instance;
    public static TaskManager taskManager;

    @Override
    public void onEnable() {
        instance = this;
        taskManager = new TaskManager();
        saveDefaultConfig();
        reloadCron();
        getCommand("ferrum").setExecutor(new FerrumCommand());
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void reloadCron() {
        CronUtils.stopSchedule();
        CronUtils.startSchedule(getConfig().getString("auto-backup.cron"));
    }
}
