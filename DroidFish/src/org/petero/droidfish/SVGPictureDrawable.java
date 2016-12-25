/*
    DroidFish - An Android chess program.
    Copyright (C) 2012-2013,2016  Peter Österlund, peterosterlund2@gmail.com

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
