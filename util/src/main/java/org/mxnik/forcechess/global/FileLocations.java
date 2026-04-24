package org.mxnik.forcechess.global;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class FileLocations {
    public static final String OPTION_FILE = "boardsNBots/option.properties";
    public static final String FEN_STRING_FILE = "boardsNBots/FenBoards.properties";
    public static final Properties FILE_PROPERTIES = new Properties();
    public static final Properties FEN_PROPERTIES = new Properties();

    // constant keys
    public static final String BOT_FILE_KEY = "botFiles";
    public static final String NETWORK_LOCATION_KEY = "network_data";
    public static final String SAMPLE_LOCATION_KEY = "samples";

    // loaded Vars
    public static final String BOT_FILES;
    public static final String NETWORK_LOCATIONS;
    public static final String SAMPLE_LOCATIONS;

    static {
        try {
            FILE_PROPERTIES.load(new FileInputStream(OPTION_FILE));
            FEN_PROPERTIES.load(new FileInputStream(FEN_STRING_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Error reading PropertyFiles on Startup");
        }

        BOT_FILES = FILE_PROPERTIES.getProperty(BOT_FILE_KEY);
        NETWORK_LOCATIONS = FILE_PROPERTIES.getProperty(NETWORK_LOCATION_KEY);
        SAMPLE_LOCATIONS = FILE_PROPERTIES.getProperty(SAMPLE_LOCATION_KEY);
    }
}
