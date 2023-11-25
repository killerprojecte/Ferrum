package dev.rgbmc.ferrum.utils;

import dev.rgbmc.ferrum.Ferrum;
import dev.rgbmc.ferrum.api.Backup;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class BackupUtils {
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static boolean locked = false;

    public static boolean startBackup(boolean fromCommand) {
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
            File folder = new File(section.getString("save-path"));
            List<File> files = Arrays.stream(folder.listFiles())
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
                    deleteFile.delete();
                }
            }
        }
        Backup backup = new Backup(targetFile, workingPath.toPath());
        if (section.getBoolean("incremental")) {
            backup.setIncremental(true);
        }
        if (section.getBoolean("encrypt")) {
            backup.setEncrypt(true);
            backup.setEncryptionMethod(EncryptionMethod.valueOf(section.getString("encrypt-method")));
        }
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
        executorService.submit(() -> {
            backup.startBackup();
        });
        return true;
    }

    private static File getTargetFile() {
        ConfigurationSection section = Ferrum.instance.getConfig().getConfigurationSection("backups");
        File folder = new File(section.getString("save-path"));
        folder.mkdirs();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(section.getString("date-format"));
        String zipName = section.getString("zip-name")
                .replace("{date}", simpleDateFormat.format(new Date()));

        File zipFile = new File(folder, zipName);
        return zipFile;
    }
}
