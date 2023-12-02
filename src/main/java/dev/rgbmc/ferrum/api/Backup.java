package dev.rgbmc.ferrum.api;

import dev.rgbmc.ferrum.api.handlers.AbstractHandler;
import dev.rgbmc.ferrum.api.handlers.SimpleHandler;
import dev.rgbmc.ferrum.api.objects.ResultInfo;
import dev.rgbmc.ferrum.api.utils.DiffUtils;
import dev.rgbmc.ferrum.api.utils.LogoUtils;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Backup {

    public final static Logger logger = LoggerFactory.getLogger("Ferrum");
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Path folder;
    private final List<AbstractHandler> handlers = new ArrayList<>(Collections.singletonList(new SimpleHandler()));
    private final String zipPath;
    private File file;
    private String password = null;
    private boolean encrypt = false;
    private boolean incremental = false;
    private CompressionMethod compressionMethod = CompressionMethod.DEFLATE;
    private CompressionLevel compressionLevel = CompressionLevel.NORMAL;
    private EncryptionMethod encryptionMethod = EncryptionMethod.AES;
    private AesKeyStrength aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256;
    private List<String> ignores = new ArrayList<>();

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

    public static void submitTask(Runnable runnable) {
        executorService.submit(runnable);
    }

    public ResultInfo startBackup() {
        //System.out.println(zipPath);
        try {
            ZipParameters zipParameters = getZipParameters();
            if (!incremental) {
                logger.warn("Zip File already exist, Ferrum will delete it when progress start");
                file.delete();
            }
            long count = Arrays.stream(file.getParentFile().listFiles()).filter(file1 -> file1.getName().endsWith(".zip")).count();
            ZipOutputStream zipOutputStream = initializeZipOutputStream(
                    file,
                    password != null,
                    (password == null ? new char[0] : password.toCharArray()),
                    incremental
            );
            walkingFile(zipOutputStream, zipParameters, folder.toFile());
            if (incremental) logger.info("Temp File has finished creating");
            zipOutputStream.setComment(getComment());
            zipOutputStream.close();
            if (incremental) {
                if (count >= 1) {
                    String originalFileName = file.getName();
                    file = new File(file.getParentFile(), originalFileName + ".tmp");
                    File diffFile = new File(file.getParentFile(), originalFileName + ".patch");
                    FileOutputStream fileOutputStream = new FileOutputStream(diffFile);
                    File zip = Arrays.stream(file.getParentFile().listFiles()).filter(file1 -> file1.getName().endsWith(".zip")).findFirst().get();
                    DiffUtils.createDiffFile(zip, file, diffFile);
                    file.delete();
                    file = diffFile;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        handlers.forEach(handler -> {
            handler.handleZipFile(file, folder);
        });
        return new ResultInfo(this, file);
    }

    private void walkingFile(ZipOutputStream zipOutputStream, ZipParameters zipParameters, File file) {
        Path walkingPath = file.toPath();
        Path relativizedPath = folder.relativize(walkingPath);
        //System.out.println(folder.relativize(file.toPath()));
        if (relativizedPath.toString().contains(zipPath)) return;
        if (ignores.stream().anyMatch(s -> relativizedPath.toString().startsWith(s) || relativizedPath.toString().endsWith(s)))
            return;
        if (file.isDirectory()) {
            for (File children : file.listFiles()) {
                walkingFile(zipOutputStream, zipParameters, children);
            }
        } else {
            saveFile(zipOutputStream, zipParameters, file);
        }
    }

    private void saveFile(ZipOutputStream zipOutputStream, ZipParameters zipParameters, File file) {
        Path walkingPath = file.toPath();
        if (file.canWrite() && file.canRead()) {
            Path relativizedPath = folder.relativize(walkingPath);
            if (relativizedPath.toString().contains(zipPath)) return;
            if (ignores.stream().anyMatch(s -> relativizedPath.toString().startsWith(s) || relativizedPath.toString().endsWith(s)))
                return;
            try {
                zipOutputStream.putNextEntry(getParameters(zipParameters, file, folder));
                byte[] buff = new byte[4096];
                try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(walkingPath), 4096)) {
                    int readLen;
                    while ((readLen = inputStream.read(buff)) != -1) {
                        zipOutputStream.write(buff, 0, readLen);
                    }
                }
                zipOutputStream.closeEntry();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("[Skipped] Failed to zipping file " + relativizedPath + ", Error message: " + e.getMessage());
            }
        } else {
            logger.warn("File " + folder.relativize(walkingPath) + " has been locked by other process or system, Ferrum skipped compress this file");
        }
    }

    private ZipOutputStream initializeZipOutputStream(File outputZipFile, boolean encrypt, char[] password, boolean incremental) throws IOException {
        if (Arrays.stream(outputZipFile.getParentFile().listFiles()).anyMatch(file1 -> file1.getName().endsWith(".zip"))) {
            outputZipFile = new File(outputZipFile.getParentFile(), outputZipFile.getName() + ".tmp");
        }

        FileOutputStream fos = new FileOutputStream(outputZipFile);

        if (!encrypt) {
            password = null;
        }

        return new ZipOutputStream(fos, password);
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
        zipParameters.setOverrideExistingFilesInZip(true);
        return zipParameters;
    }

    private ZipParameters getParameters(ZipParameters zipParameters, File file, Path folder) {
        ZipParameters copy = new ZipParameters(zipParameters);
        Path relativizedPath = folder.relativize(file.toPath());
        copy.setFileNameInZip(relativizedPath.toString());
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

    public List<String> getIgnores() {
        return ignores;
    }

    public void setIgnores(List<String> ignores) {
        this.ignores = ignores;
    }

    public File getFile() {
        return file;
    }

    public String getZipPath() {
        return zipPath;
    }

    public Path getFolder() {
        return folder;
    }
}
