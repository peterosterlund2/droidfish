/*
    DroidFish - An Android chess program.
    Copyright (C) 2019  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.petero.droidfish;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import org.petero.droidfish.gamelogic.Piece;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Handle rendering of chess pieces. */
public class PieceSet {
    private static PieceSet inst = null;

    private HashMap<String,Integer> nameToPieceType;
    private SVG[] svgTable = new SVG[Piece.nPieceTypes];
    private Bitmap[] bitmapTable = new Bitmap[Piece.nPieceTypes];
    private Set<String> availPieceSets;
    private String defaultPieceSet = "chesscases";
    private String cachedPieceSet = defaultPieceSet;
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

        String[] sa = {
                defaultPieceSet,
                "alfonso", "alpha", "cburnett", "chessicons", "chessmonk",
                "freestaunton", "kilfiger", "leipzig", "magnetic", "maya",
                "merida", "merida_new", "metaltops", "pirat", "regular",
                "wikimedia",
        };
        availPieceSets = new HashSet<>(Arrays.asList(sa));

        parseSvgData();
    }

    /** Re-parse SVG data if piece properties have changed. */
    final void readPrefs(SharedPreferences settings) {
        String pieceSet = settings.getString("viewPieceSet", cachedPieceSet);
        boolean modified = !pieceSet.equals(cachedPieceSet);
        if (modified) {
            cachedPieceSet = pieceSet;
            parseSvgData();
        }

        ColorTheme ct = ColorTheme.instance();
        int whiteColor = ct.getColor(ColorTheme.BRIGHT_PIECE);
        int blackColor = ct.getColor(ColorTheme.DARK_PIECE);
        if (modified || whiteColor != cachedWhiteColor || blackColor != cachedBlackColor) {
            recycleBitmaps();
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

    private void parseSvgData() {
        try (ZipInputStream zis = getZipStream()) {
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
                        try {
                            svgTable[pType] = SVG.getFromInputStream(bis);
                        } catch (SVGParseException ignore) {
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Cannot read chess pieces data", ex);
        }
    }

    private ZipInputStream getZipStream() throws IOException {
        String set = availPieceSets.contains(cachedPieceSet) ? cachedPieceSet
                                                             : defaultPieceSet;
        String name = "pieces/" + set + ".zip";
        InputStream is = DroidFishApp.getContext().getAssets().open(name);
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
        Paint colorPaint = new Paint();
        {
            float[] f = new float[3];
            float[] o = new float[3];
            for (int i = 0; i < 3; i++) {
                int shift = 16 - i * 8;
                int w = (cachedWhiteColor >>> shift) & 0xff;
                int b = (cachedBlackColor >>> shift) & 0xff;
                o[i] = b;
                f[i] = (w - b) / (float)255;
            }
            float[] cm = new float[] {
                    f[0], 0   , 0   , 0   , o[0],
                    0   , f[1], 0   , 0   , o[1],
                    0   , 0   , f[2], 0   , o[2],
                    0   , 0   , 0   , 1   , 0
            };
            colorPaint.setColorFilter(new ColorMatrixColorFilter(cm));
        }

        Paint alphaPaint = null;
        int wAlpha = cachedWhiteColor >>> 24;
        int bAlpha = cachedBlackColor >>> 24;
        if (wAlpha != 0xff || bAlpha != 0xff) {
            float o = bAlpha;
            float k = (wAlpha - bAlpha) / (float)255;
            float kr = 0.299f, kg = 0.587f, kb = 0.114f;
            float[] cm = new float[] {
                    0   , 0   , 0   , 0   , 255,
                    0   , 0   , 0   , 0   , 255,
                    0   , 0   , 0   , 0   , 255,
                    kr*k, kg*k, kb*k, 0   , o
            };
            alphaPaint = new Paint();
            alphaPaint.setColorFilter(new ColorMatrixColorFilter(cm));
            alphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        }

        Bitmap svgBM = Bitmap.createBitmap(sqSize, sqSize, Bitmap.Config.ARGB_8888);
        Matrix scaleMat = new Matrix();

        for (int i = 0; i < Piece.nPieceTypes; i++) {
            SVG svg = svgTable[i];
            if (svg != null) {
                svgBM.eraseColor(Color.TRANSPARENT);
                Canvas canvas = new Canvas(svgBM);
                canvas.drawPicture(svg.renderToPicture(), new Rect(0, 0, sqSize, sqSize));

                Bitmap bm = Bitmap.createBitmap(sqSize, sqSize, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(bm);
                canvas.drawBitmap(svgBM, scaleMat, colorPaint);

                if (alphaPaint != null)
                    canvas.drawBitmap(svgBM, scaleMat, alphaPaint);

                bitmapTable[i] = bm;
            }
        }

        svgBM.recycle();
    }
}
