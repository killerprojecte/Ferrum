package dev.rgbmc.ferrum.api.utils;

import net.lingala.zip4j.headers.HeaderUtil;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;

import java.lang.reflect.Field;
import java.util.List;

public class ReflectUtils {
    public static List<FileHeader> getFileHeaders(ZipOutputStream zipOutputStream) throws Exception {
        return getZipModel(zipOutputStream).getCentralDirectory().getFileHeaders();
    }

    public static FileHeader getFileHeader(ZipOutputStream zipOutputStream, String fileName) throws Exception {
        return HeaderUtil.getFileHeader(getZipModel(zipOutputStream), fileName);
    }

    public static ZipModel getZipModel(ZipOutputStream zipOutputStream) throws Exception {
        Field field = zipOutputStream.getClass().getDeclaredField("zipModel");
        field.setAccessible(true);
        return (ZipModel) field.get(zipOutputStream);
    }
}
