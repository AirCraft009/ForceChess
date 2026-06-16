package org.mxnik.forcechess.UI.settings;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.mxnik.forcechess.General.FileLocations;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public record SavedSettings(String savePath, Color lightSquare, Color darkSquare, Color lightHighlight, Color darkHighlight, Color lightMoved, Color darkMoved, String defaultBot) {
    public static final SavedSettings defaultSettings = new SavedSettings( "boardsNBots/", Color.WHITE, Color.DARKBLUE, Color.WHEAT, Color.LIGHTBLUE, Color.LIME, Color.GREEN, "");
    public static SavedSettings savedSettings;
    private static Properties properties;

    /**
     * Reads the Settings from the .properties file, and saves them in {@code savedSettings}
     */
    public static void loadSavedSettings() {
        properties = FileLocations.FILE_PROPERTIES;

        try {
            String savePath = properties.getProperty("savePath");
            Color lightSquare = Color.web(properties.getProperty("lightSquare"));
            Color darkSquare = Color.web(properties.getProperty("darkSquare"));
            Color lightHighlight = Color.web(properties.getProperty("lightHighlight"));
            Color darkHighlight = Color.web(properties.getProperty("darkHighlight"));
            Color lightMoved = Color.web(properties.getProperty("lightMoved"));
            Color darkMoved = Color.web(properties.getProperty("darkMoved"));
            String defaultBot = properties.getProperty("default_bot");

            savedSettings = new SavedSettings(savePath, lightSquare, darkSquare, lightHighlight, darkHighlight, lightMoved, darkMoved, defaultBot);
        }catch (NullPointerException e){
            savedSettings = defaultSettings;
            writeSettings();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write the {@code savedSettings} to the .properties file
     */
    public static void writeSettings() {
        properties.setProperty("savePath", savedSettings.savePath);
        properties.setProperty("boardPositionFile", savedSettings.savePath + "FenBoards.properties");
        properties.setProperty("botFiles", savedSettings.savePath + "bots/");
        properties.setProperty("network_data", savedSettings.savePath + "bots/networks/");
        properties.setProperty("samples", savedSettings.savePath + "bots/sample_data/");
        properties.setProperty("lightSquare", savedSettings.lightSquare.toString());
        properties.setProperty("darkSquare", savedSettings.darkSquare.toString());
        properties.setProperty("lightHighlight", savedSettings.lightHighlight.toString());
        properties.setProperty("darkHighlight", savedSettings.darkHighlight.toString());
        properties.setProperty("lightMoved", savedSettings.lightMoved.toString());
        properties.setProperty("darkMoved", savedSettings.darkMoved.toString());
        properties.setProperty("default_bot", savedSettings.defaultBot);

        try {
            FileOutputStream out = new FileOutputStream(FileLocations.OPTION_FILE);

            properties.store(out, "The Users Settings");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
