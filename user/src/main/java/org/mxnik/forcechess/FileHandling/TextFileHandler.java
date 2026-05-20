package org.mxnik.forcechess.FileHandling;

import java.io.File;

public class TextFileHandler {
    /**
     * @param folder the path of the folder
     * @return the content of the folder specified by the {@code folder}
     */
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
