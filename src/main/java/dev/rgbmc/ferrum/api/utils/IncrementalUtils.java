package dev.rgbmc.ferrum.api.utils;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

public class IncrementalUtils {
    public static File findOriginZip(File folder) {
        Optional<File> optional = Arrays.stream(folder.listFiles()).filter(file -> file.getName().endsWith(".full.zip")).findFirst();
        return optional.orElse(null);
    }
}
