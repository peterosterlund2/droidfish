/*
 * Copyright (C) 2020 Peter Österlund
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.margaritov.preference.colorpicker;

import android.graphics.Color;

/** Represents a color in HSV double format and an integer alpha value (0-255). */
class AHSVColor {
    private int alpha = 0xff;
    private double[] hsv = new double[]{360, 0, 0};

    AHSVColor() { }

    /** Set hue,sat,val values. Preserve alpha. */
    void setHSV(double[] hsv) {
        this.hsv[0] = hsv[0];
        this.hsv[1] = hsv[1];
        this.hsv[2] = hsv[2];
    }

    /** Set alpha value. Preserve hue,sat,val. */
    void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    /** Set ARGB color value. */
    void setARGB(int color) {
        alpha = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        double oldHue = hsv[0];
        ARGBToHSV(Color.rgb(r, g, b), hsv);
        if (hsv[1] <= 0f)
            hsv[0] = oldHue;
    }

    /** Set red (0), green (1) or blue (2) color component. */
    void setRGBComponent(int component, int value) {
        int c = getARGB();
        switch (component) {
        case 0:
            c = (c & 0xff00ffff) | (value << 16);
            break;
        case 1:
            c = (c & 0xffff00ff) | (value << 8);
            break;
        case 2:
            c = (c & 0xffffff00) | value;
            break;
        }
        setARGB(c);
    }

    /** Get hue,sat,val values. */
    double[] getHSV() {
        return new double[]{hsv[0], hsv[1], hsv[2]};
    }

    /** Get alpha value. */
    int getAlpha() {
        return alpha;
    }

    /** Get ARGB color value. */
    int getARGB() {
        return HSVToARGB(alpha, hsv);
    }

    /** Get red (0), green (1), or blue (2) color component. */
    int getRGBComponent(int component) {
        int c = getARGB();
        switch (component) {
        case 0: return Color.red(c);
        case 1: return Color.green(c);
        case 2: return Color.blue(c);
        default: throw new RuntimeException("Internal error");
        }
    }

    private static int HSVToARGB(int alpha, double[] hsv) {
        double h = hsv[0] % 360;
        double s = hsv[1];
        double v = hsv[2];
        double c = v * s;
        double m = v - c;
        double x = c * (1 - Math.abs(((h / 60.0) % 2) - 1));

        double r = 0, g = 0, b = 0;
        switch ((int)Math.floor(h / 60.0)) {
        case 0: r = c; g = x;        break;
        case 1: r = x; g = c;        break;
        case 2:        g = c; b = x; break;
        case 3:        g = x; b = c; break;
        case 4: r = x;        b = c; break;
        case 5: r = c;        b = x; break;
        }

        int red   = Math.min(Math.max((int)Math.round((r + m) * 255), 0), 255);
        int green = Math.min(Math.max((int)Math.round((g + m) * 255), 0), 255);
        int blue  = Math.min(Math.max((int)Math.round((b + m) * 255), 0), 255);

        return Color.argb(alpha, red, green, blue);
    }

    private static void ARGBToHSV(int color, double[] hsv) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        double r = red / 255.0;
        double g = green / 255.0;
        double b = blue / 255.0;

        int maxI = 0;
        double cMax = r;
        if (cMax < g) {
            cMax = g;
            maxI = 1;
        }
        if (cMax < b) {
            cMax = b;
            maxI = 2;
        }

        double cMin = Math.min(Math.min(r, g), b);
        double d = cMax - cMin;

        double v = cMax;
        double s = (cMax > 0) ? (d / cMax) : 0.0;
        double h;
        if (d <= 0) {
            h = 0;
        } else if (maxI == 0) {
            h = 60 * (g - b) / d;
            if (h < 0)
                h += 360;
        } else if (maxI == 1) {
            h = 60 * ((b - r) / d + 2);
        } else {
            h = 60 * ((r - g) / d + 4);
        }

        hsv[0] = h;
        hsv[1] = s;
        hsv[2] = v;
    }
}
