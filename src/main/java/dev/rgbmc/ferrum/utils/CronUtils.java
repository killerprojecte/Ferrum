package dev.rgbmc.ferrum.utils;

import dev.rgbmc.ferrum.Ferrum;
import it.sauronsoftware.cron4j.Scheduler;

public class CronUtils {
    private static final Scheduler scheduler = new Scheduler();
    private static String taskId = null;

    public static void startSchedule(String cron) {
        if (taskId != null) {
            scheduler.deschedule(taskId);
        }
        taskId = scheduler.schedule(cron, () -> {
            if (!BackupUtils.startBackup(false, null)) {
                Ferrum.instance.getLogger().severe("Unable to execute Cron task, there is already a backup task in progress");
            }
        });
    }

    public static void stopSchedule() {
        if (taskId != null) {
            scheduler.deschedule(taskId);
        }
    }
}
