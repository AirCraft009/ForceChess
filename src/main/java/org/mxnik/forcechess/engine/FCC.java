package org.mxnik.forcechess.engine;

import javafx.scene.Scene;
import javafx.scene.shape.Path;

import java.io.File;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Command-line tool to work with the given Files and explore them
 */
public class FCC {
    public static final String baseP = "boardsNBots/bots/";
    private String currentPath;
    private final Scanner input;
    private File[] currFileEnv;
    private File selectedFile;
    private boolean hasSelected = false;

    public FCC (){
        currentPath = baseP;
        input = new Scanner(System.in);
        currFileEnv = new File[0];
    }

    public void showEnv() {
        File dir = new File(currentPath);

        if (!dir.exists()) {
            System.out.println("Path does not exist: " + currentPath);
            return;
        }

        if (!dir.isDirectory()) {
            System.out.println("Path is not a directory: " + currentPath);
            return;
        }

        File[] files = dir.listFiles();

        if (files == null || files.length == 0) {
            System.out.println("Directory is empty.");
            return;
        }

        System.out.println("Contents of: " + currentPath);
        System.out.println("----------------------------------");

        this.currFileEnv = files;

        for (int i = 0; i < files.length; i ++) {
            File file = files[i];
            if (file.isDirectory()) {
                System.out.println("[DIR " + i + "] " + file.getName());
            } else {
                System.out.println("[FILE " + i + "] " + file.getName() + " (" + file.length() + " bytes)");
            }
        }
        System.out.println();
    }

    public boolean chooseDialog(){
        System.out.println("Enter the number of the file or directory to: ");
        System.out.println("- select the file");
        System.out.println("- open The directory");
        System.out.println("- . to go back up");
        System.out.println("- type -1 to quit");

        int selection = 0;
        String raw = null;
        while (input.hasNext()) {
            try {
                 raw = input.next();
                 selection = Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                assert raw != null;

                if(raw.equals("q")){
                    return false;
                }else if (raw.equals(".")){
                    // parseString and go up one
                    return true;
                }

                System.err.println("only an int can be a selection!!");
                continue;
            }



            if(selection < 0 || selection > currFileEnv.length) {
                System.err.println("selection out of bounds");
                continue;
            }

            break;
        }

        selectedFile = currFileEnv[selection];
        if (selectedFile.isDirectory()) {
            currentPath = selectedFile.getPath();
        }
        return true;
    }

    public void close(){
        input.close();
    }

    public static void main(String[] args) {
        FCC f = new FCC();
        do {
            f.showEnv();
        } while (f.chooseDialog());
        f.close();
    }
}
