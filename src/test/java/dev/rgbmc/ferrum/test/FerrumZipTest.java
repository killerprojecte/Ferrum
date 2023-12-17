package dev.rgbmc.ferrum.test;

import dev.rgbmc.ferrum.api.Backup;
import dev.rgbmc.ferrum.api.objects.ResultInfo;

import java.io.File;

public class FerrumZipTest {
    public static void main(String[] args) {
        File folder = new File(System.getProperty("user.dir"));
        Backup backup = new Backup(new File(folder, "test/output/Zipped.zip"), folder.toPath().resolve("test/files"), "test/output/");
        backup.setIncremental(false);
        ResultInfo resultInfo = backup.startBackup();
        System.out.println(resultInfo.getFile().getName());
    }
}
