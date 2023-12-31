package dev.rgbmc.ferrum.utils;

import dev.rgbmc.ferrum.Ferrum;
import dev.rgbmc.ferrum.api.Backup;
import dev.rgbmc.ferrum.api.objects.ResultInfo;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BackupUtils {

    private static boolean locked = false;

    public static boolean startBackup(boolean fromCommand, CommandSender sender) {
        if (locked) {
            Ferrum.instance.getLogger().warning("There is already a backup task in progress.");
            Ferrum.instance.getLogger().warning("Please wait for the task to complete before running a new backup task.");
            return false;
        }
        locked = true;

        ConfigurationSection section = Ferrum.instance.getConfig().getConfigurationSection("backups");

        File workingPath = new File(System.getProperty("user.dir"));
        File targetFile = getTargetFile();
        if (!fromCommand) {
            File folder = new File(Objects.requireNonNull(Objects.requireNonNull(section).getString("save-path")));
            List<File> files = Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                    .filter(file -> file.getName().endsWith(".zip"))
                    .sorted((o1, o2) -> {
                        try {
                            long t1 = Files.readAttributes(o1.toPath(), BasicFileAttributes.class).creationTime().toMillis();
                            long t2 = Files.readAttributes(o2.toPath(), BasicFileAttributes.class).creationTime().toMillis();
                            if (t1 > t2) return 1;
                            if (t1 < t2) return -1;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());
            int maxBackups = Ferrum.instance.getConfig().getInt("auto-backup.max-backups");
            if (files.size() > maxBackups) {
                for (int i = 0; i < (files.size() - maxBackups); i++) {
                    File deleteFile = files.get(i);
                    if (!deleteFile.delete()) Backup.logger.warn("Cannot delete file: " + deleteFile.getPath());
                }
            }
        }
        Backup backup = new Backup(targetFile, workingPath.toPath(), Objects.requireNonNull(Objects.requireNonNull(section).getString("save-path")).replace("/", "\\"));
        if (section.getBoolean("incremental")) {
            backup.setIncremental(true);
        }
        if (section.getBoolean("encrypt")) {
            backup.setEncrypt(true);
            backup.setEncryptionMethod(EncryptionMethod.valueOf(section.getString("encrypt-method")));
        }
        backup.setIgnores(section.getStringList("ignores").stream().map(s -> s.replace("/", "\\")).collect(Collectors.toList()));
        for (CompressionLevel value : CompressionLevel.values()) {
            if (value.getLevel() == section.getInt("compression-level")) {
                backup.setCompressionLevel(value);
                break;
            }
        }
        String password = section.getString("password", "");
        if (password != null && !password.isEmpty()) {
            backup.setPassword(password);
        }
        Backup.submitTask(() -> {
            Instant startTime = Instant.now();
            ResultInfo resultInfo = backup.startBackup();
            locked = false;
            CommandSender output;
            if (fromCommand) {
                output = sender;
            } else {
                output = Bukkit.getConsoleSender();
            }
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            output.sendMessage(Color.color("&aBackup Task Finished! &7(&eTime Used: " + getBetweenTime(duration.getSeconds()) + "&7)"));
            Ferrum.taskManager.parseExpressions(section.getStringList("finished-tasks"), resultInfo, backup);
        });
        return true;
    }

    private static File getTargetFile() {
        ConfigurationSection section = Ferrum.instance.getConfig().getConfigurationSection("backups");
        File folder = new File(Objects.requireNonNull(Objects.requireNonNull(section).getString("save-path")));
        if (!folder.mkdirs()) Backup.logger.warn("Failed to mkdir folder");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Objects.requireNonNull(section.getString("date-format")));
        String zipName = Objects.requireNonNull(section.getString("zip-name"))
                .replace("{date}", simpleDateFormat.format(new Date()));

        return new File(folder, zipName);
    }

    private static String getBetweenTime(long betweenSeconds) {
        long hour = 1000 * 60 * 60;
        long min = 1000 * 60;
        return String.format("%dh %dm %ds", betweenSeconds / hour, betweenSeconds % hour / min, betweenSeconds % hour % min);
    }
}
