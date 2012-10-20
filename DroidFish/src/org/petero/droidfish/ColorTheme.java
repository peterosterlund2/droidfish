/*
    DroidFish - An Android chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

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
import android.content.SharedPreferences.Editor;
import android.graphics.Color;

public class ColorTheme {
    private static ColorTheme inst = null;

    /** Get singleton instance. */
    public static final ColorTheme instance() {
        if (inst == null)
            inst = new ColorTheme();
        return inst;
    }

    final static int DARK_SQUARE = 0;
    final static int BRIGHT_SQUARE = 1;
    final static int SELECTED_SQUARE = 2;
    final static int CURSOR_SQUARE = 3;
    final static int DARK_PIECE = 4;
    final static int BRIGHT_PIECE = 5;
    final static int CURRENT_MOVE = 6;
    final static int ARROW_0 = 7;
    final static int ARROW_1 = 8;
    final static int ARROW_2 = 9;
    final static int ARROW_3 = 10;
    final static int ARROW_4 = 11;
    final static int ARROW_5 = 12;
    final static int SQUARE_LABEL = 13;
    final static int DECORATION = 14;
    final static int PGN_COMMENT = 15;
    public final static int FONT_FOREGROUND = 16;
    public final static int GENERAL_BACKGROUND = 17;
    private final static int numColors = 18;

    private int colorTable[] = new int[numColors];

    private static final String[] prefNames = {
        "darkSquare", "brightSquare", "selectedSquare", "cursorSquare", "darkPiece", "brightPiece", "currentMove",
        "arrow0", "arrow1", "arrow2", "arrow3", "arrow4", "arrow5", "squareLabel", "decoration", "pgnComment",
        "fontForeground", "generalBackground"
    };
    private static final String prefPrefix = "color_";

    private final static int defaultTheme = 2;
    final static int[] themeNames = {
        R.string.colortheme_original,
        R.string.colortheme_xboard,
        R.string.colortheme_blue,
        R.string.colortheme_grey,
        R.string.colortheme_scid_default,
        R.string.colortheme_scid_brown,
        R.string.colortheme_scid_green
    };
    private final static String themeColors[][] = {
    { // Original
        "#FF808080", "#FFBEBE5A", "#FFFF0000", "#FF00FF00", "#FF000000", "#FFFFFFFF", "#FF888888",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#1E1F1FFF", "#28FF1F1F", "#FFFF0000",
        "#FF9F9F66", "#FFC0C000", "#FFF7FBC6", "#FF292C10"
    },
    { // XBoard
        "#FF77A26D", "#FFC8C365", "#FFFFFF00", "#FF00FF00", "#FF202020", "#FFFFFFCC", "#FF6B9262",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#1E1F1FFF", "#28FF1F1F", "#FFFF0000",
        "#FF808080", "#FFC0C000", "#FFEFFBBC", "#FF28320C"
    },
    { // Blue
        "#FF83A5D2", "#FFFFFFFA", "#FF3232D1", "#FF5F5FFD", "#FF282828", "#FFF0F0F0", "#FF3333FF",
        "#A01F1FFF", "#A01FFF1F", "#501F1FFF", "#501FFF1F", "#1E1F1FFF", "#281FFF1F", "#FFFF0000",
        "#FF808080", "#FFC0C000", "#FFFFFF00", "#FF2E2B53"
    },
    { // Grey
        "#FF666666", "#FFDDDDDD", "#FFFF0000", "#FF0000FF", "#FF000000", "#FFFFFFFF", "#FF888888",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#1E1F1FFF", "#28FF1F1F", "#FFFF0000",
        "#FF909090", "#FFC0C000", "#FFFFFFFF", "#FF202020"
    },
    { // Scid Default
        "#FF80A0A0", "#FFD0E0D0", "#FFFF0000", "#FF00FF00", "#FF000000", "#FFFFFFFF", "#FF666666",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#1E1F1FFF", "#28FF1F1F", "#FFFF0000",
        "#FF808080", "#FFC0C000", "#FFDEFBDE", "#FF213429"
    },
    { // Scid Brown
        "#B58863",   "#F0D9B5",   "#FFFF0000", "#FF00FF00", "#FF000000", "#FFFFFFFF", "#FF666666",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#1E1F1FFF", "#28FF1F1F", "#FFFF0000",
        "#FF808080", "#FFC0C000", "#FFF7FAE3", "#FF40260A"
    },
    { // Scid Green
        "#FF769656", "#FFEEEED2", "#FFFF0000", "#FF0000FF", "#FF000000", "#FFFFFFFF", "#FF666666",
        "#A01F1FFF", "#A0FF1F1F", "#501F1FFF", "#50FF1F1F", "#1E1F1FFF", "#28FF1F1F", "#FFFF0000",
        "#FF808080", "#FFC0C000", "#FFDEE3CE", "#FF183C21"
    }
    };

    final void readColors(SharedPreferences settings) {
        for (int i = 0; i < numColors; i++) {
            String prefName = prefPrefix + prefNames[i];
            String defaultColor = themeColors[defaultTheme][i];
            String colorString = settings.getString(prefName, defaultColor);
            colorTable[i] = 0;
            try {
                colorTable[i] = Color.parseColor(colorString);
            } catch (IllegalArgumentException e) {
            } catch (StringIndexOutOfBoundsException e) {
            }
        }
    }

    final void setTheme(SharedPreferences settings, int themeType) {
        Editor editor = settings.edit();
        for (int i = 0; i < numColors; i++)
            editor.putString(prefPrefix + prefNames[i], themeColors[themeType][i]);
        editor.commit();
        readColors(settings);
    }

    public final int getColor(int colorType) {
        return colorTable[colorType];
    }
}
