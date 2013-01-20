package org.petero.droidfish;

import com.larvalabs.svgandroid.SVG;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.PictureDrawable;

/**
 * Like PictureDrawable but scales the picture according to current drawing bounds.
 */
public class SVGPictureDrawable extends PictureDrawable {

    private final int iWidth;
    private final int iHeight;

    private Rect cachedBounds;
    private Bitmap cachedBitmap;

    public SVGPictureDrawable(SVG svg) {
        super(svg.getPicture());
        RectF bounds = svg.getBounds();
        RectF limits = svg.getLimits();
        if (bounds != null) {
            iWidth = (int)bounds.width();
            iHeight = (int)bounds.height();
        } else if (limits != null) {
            iWidth = (int)limits.width();
            iHeight = (int)limits.height();
        } else {
            iWidth = -1;
            iHeight = -1;
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return iWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return iHeight;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (!b.equals(cachedBounds)) {
            Bitmap bm = Bitmap.createBitmap(b.right-b.left, b.bottom-b.top, Bitmap.Config.ARGB_8888);
            Canvas bmCanvas = new Canvas(bm);
            bmCanvas.drawPicture(getPicture(), b);
            cachedBitmap = bm;
            cachedBounds = b;
        }
        canvas.drawBitmap(cachedBitmap, null, b, null);
    }
}
