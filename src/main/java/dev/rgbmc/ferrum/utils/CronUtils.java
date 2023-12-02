package dev.rgbmc.ferrum.utils;

import dev.rgbmc.ferrum.Ferrum;
import it.sauronsoftware.cron4j.Scheduler;

public class CronUtils {
    private static final Scheduler scheduler = new Scheduler();
    private static String taskId = null;

    public static void startSchedule(String cron) {
        stopSchedule();
        Ferrum.instance.getLogger().info("Cron Task Scheduled [" + cron + " +] TimeZone: " + scheduler.getTimeZone().getDisplayName());
        taskId = scheduler.schedule(cron, () -> {
            Ferrum.instance.getLogger().info("Cron Task Running");
            if (!BackupUtils.startBackup(false, null)) {
                Ferrum.instance.getLogger().severe("Unable to execute Cron task, there is already a backup task in progress");
            }
        });
        scheduler.start();
    }

    public static void stopSchedule() {
        if (taskId != null) {
            scheduler.stop();
            scheduler.deschedule(taskId);
            taskId = null;
        }
    }
}
