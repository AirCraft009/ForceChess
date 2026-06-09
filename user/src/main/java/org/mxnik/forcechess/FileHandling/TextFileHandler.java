package org.mxnik.forcechess.FileHandling;

import java.io.File;
import java.util.ArrayList;

public class TextFileHandler {
    /**
     * @param folder the path of the folder
     * @return the content of the folder specified by the {@code folder}
     */
    public static ArrayList<String> getFolderContents(String folder) {
        ArrayList<String> contents = new ArrayList<>(0);
        File folderFile = new File(folder);
        File[] listOfFiles = folderFile.listFiles();
        if (listOfFiles != null) {
            for (File listOfFile : listOfFiles) {
                if (listOfFile.isFile()) {
                    contents.add(listOfFile.getName());
                }
            }
        }
        return contents;
    }
}
