package dev.rgbmc.ferrum.api;

import com.google.gson.Gson;
import dev.rgbmc.ferrum.api.handlers.AbstractHandler;
import dev.rgbmc.ferrum.api.handlers.SimpleHandler;
import dev.rgbmc.ferrum.api.objects.IncrementalIndex;
import dev.rgbmc.ferrum.api.objects.ResultInfo;
import dev.rgbmc.ferrum.api.utils.CRCUtils;
import dev.rgbmc.ferrum.api.utils.IncrementalUtils;
import dev.rgbmc.ferrum.api.utils.LogoUtils;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.FileHeader;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class Backup {

    public final static Logger logger = LoggerFactory.getLogger("Ferrum");
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private int count = 0;

        @Override
        public Thread newThread(Runnable r) {
            count++;
            return new Thread(r, "Ferrum-" + count);
        }
    });
    private final Path folder;
    private final List<AbstractHandler> handlers = new ArrayList<>(Collections.singletonList(new SimpleHandler()));
    private final Map<String, FileHeader> cachedFileHeader = new HashMap<>();
    private String zipPath;
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

    public static void shutdown() {
        executorService.shutdownNow();
    }

    public ResultInfo startBackup() {
        //System.out.println(zipPath);
        try {
            ZipParameters zipParameters = getZipParameters();
            if (!incremental && file.exists()) {
                logger.warn("Zip File already exist, Ferrum will delete it when progress start");
                file.delete();
            }
            long count = Arrays.stream(file.getParentFile().listFiles()).filter(file1 -> file1.getName().endsWith(".full.zip")).count();
            ZipOutputStream zipOutputStream = initializeZipOutputStream(
                    file,
                    password != null,
                    (password == null ? new char[0] : password.toCharArray()),
                    incremental
            );
            if (incremental) {
                if (count >= 1) {
                    File originZip = IncrementalUtils.findOriginZip(file.getParentFile());
                    ZipFile originZipFile;
                    if (encrypt) {
                        originZipFile = new ZipFile(originZip, password.toCharArray());
                    } else {
                        originZipFile = new ZipFile(originZip);
                    }
                    IncrementalIndex index = new IncrementalIndex();
                    //long time = System.currentTimeMillis();
                    List<FileHeader> fileHeaders = new ArrayList<>(originZipFile.getFileHeaders());
                    //System.out.println("Copied headers");
                    ExecutorService executor = Executors.newFixedThreadPool(4);
                    CountDownLatch latch = new CountDownLatch(fileHeaders.size());
                    for (FileHeader fileHeader : fileHeaders) {
                        executor.submit(() -> {
                            try {
                                cachedFileHeader.put(fileHeader.getFileName(), fileHeader);
                                Path realPath = folder.resolve(fileHeader.getFileName());
                                File realFile = realPath.toFile();
                                if (!realFile.exists()) {
                                    index.getDeletion().add(fileHeader.getFileName());
                                } else if (fileHeader.getCrc() != CRCUtils.getCRC(realFile)) {
                                    index.getModification().add(fileHeader.getFileName());
                                    writeFileToZip(zipOutputStream, zipParameters, realFile);
                                }
                            } catch (Exception e) {
                                e.printStackTrace(System.err);
                            } finally {
                                latch.countDown();
                            }
                        });
                    }
                    latch.await();
                    executor.shutdown();
                    //System.out.println("Finished File-Header loop in " + (System.currentTimeMillis() - time));
                    walkingFile(index, zipOutputStream, folder.toFile(), zipParameters);
                    File tempFile = File.createTempFile("Ferrum_Incremental_", ".json");
                    String json = new Gson().toJson(index);
                    Files.write(tempFile.toPath(), json.getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                    writeFileToZip(zipOutputStream, getParameters(zipParameters, "ferrum_index.json"), tempFile, false);
                    zipOutputStream.setComment("[Incremental Zip File]\n" +
                            "!! This file contains only files changed between the full-data zip (names containing .full) !!\n"
                            + getComment());
                    zipOutputStream.close();
                    tempFile.deleteOnExit();
                    originZipFile.close();
                    return new ResultInfo(this, file);
                } else {
                    zipPath = zipPath.replace(".zip", ".full.zip");
                    file = new File(file.getParentFile(), file.getName().replace(".zip", ".full.zip"));
                    logger.info("[Incremental Mode] Creating full-file zip for next time");
                }
            }
            walkingFile(zipOutputStream, zipParameters, folder.toFile());
            zipOutputStream.setComment(getComment());
            zipOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
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
        executorService.submit(() -> {
            if (file.isDirectory()) {
                for (File children : file.listFiles()) {
                    walkingFile(zipOutputStream, zipParameters, children);
                }
            } else {
                saveFile(zipOutputStream, zipParameters, file);
            }
        });
    }

    private void walkingFile(IncrementalIndex index, ZipOutputStream zipOutputStream, File file, ZipParameters zipParameters) {
        Path walkingPath = file.toPath();
        Path relativizedPath = folder.relativize(walkingPath);
        //System.out.println(folder.relativize(file.toPath()));
        if (relativizedPath.toString().contains(zipPath)) return;
        if (ignores.stream().anyMatch(s -> relativizedPath.toString().startsWith(s) || relativizedPath.toString().endsWith(s)))
            return;
        executorService.submit(() -> {
            if (file.isDirectory()) {
                for (File children : file.listFiles()) {
                    walkingFile(index, zipOutputStream, children, zipParameters);
                }
            } else {
                saveFile(index, zipOutputStream, file, zipParameters);
            }
        });
    }

    private void saveFile(ZipOutputStream zipOutputStream, ZipParameters zipParameters, File file) {
        Path walkingPath = file.toPath();
        if (file.canWrite() && file.canRead()) {
            Path relativizedPath = folder.relativize(walkingPath);
            if (relativizedPath.toString().contains(zipPath)) return;
            if (ignores.stream().anyMatch(s -> relativizedPath.toString().startsWith(s) || relativizedPath.toString().endsWith(s)))
                return;
            try {
                writeFileToZip(zipOutputStream, zipParameters, file);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("[Skipped] Failed to zipping file " + relativizedPath + ", Error message: " + e.getMessage());
            }
        } else {
            logger.warn("File " + folder.relativize(walkingPath) + " has been locked by other process or system, Ferrum skipped compress this file");
        }
    }

    private void writeFileToZip(ZipOutputStream zipOutputStream, ZipParameters zipParameters, File file) throws IOException {
        writeFileToZip(zipOutputStream, zipParameters, file, true);
    }

    private synchronized void writeFileToZip(ZipOutputStream zipOutputStream, ZipParameters zipParameters, File file, boolean newParameters) throws IOException {
        if (newParameters) zipParameters = getParameters(zipParameters, file, folder);
        zipOutputStream.putNextEntry(zipParameters);
        byte[] buff = new byte[4096];
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()), 4096)) {
            int readLen;
            while ((readLen = inputStream.read(buff)) != -1) {
                zipOutputStream.write(buff, 0, readLen);
            }
        }
        zipOutputStream.closeEntry();
    }

    private void saveFile(IncrementalIndex index, ZipOutputStream zipOutputStream, File file, ZipParameters zipParameters) {
        Path walkingPath = file.toPath();
        if (file.canWrite() && file.canRead()) {
            Path relativizedPath = folder.relativize(walkingPath);
            if (relativizedPath.toString().contains(zipPath)) return;
            if (ignores.stream().anyMatch(s -> relativizedPath.toString().startsWith(s) || relativizedPath.toString().endsWith(s)))
                return;
            try {
                if (cachedFileHeader.containsKey(relativizedPath.toString())) return;
                writeFileToZip(zipOutputStream, zipParameters, file);
                index.getCreation().add(relativizedPath.toString());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("[Skipped] Failed to checking file " + relativizedPath + ", Error message: " + e.getMessage());
            }
        } else {
            logger.warn("File " + folder.relativize(walkingPath) + " has been locked by other process or system, Ferrum skipped compress this file");
        }
    }

    private ZipOutputStream initializeZipOutputStream(File outputZipFile, boolean encrypt, char[] password, boolean incremental) throws IOException {
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

    private ZipParameters getParameters(ZipParameters zipParameters, String name) {
        ZipParameters copy = new ZipParameters(zipParameters);
        copy.setFileNameInZip(name);
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
