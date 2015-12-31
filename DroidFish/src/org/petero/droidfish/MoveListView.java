/*
    DroidFish - An Android chess program.
    Copyright (C) 2015  Peter Österlund, peterosterlund2@gmail.com

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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

/** Custom view for displaying move list.
 *  This is much faster than using a TextView. */
public class MoveListView extends View {
    private CharSequence text = null;
    private Layout layout = null;
    private int layoutWidth = -1;
    private TextPaint textPaint;
    private Typeface defaultTypeface;

    /** Constructor. */
    public MoveListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.density = getResources().getDisplayMetrics().density;
        defaultTypeface = Typeface.create("monospace", Typeface.NORMAL);
        textPaint.setTypeface(defaultTypeface);
    }

    /** Set text to display. */
    public void setText(CharSequence text) {
        if (text != this.text) {
            this.text = text;
            createLayout(getWidth());
            requestLayout();
        }
        invalidate();
    }

    /** Set typeface and text size. If tf is null the default typeface is used. */
    public void setTypeface(Typeface tf, float size) {
        if (tf == null)
            tf = defaultTypeface;
        boolean modified = false;
        if (tf != textPaint.getTypeface()) {
            textPaint.setTypeface(tf);
            modified = true;
        }
        DisplayMetrics metric = getContext().getResources().getDisplayMetrics();
        size *= metric.scaledDensity;
        if (size != textPaint.getTextSize()) {
            textPaint.setTextSize(size);
            modified = true;
        }
        if (modified) {
            createLayout(getWidth());
            requestLayout();
            invalidate();
        }
    }

    public void setTextColor(int color) {
        if (color != textPaint.getColor()) {
            textPaint.setColor(color);
            invalidate();
        }
    }

    /** Get line number corresponding to a character offset,
     *  or -1 if layout has not been created yet. */
    public int getLineForOffset(int currPos) {
        if (layout == null)
            return -1;
        return layout.getLineForOffset(currPos);
    }

    /** Get line height in pixels. */
    public int getLineHeight() {
        return textPaint.getFontMetricsInt(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMeasure = MeasureSpec.getSize(widthMeasureSpec);
        int heightMeasure = MeasureSpec.getSize(heightMeasureSpec);

        int width = getMeasuredWidth();
        switch (MeasureSpec.getMode(widthMeasureSpec)) {
        case MeasureSpec.UNSPECIFIED:
            break;
        case MeasureSpec.EXACTLY:
            width = widthMeasure;
            break;
        case MeasureSpec.AT_MOST:
            width = Math.min(width, widthMeasure);
            break;
        }

        if (width != layoutWidth)
            createLayout(width);

        int height = 0;
        if (layout != null) {
            height = layout.getLineCount() * getLineHeight();
            ViewParent p = getParent();
            if (p != null)
                p = p.getParent();
            if (p instanceof MyRelativeLayout)
                height += -getLineHeight() + ((MyRelativeLayout)p).getNewHeight();
        }
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
        case MeasureSpec.UNSPECIFIED:
            break;
        case MeasureSpec.EXACTLY:
            height = heightMeasure;
            break;
        case MeasureSpec.AT_MOST:
            height = Math.min(height, heightMeasure);
            break;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (layout != null) {
            canvas.save();
            canvas.translate(getPaddingLeft(), getPaddingTop());
            layout.draw(canvas);
            canvas.restore();
        }
    }

    public interface OnLinkClickListener {
        boolean onLinkClick(int offs);
    }
    private OnLinkClickListener onLinkClickListener;

    public void setOnLinkClickListener(OnLinkClickListener listener) {
        onLinkClickListener = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        boolean ret = super.onTouchEvent(event);
        if ((action == MotionEvent.ACTION_UP) && (layout != null) &&
            (onLinkClickListener != null)) {
            int x = (int)event.getX() - getPaddingLeft() + getScrollX();
            int y = (int)event.getY() - getPaddingTop()  + getScrollY();
            int line = layout.getLineForVertical(y);
            int offs = layout.getOffsetForHorizontal(line, x);
            if (onLinkClickListener.onLinkClick(offs))
                return true;
        }
        return ret;
    }

    /** Create a StaticLayout corresponding to the current text. */
    private void createLayout(int width) {
        if (width <= 0)
            return;
        if (text == null) {
            layout = null;
            layoutWidth = -1;
        } else {
            layout = new StaticLayout(text, textPaint, width,
                    Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
            layoutWidth = width;
        }
    }
}
