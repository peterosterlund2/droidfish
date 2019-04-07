package org.petero.droidfish;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

import org.petero.droidfish.gamelogic.Piece;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Handle rendering of chess pieces. */
public class PieceSet {
    private static PieceSet inst = null;

    private HashMap<String,Integer> nameToPieceType;
    private SVG[] svgTable = new SVG[Piece.nPieceTypes];
    private Bitmap[] bitmapTable = new Bitmap[Piece.nPieceTypes];
    private int cachedSquareSize = -1;
    private int cachedWhiteColor = 0xffffffff;
    private int cachedBlackColor = 0xff000000;

    /** Get singleton instance. */
    public static PieceSet instance() {
        if (inst == null)
            inst = new PieceSet();
        return inst;
    }

    private PieceSet() {
        nameToPieceType = new HashMap<>();
        nameToPieceType.put("wk.svg", Piece.WKING);
        nameToPieceType.put("wq.svg", Piece.WQUEEN);
        nameToPieceType.put("wr.svg", Piece.WROOK);
        nameToPieceType.put("wb.svg", Piece.WBISHOP);
        nameToPieceType.put("wn.svg", Piece.WKNIGHT);
        nameToPieceType.put("wp.svg", Piece.WPAWN);
        nameToPieceType.put("bk.svg", Piece.BKING);
        nameToPieceType.put("bq.svg", Piece.BQUEEN);
        nameToPieceType.put("br.svg", Piece.BROOK);
        nameToPieceType.put("bb.svg", Piece.BBISHOP);
        nameToPieceType.put("bn.svg", Piece.BKNIGHT);
        nameToPieceType.put("bp.svg", Piece.BPAWN);

        parseSvgData(cachedWhiteColor, cachedBlackColor);
    }

    /** Re-parse SVG data if piece properties have changed. */
    final void readPrefs(SharedPreferences settings) {
        ColorTheme ct = ColorTheme.instance();
        int whiteColor = ct.getColor(ColorTheme.BRIGHT_PIECE);
        int blackColor = ct.getColor(ColorTheme.DARK_PIECE);
        if (whiteColor != cachedWhiteColor || blackColor != cachedBlackColor) {
            recycleBitmaps();
            parseSvgData(whiteColor, blackColor);
            cachedWhiteColor = whiteColor;
            cachedBlackColor = blackColor;
            cachedSquareSize = -1;
        }
    }

    /** Return a bitmap for the specified piece type and square size. */
    public Bitmap getPieceBitmap(int pType, int sqSize) {
        if (sqSize != cachedSquareSize) {
            recycleBitmaps();
            createBitmaps(sqSize);
            cachedSquareSize = sqSize;
        }
        return bitmapTable[pType];
    }

    private void parseSvgData(int whiteColor, int blackColor) {
        HashMap<Integer,Integer> colorReplace = new HashMap<>();
        colorReplace.put(0xffffffff, whiteColor);
        colorReplace.put(0xff000000, blackColor);
        try {
            ZipInputStream zis = getZipStream();
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    Integer pType = nameToPieceType.get(name);
                    if (pType != null) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = zis.read(buf)) != -1)
                            bos.write(buf, 0, len);
                        buf = bos.toByteArray();
                        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
                        svgTable[pType] = SVGParser.getSVGFromInputStream(bis, colorReplace);
                    }
                }
                zis.closeEntry();
            }
            zis.close();
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read chess pieces data", ex);
        }
    }

    private ZipInputStream getZipStream() throws IOException {
        InputStream is = DroidFishApp.getContext().getAssets().open("pieces/chesscases.zip");
        return new ZipInputStream(is);
    }

    private void recycleBitmaps() {
        for (int i = 0; i < Piece.nPieceTypes; i++) {
            if (bitmapTable[i] != null) {
                bitmapTable[i].recycle();
                bitmapTable[i] = null;
            }
        }
    }

    private void createBitmaps(int sqSize) {
        for (int i = 0; i < Piece.nPieceTypes; i++) {
            SVG svg = svgTable[i];
            if (svg != null) {
                Bitmap bm = Bitmap.createBitmap(sqSize, sqSize, Bitmap.Config.ARGB_8888);
                Canvas bmCanvas = new Canvas(bm);
                bmCanvas.drawPicture(svg.getPicture(), new Rect(0, 0, sqSize, sqSize));
                bitmapTable[i] = bm;
            }
        }
    }
}
