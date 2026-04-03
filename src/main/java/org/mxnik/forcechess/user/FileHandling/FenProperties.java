package org.mxnik.forcechess.user.FileHandling;

import java.io.*;
import java.util.Properties;
import java.util.Set;

public class FenProperties {
    private static final String FEN_PROPERTIES_FILE = System.getProperty("user.dir") + "/boardsNBots/FenBoards.properties";//TODO File location with Properties
    private static Properties props;
    public static Set<String> fenNames;

    public static void load() {
        props = new Properties();
        try {
            props.load(new BufferedReader(new FileReader(FEN_PROPERTIES_FILE)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        fenNames = props.stringPropertyNames();
    }
    public static String getFenStr(String name) {
        return props.getProperty(name);
    }
    public static void addFenStr(String name, String value) {
        props.setProperty(name, value);
        save();
    }
    public static void removeFenStr(String name) {
        if(name.equals("default"))
            return;
        props.remove(name);
        save();
    }
    private static void save() {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(FEN_PROPERTIES_FILE));

            props.store(out, "All saved Fen-Strings and the default 8x8 board, saved as a property with their names");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        load();
    }
}
