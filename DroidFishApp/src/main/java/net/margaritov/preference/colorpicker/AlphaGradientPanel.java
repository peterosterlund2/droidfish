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

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

class AlphaGradientPanel extends GradientPanel {
    /** Constructor. */
    AlphaGradientPanel(RectF rect, AHSVColor color, float density) {
        super(rect, color, density, getAlphaPattern(rect, density));
    }

    private static Drawable getAlphaPattern(RectF rect, float density) {
        AlphaPatternDrawable pattern = new AlphaPatternDrawable((int)(5 * density));
        pattern.setBounds(Math.round(rect.left),
                          Math.round(rect.top),
                          Math.round(rect.right),
                          Math.round(rect.bottom));
        return pattern;
    }

    @Override
    protected void setGradientPaint() {
        int rgb = color.getARGB();
        int colorFF = rgb | 0xff000000;
        int color00 = rgb & 0x00ffffff;
        Shader alphaShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top,
                                                colorFF, color00, Shader.TileMode.CLAMP);
        gradientPaint.setShader(alphaShader);
    }

    @Override
    protected void drawTracker(Canvas canvas) {
        Point p = alphaToPoint(color.getAlpha());
        drawRectangleTracker(canvas, p, true);
    }

    @Override
    void updateColor(Point point) {
        int alpha = pointToAlpha(point.x);
        color.setAlpha(alpha);
    }

    private Point alphaToPoint(int alpha) {
        float width = rect.width();
        return new Point((int)(width - (alpha * width / 0xff) + rect.left),
                         (int)rect.top);
    }

    private int pointToAlpha(int x) {
        int width = (int)rect.width();
        x = Math.min(Math.max(x - (int)rect.left, 0), width);
        return 0xff - (x * 0xff / width);
    }
}
