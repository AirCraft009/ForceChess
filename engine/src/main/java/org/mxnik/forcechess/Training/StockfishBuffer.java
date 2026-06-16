package org.mxnik.forcechess.Training;

import au.com.bytecode.opencsv.CSVReader;
import org.json.JSONArray;
import org.mxnik.forcechess.General.ConsoleBar;
import org.mxnik.forcechess.General.DiversePair;
import org.mxnik.forcechess.Pos.*;
import org.mxnik.forcechess.bot.ChessBot;

import java.io.*;
import java.util.Arrays;

import static java.lang.Math.abs;
import static org.mxnik.forcechess.Pos.PositionUtils.fromFen;
import static org.mxnik.forcechess.Training.SampleBuffer.softMax;

public class StockfishBuffer implements TrainingsBuffer {
    private final String Path;
    private PositionEncoder.Position pos;
    private CSVReader reader;
    private final int[] tempMove = new int[ChessBot.MAX_MOVES_IN_POS];
    private final float[] moveBuff = new float[Move.MOVE_POSSIBILITIES];

    public static final int FEN_POS = 0;
    public static final int BEST_MOVE = 1;
    public static final int TOP_MOVES_JSON = 2;
    public static final int SCORE_CP = 3;
    public static final int SCORE_WDL_W = 4;
    public static final int SCORE_WDL_D = 5;
    public static final int SCORE_WDL_L = 6;
    public static final int SOURCE = 7;


    public StockfishBuffer(String fileP) throws IOException {
        if(fileP == null){
            throw new IllegalArgumentException("Can't pass null as an argument for fileP");
        }
        this.Path = fileP;
        reader = new CSVReader(new BufferedReader(new FileReader(fileP)));
        try {
            reader.readNext();      // skip first line (only shows the different attr.)
        }catch (EOFException e){
            throw new IllegalStateException("End of file was reached on first read. EMPTY FILE: ");
        }
    }

    public void skipLines(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            reader.readNext();
        }
    }

    /**
     * reads count lines from the file and returns a String[][];<br>
     * - line<br>
     * - features
     *
     * @throws IOException if bad things happen. EOF exceptions are caught
     */
    public String[][] readSets(int count) throws IOException {
        String[][] lines = new String[count][];
        try {
            for (int i = 0; i < count; i++) {
                lines[i] =  reader.readNext();
            }
        }catch (EOFException e){
            System.out.println("reached end of file");
        }

        return lines;
    }

    public DiversePair<SampleBuffer.TrainingSample, PositionEncoder.Position> getNext(){
        String[] line;
        try {
             line = reader.readNext();
        }catch (EOFException e){
            reset();
            return getNext();       // shouldn't recurse more than once.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        pos = fromFen(line[FEN_POS]);
        int off = MoveGen.generateMoves(pos, 0, true, tempMove);
        int zVal = Integer.parseInt(line[SCORE_CP]);
        float[] dist =  getMoveDist(line[TOP_MOVES_JSON], off, zVal);

        return new DiversePair<>(new SampleBuffer.TrainingSample(PositionEncoder.encodeFlat(pos), dist, (float) zVal /100), pos);
    }

    public void reset() {
        try {
            reader.close();
            reader = new CSVReader(new BufferedReader(new FileReader(Path)));
        }catch (IOException e){
            throw new RuntimeException("Error while closing old reader or opening new one: " + e);
        }
    }


    public SampleBuffer.TrainingSample[] getNextFew(int count) throws IOException {
        SampleBuffer.TrainingSample[] samples = new SampleBuffer.TrainingSample[count];
        String[][] lines = readSets(count+1);
        for (int i = 1; i < count+1; i++) {
            pos = fromFen(lines[i][FEN_POS]);
            int off = MoveGen.generateMoves(pos, 0, true, tempMove);
            int zVal = Integer.parseInt(lines[i][SCORE_CP]);
            float[] dist =  getMoveDist(lines[i][TOP_MOVES_JSON], off, zVal);
            samples[i-1] = new SampleBuffer.TrainingSample(PositionEncoder.encodeFlat(pos), dist, (float) zVal /100);
            ConsoleBar.render((i - (double) 1) / count, 2);
        }
        return samples;
    }

    public float[] getMoveDist(String jsonMoves, int off, int cp){
        JSONArray arr = new JSONArray(jsonMoves);

        Arrays.fill(moveBuff, Float.NEGATIVE_INFINITY);

        for (int i = 0; i < off; i++) {
            moveBuff[PolicyIndex.toPolicyIndex(tempMove[i])] = cp-10;
        }

        for (int i = 0; i < arr.length(); i++) {
            var moveOb = arr.getJSONObject(i);
            var moveFromTo = moveOb.get("move");
            int cpScore = moveOb.getInt("cp");

            String moveStr = moveFromTo.toString();
            int from = PositionUtils.parseFieldName(moveStr.substring(0,2));
            int to = PositionUtils.parseFieldName(moveStr.substring(2,4));
            int promotes = 0;

            if(moveStr.length() > 4){
                switch (moveStr.charAt(4)){
                    case 'b' -> promotes = 3;
                    case 'n' -> promotes = 4;
                    case 'r' -> promotes = 2;
                    case 'q' -> promotes = 1;
                    default -> throw new IllegalStateException("moveStr should never have a none promotion char (b,n,r,q) at pos 4");
                }
            }

            int move = Move.of(from, to, Move.toFlags(pos, from, to, promotes));
            moveBuff[PolicyIndex.toPolicyIndex(move)] = (float) cpScore;
        }

        return softMax(moveBuff, 1.2F);
    }



    public static void main(String[] args) throws IOException {
        StockfishBuffer st = new StockfishBuffer("C:\\Users\\cocon\\Documents\\programming\\School\\POS\\ForceChess\\engine\\src\\main\\java\\org\\mxnik\\forcechess\\stockfish\\full_data.csv");
    }


}
