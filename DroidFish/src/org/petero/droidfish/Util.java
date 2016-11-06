package org.petero.droidfish;

import org.petero.droidfish.gamelogic.Piece;
import org.petero.droidfish.gamelogic.Position;

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
                whiteString.append(Piece.toUniCode(Piece.swapColor(p)));
                diff++;
            }
            while (diff > 0) {
                blackString.append(Piece.toUniCode(p));
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
}
