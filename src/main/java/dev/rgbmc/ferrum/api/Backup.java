package dev.rgbmc.ferrum.api;

import dev.rgbmc.ferrum.api.handlers.AbstractHandler;
import dev.rgbmc.ferrum.api.handlers.SimpleHandler;
import dev.rgbmc.ferrum.api.objects.ResultInfo;
import dev.rgbmc.ferrum.api.utils.CRCUtils;
import dev.rgbmc.ferrum.api.utils.LogoUtils;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Backup {

    public final static Logger logger = LoggerFactory.getLogger("Ferrum");
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final File file;
    private final Path folder;
    private final List<AbstractHandler> handlers = new ArrayList<>(Collections.singletonList(new SimpleHandler()));
    private String password = null;
    private boolean encrypt = false;
    private boolean incremental = false;
    private CompressionMethod compressionMethod = CompressionMethod.DEFLATE;
    private CompressionLevel compressionLevel = CompressionLevel.NORMAL;
    private EncryptionMethod encryptionMethod = EncryptionMethod.AES;
    private AesKeyStrength aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256;
    private List<String> ignores = new ArrayList<>();
    private final String zipPath;

    public Backup(File zipFile, Path folder, String zipPath) {
        this.file = zipFile;
        this.folder = folder;
        this.zipPath = zipPath;
    }

    private static String getComment() {
        return LogoUtils.getLogo() + "\n" +
                "Powered By Ferrum" + "\n" +
                "GitHub: https://github.com/killerproject/Ferrum" + "\n" +
                "Date: " + simpleDateFormat.format(new Date()) + "\n" +
                "[Please leave a star to our repository <3]" +
                "\n" +
                "[Consider Sponsoring us to keep this Open Source]";
    }

    public ResultInfo startBackup() {
        //System.out.println(zipPath);
        AtomicInteger deletions = new AtomicInteger(0);
        AtomicInteger modifications = new AtomicInteger(0);
        AtomicInteger additions = new AtomicInteger(0);
        try {
            ZipParameters zipParameters = getZipParameters();
            if (!incremental) {
                logger.warn("Zip File already exist, Ferrum will delete it when progress start");
                file.delete();
            }
            ZipFile zipFile;
            if (password == null) {
                zipFile = new ZipFile(file);
            } else {
                zipFile = new ZipFile(file, password.toCharArray());
            }
            if (incremental) {
                List<FileHeader> fileHeaders = new ArrayList<>(zipFile.getFileHeaders());
                for (FileHeader fileHeader : fileHeaders) {
                    Path targetPath = folder.resolve(fileHeader.getFileName());
                    File targetFile = targetPath.toFile();
                    if (!targetFile.exists()) {
                        zipFile.removeFile(fileHeader);
                        deletions.getAndIncrement();
                        continue;
                    }
                    if (fileHeader.getCrc() != CRCUtils.getCRC(targetFile)) {
                        zipFile.removeFile(fileHeader);
                        zipFile.addFile(targetFile, getParameters(zipParameters, targetFile, folder));
                        modifications.getAndIncrement();
                    }
                }
            }
            Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path walkingPath, BasicFileAttributes attrs) {
                    File walkingFile = walkingPath.toFile();
                    if (walkingFile.canWrite() && walkingFile.canRead()) {
                        Path relativizedPath = folder.relativize(walkingPath);
                        //System.out.println(folder.relativize(file.toPath()).toString());
                        if (relativizedPath.toString().contains(zipPath)) return FileVisitResult.CONTINUE;
                        if (ignores.stream().anyMatch(s -> relativizedPath.toString().startsWith(s) || relativizedPath.toString().endsWith(s))) return FileVisitResult.CONTINUE;
                        try {
                            if (incremental) {
                                FileHeader fileHeader = zipFile.getFileHeader(relativizedPath.toString());
                                if (fileHeader != null) return FileVisitResult.CONTINUE;
                            }
                            zipFile.addFile(walkingFile, getParameters(zipParameters, walkingFile, folder));
                            additions.getAndIncrement();
                        } catch (IOException e) {
                            e.printStackTrace();
                            logger.error("[Skipped] Failed to zipping file " + relativizedPath + ", Error message: " + e.getMessage());
                        }
                    } else {
                        logger.warn("File " + folder.relativize(walkingPath) + " has been locked by other process or system, Ferrum skipped compress this file");
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            zipFile.setComment(getComment());
            zipFile.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        handlers.forEach(handler -> {
            handler.handleZipFile(file, folder);
        });
        return new ResultInfo(deletions.get(), modifications.get(), additions.get());
    }

    private ZipParameters getZipParameters() {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setFileComment(getComment());
        if (encrypt) {
            zipParameters.setEncryptFiles(true);
            zipParameters.setEncryptionMethod(encryptionMethod);
            zipParameters.setAesKeyStrength(aesKeyStrength);
        }
        zipParameters.setCompressionMethod(compressionMethod);
        if (compressionMethod == CompressionMethod.DEFLATE) {
            zipParameters.setCompressionLevel(compressionLevel);
        }
        zipParameters.setIncludeRootFolder(true);
        return zipParameters;
    }

    private ZipParameters getParameters(ZipParameters zipParameters, File file, Path folder) {
        ZipParameters copy = new ZipParameters(zipParameters);
        Path relativizedPath = folder.relativize(file.getParentFile().toPath());
        copy.setRootFolderNameInZip(relativizedPath.toString());
        return copy;
    }

    public boolean isEncrypt() {
        return encrypt;
    }

    public void setEncrypt(boolean encrypt) {
        this.encrypt = encrypt;
    }

    public CompressionMethod getCompressionMethod() {
        return compressionMethod;
    }

    public void setCompressionMethod(CompressionMethod compressionMethod) {
        this.compressionMethod = compressionMethod;
    }

    public EncryptionMethod getEncryptionMethod() {
        return encryptionMethod;
    }

    public void setEncryptionMethod(EncryptionMethod encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
    }

    public CompressionLevel getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(CompressionLevel compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AesKeyStrength getAesKeyStrength() {
        return aesKeyStrength;
    }

    public void setAesKeyStrength(AesKeyStrength aesKeyStrength) {
        this.aesKeyStrength = aesKeyStrength;
    }

    public List<AbstractHandler> getHandlers() {
        return handlers;
    }

    public void registerHandler(AbstractHandler handler) {
        handlers.add(handler);
    }

    public void setIgnores(List<String> ignores) {
        this.ignores = ignores;
    }

    public List<String> getIgnores() {
        return ignores;
    }
}
