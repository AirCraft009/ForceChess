package org.mxnik.forcechess.FileHandling;

import java.io.File;

public class TextFileHandler {
    public static String[] getFolderContents(String folder) {
        String[] contents = null;
        File folderFile = new File(folder);
        File[] listOfFiles = folderFile.listFiles();
        if (listOfFiles != null) {
            contents = new String[listOfFiles.length];
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    contents[i] = listOfFiles[i].getName();
                }
            }
        }
        return contents;
    }
}
