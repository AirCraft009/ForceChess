package org.mxnik.forcechess.General;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class FileLocations {
    public static final String OPTION_FILE = "boardsNBots/option.properties";
    public static final String FEN_STRING_FILE = "boardsNBots/FenBoards.properties";
    public static Properties FILE_PROPERTIES = new Properties();
    public static Properties FEN_PROPERTIES = new Properties();

    // constant keys
    public static final String BOT_FILE_KEY = "botFiles";
    public static final String NETWORK_LOCATION_KEY = "network_data";
    public static final String SAMPLE_LOCATION_KEY = "samples";
    public static final String FEN_PROPERTIES_KEY = "boardPositionFile";

    // loaded Vars
    public static String BOT_FILES;
    public static String NETWORK_LOCATIONS;
    public static String SAMPLE_LOCATIONS;

    static {
        try {
            FILE_PROPERTIES.load(new FileInputStream(OPTION_FILE));
        } catch (IOException e) {
            throw new RuntimeException("Error on initial properties load", e);
        }
    }


    public static void loadPaths(){
        try {
            FILE_PROPERTIES.load(new FileInputStream(OPTION_FILE));

            NETWORK_LOCATIONS = FILE_PROPERTIES.getProperty(NETWORK_LOCATION_KEY);
            File netDir = new File(NETWORK_LOCATIONS);
            if(!netDir.exists())
                netDir.mkdirs();

            File fenProperties = new File(FILE_PROPERTIES.getProperty(FEN_PROPERTIES_KEY));
            if(!fenProperties.exists())
                fenProperties.createNewFile();
            FEN_PROPERTIES.load(new FileInputStream(FILE_PROPERTIES.getProperty(FEN_PROPERTIES_KEY)));
        }
        catch (IOException e){
            throw new RuntimeException("Error reading PropertyFiles on Startup");
        }

        BOT_FILES = FILE_PROPERTIES.getProperty(BOT_FILE_KEY);
        SAMPLE_LOCATIONS = FILE_PROPERTIES.getProperty(SAMPLE_LOCATION_KEY);
    }
}
