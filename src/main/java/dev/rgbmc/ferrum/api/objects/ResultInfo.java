package dev.rgbmc.ferrum.api.objects;

import dev.rgbmc.ferrum.api.Backup;

import java.io.File;

public class ResultInfo {
    private final Backup backup;
    private final File file;

    public ResultInfo(Backup backup, File file) {
        this.backup = backup;
        this.file = file;
    }

    public Backup getBackup() {
        return backup;
    }

    public File getFile() {
        return file;
    }
}
