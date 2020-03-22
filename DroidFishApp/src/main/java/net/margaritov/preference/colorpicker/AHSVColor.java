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

/** Represents a color in HSV float format and an alpha value. */
class AHSVColor {
    private int     alpha = 0xff;
    private float[] hsv = new float[]{360f, 0f, 0f};

    AHSVColor() { }

    /** Set hue,sat,val values. Preserve alpha. */
    void setHSV(float[] hsv) {
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
        float oldHue = hsv[0];
        Color.RGBToHSV(r, g, b, hsv);
        if (hsv[1] <= 0f)
            hsv[0] = oldHue;
    }

    /** Get hue,sat,val values. */
    float[] getHSV() {
        return new float[]{hsv[0], hsv[1], hsv[2]};
    }

    /** Get alpha value. */
    int getAlpha() {
        return alpha;
    }

    /** Get ARGB color value. */
    int getARGB() {
        return Color.HSVToColor(alpha, hsv);
    }
}
