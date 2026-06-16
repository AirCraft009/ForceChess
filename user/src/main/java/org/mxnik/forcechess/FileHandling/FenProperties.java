package org.mxnik.forcechess.FileHandling;

import java.io.*;
import java.util.Properties;
import java.util.Set;

public class FenProperties {
    private static final String FEN_PROPERTIES_FILE = "boardsNBots/FenBoards.properties";//TODO File location with Properties
    private static Properties props = new Properties();
    public static Set<String> fenNames;

    /**
     * Loads the FEN-Strings and their corresponding names from the properties file
     */
    public static void load() {
        try {
            props.load(new BufferedReader(new FileReader(FEN_PROPERTIES_FILE)));
            fenNames = props.stringPropertyNames();
            if (fenNames.isEmpty()) {
                FenProperties.addFenStr("default", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 8");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    /**
     * @param name the Name of the FEN-String
     * @return the corresponding FEN-String as saved in the .properties file
     */
    public static String getFenStr(String name) {
        return props.getProperty(name);
    }

    /**
     * adds a FEN-String to the properties-file as a {@code name}, {@code value} pair
     * @param name
     * @param value
     */
    public static void addFenStr(String name, String value) {
        props.setProperty(name, value);
        save();
    }

    /**
     * Removes the FEN-String with the name specified in {@code name}
     * @param name
     */
    public static void removeFenStr(String name) {
        if(name.equals("default"))
            return;
        props.remove(name);
        save();
    }

    /**
     * A method to save changes to the .properties file and reload it's content
     */
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
