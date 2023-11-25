package dev.rgbmc.ferrum.api.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.CRC32;

public class CRCUtils {
    public static long getCRC(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        return crc32.getValue();
    }
}
