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
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;

public class HueGradientPanel extends GradientPanel {
    /** Constructor. */
    HueGradientPanel(RectF rect, AHSVColor color, float density) {
        super(rect, color, density, null);
        Shader hueShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
                                              buildHueColorArray(), null, Shader.TileMode.CLAMP);
        gradientPaint.setShader(hueShader);
    }

    private int[] buildHueColorArray() {
        int[] hue = new int[361];
        for (int i = hue.length - 1, count = 0; i >= 0; i--, count++)
            hue[count] = Color.HSVToColor(new float[]{i, 1f, 1f});
        return hue;
    }

    @Override
    protected void setGradientPaint() {
    }

    @Override
    protected void drawTracker(Canvas canvas) {
        Point p = hueToPoint(color.getHSV()[0]);
        drawRectangleTracker(canvas, p, false);
    }

    @Override
    void updateColor(Point point) {
        double[] hsv = color.getHSV();
        hsv[0] = pointToHue(point);
        color.setHSV(hsv);
    }

    private Point hueToPoint(double hue) {
        double height = rect.height();
        return new Point(Math.round(rect.left),
                         (int)Math.round((height - (hue * height / 360) + rect.top)));
    }

    private double pointToHue(Point p) {
        double height = rect.height();
        double y = Math.min(Math.max(p.y - rect.top, 0f), height);
        return 360 - (y * 360 / height);
    }
}
