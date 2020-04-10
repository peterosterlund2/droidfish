/*
    DroidFish - An Android chess program.
    Copyright (C) 2016  Peter Österlund, peterosterlund2@gmail.com

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

import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;
import org.petero.droidfish.view.MoveListView;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public final class Util {
    public final static String boldStart;
    public final static String boldStop;

    static {
        // Using bold face causes crashes in android 4.1, see:
        // http://code.google.com/p/android/issues/detail?id=34872
        final int sdkVersion = Build.VERSION.SDK_INT;
        if (sdkVersion == 16) {
            boldStart = "";
            boldStop = "";
        } else {
            boldStart = "<b>";
            boldStop = "</b>";
        }
    }

    /** Represent material difference as two unicode strings. */
    public final static class MaterialDiff {
        public CharSequence white;
        public CharSequence black;
        MaterialDiff(CharSequence w, CharSequence b) {
            white = w;
            black = b;
        }
    }

    /** Compute material difference for a position. */
    public static MaterialDiff getMaterialDiff(Position pos) {
        StringBuilder whiteString = new StringBuilder();
        StringBuilder blackString = new StringBuilder();
        for (int p = Piece.WPAWN; p >= Piece.WKING; p--) {
            int diff = pos.nPieces(p) - pos.nPieces(Piece.swapColor(p));
            while (diff < 0) {
                whiteString.append(PieceFontInfo.toUniCode(Piece.swapColor(p)));
                diff++;
            }
            while (diff > 0) {
                blackString.append(PieceFontInfo.toUniCode(p));
                diff--;
            }
        }
        return new MaterialDiff(whiteString, blackString);
    }

    /** Enable/disable full screen mode for an activity. */
    public static void setFullScreenMode(Activity a, SharedPreferences settings) {
        boolean fullScreenMode = settings.getBoolean("fullScreenMode", false);
        WindowManager.LayoutParams attrs = a.getWindow().getAttributes();
        if (fullScreenMode) {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        } else {
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        a.getWindow().setAttributes(attrs);
    }

    /** Change foreground/background color in a view. */
    public static void overrideViewAttribs(final View v) {
        if (v == null)
            return;
        final int bg = ColorTheme.instance().getColor(ColorTheme.GENERAL_BACKGROUND);
        Object tag = v.getTag();
        final boolean excludedItems = v instanceof Button ||
                                      ((v instanceof EditText) && !(v instanceof MoveListView)) ||
                                      v instanceof ImageButton ||
                                      "title".equals(tag);
        if (!excludedItems) {
            int c = bg;
            if ("thinking".equals(tag)) {
                float[] hsv = new float[3];
                Color.colorToHSV(c, hsv);
                hsv[2] += hsv[2] > 0.5f ? -0.1f : 0.1f;
                c = Color.HSVToColor(Color.alpha(c), hsv);
            }
            v.setBackgroundColor(c);
        }
        if (v instanceof ListView)
            ((ListView) v).setCacheColorHint(bg);
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                overrideViewAttribs(child);
            }
        } else if (!excludedItems && (v instanceof TextView)) {
            int fg = ColorTheme.instance().getColor(ColorTheme.FONT_FOREGROUND);
            ((TextView) v).setTextColor(fg);
        } else if (!excludedItems && (v instanceof MoveListView)) {
            int fg = ColorTheme.instance().getColor(ColorTheme.FONT_FOREGROUND);
            ((MoveListView) v).setTextColor(fg);
        }
    }

    /** Return a hash value for a string, with better quality than String.hashCode(). */
    public static long stringHash(String s) {
        int n = s.length();
        long h = n;
        for (int i = 0; i < n; i += 4) {
            long tmp = s.charAt(i) & 0xffff;
            try {
                tmp = (tmp << 16) | (s.charAt(i+1) & 0xffff);
                tmp = (tmp << 16) | (s.charAt(i+2) & 0xffff);
                tmp = (tmp << 16) | (s.charAt(i+3) & 0xffff);
            } catch (IndexOutOfBoundsException ignore) {}

            h += tmp;

            h *= 0x7CF9ADC6FE4A7653L;
            h ^= h >>> 37;
            h *= 0xC25D3F49433E7607L;
            h ^= h >>> 43;
        }
        return h;
    }
}
