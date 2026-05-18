package org.mxnik.forcechess.Training;

import au.com.bytecode.opencsv.CSVReader;
import net.chesstango.gardel.fen.FEN;
import org.bytedeco.libfreenect._freenect_context;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mxnik.forcechess.General.ConsoleBar;
import org.mxnik.forcechess.Pos.*;
import org.mxnik.forcechess.bot.ChessBot;

import java.io.*;
import java.util.Arrays;

import static org.mxnik.forcechess.Pos.PositionUtils.fromFen;

public class StockfishBufferBuilder {
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


    public StockfishBufferBuilder(String fileP) throws FileNotFoundException {
        if(fileP == null){
            throw new IllegalArgumentException("Can't pass null as an argument for fileP");
        }
        this.Path = fileP;
        reader = new CSVReader(new BufferedReader(new FileReader(fileP)));
    }

    private void skipLines(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            reader.readNext();
        }
    }

    /**
     * reads count lines from the file and returns a String[][];<br>
     * - line<br>
     * - features
     *
     * @throws IOException if bad things happen EOF exceptions are caught
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

    public SampleBuffer buildBufferFromLines(int count, int chunkSize, SampleBuffer s) throws IOException{
        int chunkCont = count / chunkSize;
        s.flushToFile(false);
        System.out.print("Building buffer from file: chunked");
        for (int i = 0; i < chunkCont; i++) {
            System.out.printf("\nchunk %d: %d -> %d\n", i, i * chunkSize, (i + 1) * chunkSize);
            buildBufferFromLines(chunkSize, s).flushToFile(true);
        }
        return s;
    }

    public SampleBuffer buildBufferFromLines(int count, SampleBuffer s) throws IOException {
        String[][] lines = readSets(count+1);
        for (int i = 1; i < count+1; i++) {
            pos = fromFen(lines[i][FEN_POS]);
            int off = MoveGen.generateMoves(pos, 0, true, tempMove);
            int zVal = Integer.parseInt(lines[i][SCORE_CP]);
            float[] dist =  getMoveDist(lines[i][TOP_MOVES_JSON], off, zVal);
            s.addSample(PositionEncoder.encodeFlat(pos), dist, (float) zVal /100);
            ConsoleBar.render((i - (double) 1) / count, 2);
        }
        return s;
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

            int move = Move.of(from, to, Move.toFlags(pos, to, 0));             // no promotion
            moveBuff[PolicyIndex.toPolicyIndex(move)] = (float) cpScore;
        }

        return EndgameBufferBuilder.softMax(moveBuff, 1F);
    }



    public SampleBuffer buildBufferFromLines(int count, String fileName) throws IOException {
        return buildBufferFromLines(count, new SampleBuffer(count, fileName, false));
    }

    public SampleBuffer buildBufferFromLines(int count, int chunk, String fileName) throws IOException {
            return buildBufferFromLines(count, chunk, new SampleBuffer(count, fileName, false));
        }

    public static void main(String[] args) throws IOException {
        StockfishBufferBuilder st = new StockfishBufferBuilder("C:\\Users\\cocon\\Documents\\programming\\School\\POS\\ForceChess\\engine\\src\\main\\java\\org\\mxnik\\forcechess\\stockfish\\chess_training_data.csv");
        st.buildBufferFromLines(270000, 27000, "Stockfish");
    }


}
