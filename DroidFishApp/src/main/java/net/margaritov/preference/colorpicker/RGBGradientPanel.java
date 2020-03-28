package net.margaritov.preference.colorpicker;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;

public class RGBGradientPanel extends GradientPanel {
    private final int component; // 0=red, 1=green, 2=blue
    private final int colorMask;
    private final boolean horizontal;

    /** Constructor. */
    RGBGradientPanel(int component, RectF rect, AHSVColor color, float density,
                     boolean horizontal) {
        super(rect, color, density, null);
        this.component = component;
        switch (component) {
        case 0: colorMask = 0x00ff0000; break;
        case 1: colorMask = 0x0000ff00; break;
        case 2: colorMask = 0x000000ff; break;
        default: colorMask = 0; break;
        }
        this.horizontal = horizontal;
    }

    @Override
    protected void setGradientPaint() {
        int rgb = color.getARGB();
        int color00 = (rgb & ~colorMask) | 0xff000000;
        int colorFF = (rgb |  colorMask) | 0xff000000;
        Shader rgbShader;
        if (horizontal) {
            rgbShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top,
                                           color00, colorFF, Shader.TileMode.CLAMP);
        } else {
            rgbShader = new LinearGradient(rect.left, rect.bottom, rect.left, rect.top,
                                           color00, colorFF, Shader.TileMode.CLAMP);
        }
        gradientPaint.setShader(rgbShader);
    }

    protected void drawTracker(Canvas canvas) {
        int val = color.getRGBComponent(component);
        Point p = rgbComponentToPoint(val);
        drawRectangleTracker(canvas, p, horizontal);
    }

    @Override
    void updateColor(Point point) {
        int rgbVal = pointToRgbComponent(point);
        color.setRGBComponent(component, rgbVal);
    }

    private Point rgbComponentToPoint(int val) {
        if (horizontal) {
            float width = rect.width();
            return new Point((int)((val * width / 0xff) + rect.left),
                             (int)rect.top);
        } else {
            float height = rect.height();
            return new Point((int)rect.left,
                             (int)(rect.bottom - (val * height / 0xff)));
        }
    }

    private int pointToRgbComponent(Point p) {
        if (horizontal) {
            int width = (int)rect.width();
            int x = Math.min(Math.max(p.x - (int)rect.left, 0), width);
            return x * 0xff / width;
        } else {
            int height = (int)rect.height();
            int y = Math.min(Math.max((int)rect.bottom - p.y, 0), height);
            return y * 0xff / height;
        }
    }
}
