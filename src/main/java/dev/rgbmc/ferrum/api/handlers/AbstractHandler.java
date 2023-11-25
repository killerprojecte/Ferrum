package dev.rgbmc.ferrum.api.handlers;

import java.io.File;
import java.nio.file.Path;

public abstract class AbstractHandler {
    public abstract void handleZipFile(File zipFile, Path sourceFolder);
}
