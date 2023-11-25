package dev.rgbmc.ferrum.api.handlers;

import dev.rgbmc.ferrum.api.Backup;

import java.io.File;
import java.nio.file.Path;

public class SimpleHandler extends AbstractHandler {

    @Override
    public void handleZipFile(File zipFile, Path sourceFolder) {
        Backup.logger.info("Backup Process Finished!");
    }
}
