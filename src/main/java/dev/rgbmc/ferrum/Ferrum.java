package dev.rgbmc.ferrum;

import dev.rgbmc.ferrum.utils.CronUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class Ferrum extends JavaPlugin {

    public static Ferrum instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        CronUtils.startSchedule(getConfig().getString("auto-backup.cron"));
        // Plugin startup logic

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
