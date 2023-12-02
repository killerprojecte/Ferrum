package dev.rgbmc.ferrum.api.utils;

import org.badiff.FileDiffs;
import org.badiff.imp.FileDiff;

import java.io.File;
import java.io.IOException;

public class DiffUtils {
    // TODO: I Hate Diff Incremental!!!!! (Rollback to use jBsDiff) I will find a new way for incremental mode (Like: save a deleted file index in a new zip? and only save modified file in there)
    public static void createDiffFile(File oldFile, File newFile, File patchFile) throws IOException {
        FileDiff diff = FileDiffs.diff(oldFile, newFile);

        if (patchFile.exists()) patchFile.delete();
        diff.renameTo(patchFile);
    }
}
