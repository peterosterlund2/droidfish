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
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

abstract class GradientPanel {
    private final static float BORDER_WIDTH_PX = 1;

    protected final RectF rect;
    protected final AHSVColor color;
    protected final float density;
    private final Drawable background;

    private Paint borderPaint = new Paint();
    protected Paint gradientPaint = new Paint();

    /** Constructor. */
    GradientPanel(RectF rect, AHSVColor color, float density, Drawable background) {
        this.rect = rect;
        this.color = color;
        this.density = density;
        this.background = background;
        borderPaint.setColor(0xff6E6E6E);
    }

    boolean contains(Point point) {
        return rect != null && rect.contains(point.x, point.y);
    }

    /** Update color from point. */
    abstract void updateColor(Point point);

    void draw(Canvas canvas) {
        if (rect == null)
            return;

        canvas.drawRect(rect.left   - BORDER_WIDTH_PX,
                        rect.top    - BORDER_WIDTH_PX,
                        rect.right  + BORDER_WIDTH_PX,
                        rect.bottom + BORDER_WIDTH_PX,
                        borderPaint);

        if (background != null)
            background.draw(canvas);

        setGradientPaint();
        canvas.drawRect(rect, gradientPaint);

        drawTracker(canvas);
    }

    /** Set gradientPaint properties. */
    abstract protected void setGradientPaint();

    /** Draw "current color" tracker marker. */
    abstract protected void drawTracker(Canvas canvas);
}
