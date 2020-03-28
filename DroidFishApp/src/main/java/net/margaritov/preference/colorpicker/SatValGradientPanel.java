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
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;

public class SatValGradientPanel extends GradientPanel {
    private float PALETTE_CIRCLE_TRACKER_RADIUS = 5f;

    private Shader valShader;

    /** Constructor. */
    SatValGradientPanel(RectF rect, AHSVColor color, float density) {
        super(rect, color, density, null);

        PALETTE_CIRCLE_TRACKER_RADIUS *= density;

        valShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
                                       0xffffffff, 0xff000000, Shader.TileMode.CLAMP);
    }

    @Override
    protected void setGradientPaint() {
        float[] hsv = color.getHSV();
        hsv[1] = 1f;
        hsv[2] = 1f;
        AHSVColor hue = new AHSVColor();
        hue.setHSV(hsv);
        Shader satShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top,
                                              0xffffffff, hue.getARGB(), Shader.TileMode.CLAMP);
        ComposeShader shader = new ComposeShader(valShader, satShader, PorterDuff.Mode.MULTIPLY);
        gradientPaint.setShader(shader);
    }

    @Override
    protected void drawTracker(Canvas canvas) {
        float[] hsv = color.getHSV();
        Point p = satValToPoint(hsv[1], hsv[2]);

        float r = PALETTE_CIRCLE_TRACKER_RADIUS;
        trackerPaint.setColor(0xff000000);
        canvas.drawCircle(p.x, p.y, r - 1f * density, trackerPaint);
        trackerPaint.setColor(0xffdddddd);
        canvas.drawCircle(p.x, p.y, r, trackerPaint);
    }

    @Override
    void updateColor(Point point) {
        float[] hsv = color.getHSV();
        float[] result = pointToSatVal(point.x, point.y);
        hsv[1] = result[0];
        hsv[2] = result[1];
        color.setHSV(hsv);
    }

    private Point satValToPoint(float sat, float val) {
        final float width = rect.width();
        final float height = rect.height();

        return new Point((int)(sat * width + rect.left),
                         (int)((1f - val) * height + rect.top));
    }

    private float[] pointToSatVal(float x, float y) {
        float width = rect.width();
        float height = rect.height();

        x = Math.min(Math.max(x - rect.left, 0f), width);
        y = Math.min(Math.max(y - rect.top, 0f), height);

        return new float[]{ x / width, 1f - y / height };
    }
}
