/*
 * Copyright (C) 2010 Daniel Nilsson
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Paint.Style;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Displays a color picker to the user and allow them to select a color.
 * @author Daniel Nilsson
 */
@SuppressLint("ClickableViewAccessibility")
public class ColorPickerView extends View {
    /** The width in pixels of the border surrounding all color panels. */
    private final static float    BORDER_WIDTH_PX = 1;

    /** The width in dp of the hue panel. */
    private float         HUE_PANEL_WIDTH = 30f;

    /** The height in dp of the alpha panel */
    private float         ALPHA_PANEL_HEIGHT = 20f;

    /** The distance in dp between the different color panels. */
    private float         PANEL_SPACING = 10f;

    /** The radius in dp of the color palette tracker circle. */
    private float         PALETTE_CIRCLE_TRACKER_RADIUS = 5f;

    /** The dp which the tracker of the hue or alpha panel will extend outside of its bounds. */
    private float         RECTANGLE_TRACKER_OFFSET = 2f;


    private float         mDensity = 1f;

    private OnColorChangedListener    mListener;

    private Paint         mSatValPaint = new Paint();
    private Paint         mSatValTrackerPaint = new Paint();

    private Paint         mHuePaint = new Paint();
    private Paint         mHueTrackerPaint = new Paint();

    private Paint         mAlphaPaint = new Paint();
    private Paint         mBorderPaint = new Paint();

    private Shader        mValShader;
    private Shader        mHueShader;

    private AHSVColor     color = new AHSVColor();

    private final int     mBorderColor = 0xff6E6E6E;

    /** Offset from the edge we must have or else the finger tracker will
     *  get clipped when it is drawn outside of the view. */
    private float         mDrawingOffset;

    /** Distance form the edges of the view of where we are allowed to draw. */
    private RectF mDrawingRect;

    private RectF mSatValRect;
    private RectF mHueRect;
    private RectF mAlphaRect;

    private AlphaPatternDrawable mAlphaPattern;

    private Point mStartTouchPoint = null;

    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mDensity = getContext().getResources().getDisplayMetrics().density;
        PALETTE_CIRCLE_TRACKER_RADIUS *= mDensity;
        RECTANGLE_TRACKER_OFFSET *= mDensity;
        HUE_PANEL_WIDTH *= mDensity;
        ALPHA_PANEL_HEIGHT *= mDensity;
        PANEL_SPACING *= mDensity;

        mDrawingOffset = calculateRequiredOffset();

        initPaintTools();

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    private void initPaintTools() {
        mSatValTrackerPaint.setStyle(Style.STROKE);
        mSatValTrackerPaint.setStrokeWidth(2f * mDensity);
        mSatValTrackerPaint.setAntiAlias(true);

        mHueTrackerPaint.setColor(0xff1c1c1c);
        mHueTrackerPaint.setStyle(Style.STROKE);
        mHueTrackerPaint.setStrokeWidth(2f * mDensity);
        mHueTrackerPaint.setAntiAlias(true);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private float calculateRequiredOffset() {
        float offset = Math.max(PALETTE_CIRCLE_TRACKER_RADIUS, RECTANGLE_TRACKER_OFFSET);
        offset = Math.max(offset, BORDER_WIDTH_PX * mDensity);

        return offset * 1.5f;
    }

    private int[] buildHueColorArray() {
        int[] hue = new int[361];

        int count = 0;
        for (int i = hue.length - 1; i >= 0; i--, count++) {
            hue[count] = Color.HSVToColor(new float[]{i, 1f, 1f});
        }

        return hue;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawingRect.width() <= 0 || mDrawingRect.height() <= 0)
            return;

        drawSatValPanel(canvas);
        drawHuePanel(canvas);
        drawAlphaPanel(canvas);
    }

    private void drawSatValPanel(Canvas canvas) {
        final RectF    rect = mSatValRect;

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.setColor(mBorderColor);
            canvas.drawRect(mDrawingRect.left, mDrawingRect.top, rect.right + BORDER_WIDTH_PX,
                            rect.bottom + BORDER_WIDTH_PX, mBorderPaint);
        }

        if (mValShader == null)
            mValShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
                    0xffffffff, 0xff000000, TileMode.CLAMP);

        float[] hsv = color.getHSV();
        hsv[1] = 1f;
        hsv[2] = 1f;
        AHSVColor hue = new AHSVColor();
        hue.setHSV(hsv);
        Shader mSatShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top,
                                               0xffffffff, hue.getARGB(), TileMode.CLAMP);
        ComposeShader mShader = new ComposeShader(mValShader, mSatShader, PorterDuff.Mode.MULTIPLY);
        mSatValPaint.setShader(mShader);

        canvas.drawRect(rect, mSatValPaint);

        hsv = color.getHSV();
        Point p = satValToPoint(hsv[1], hsv[2]);

        float r = PALETTE_CIRCLE_TRACKER_RADIUS;
        mSatValTrackerPaint.setColor(0xff000000);
        canvas.drawCircle(p.x, p.y, r - 1f * mDensity, mSatValTrackerPaint);
        mSatValTrackerPaint.setColor(0xffdddddd);
        canvas.drawCircle(p.x, p.y, r, mSatValTrackerPaint);
    }

    private void drawHuePanel(Canvas canvas) {
        final RectF rect = mHueRect;

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.setColor(mBorderColor);
            canvas.drawRect(rect.left - BORDER_WIDTH_PX,
                            rect.top - BORDER_WIDTH_PX,
                            rect.right + BORDER_WIDTH_PX,
                            rect.bottom + BORDER_WIDTH_PX,
                            mBorderPaint);
        }

        if (mHueShader == null) {
            mHueShader = new LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
                                            buildHueColorArray(), null, TileMode.CLAMP);
            mHuePaint.setShader(mHueShader);
        }

        canvas.drawRect(rect, mHuePaint);

        float rectHeight = 4 * mDensity / 2;

        Point p = hueToPoint(color.getHSV()[0]);

        RectF r = new RectF();
        r.left = rect.left - RECTANGLE_TRACKER_OFFSET;
        r.right = rect.right + RECTANGLE_TRACKER_OFFSET;
        r.top = p.y - rectHeight;
        r.bottom = p.y + rectHeight;

        canvas.drawRoundRect(r, 2, 2, mHueTrackerPaint);
    }

    private void drawAlphaPanel(Canvas canvas) {
        if (mAlphaRect == null || mAlphaPattern == null)
            return;

        final RectF rect = mAlphaRect;

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.setColor(mBorderColor);
            canvas.drawRect(rect.left - BORDER_WIDTH_PX,
                            rect.top - BORDER_WIDTH_PX,
                            rect.right + BORDER_WIDTH_PX,
                            rect.bottom + BORDER_WIDTH_PX,
                            mBorderPaint);
        }

        mAlphaPattern.draw(canvas);

        int rgb = color.getARGB();
        int colorFF = rgb | 0xff000000;
        int color00 = rgb & 0x00ffffff;
        Shader mAlphaShader = new LinearGradient(rect.left, rect.top, rect.right, rect.top,
                                                 colorFF, color00, TileMode.CLAMP);

        mAlphaPaint.setShader(mAlphaShader);

        canvas.drawRect(rect, mAlphaPaint);

        float rectWidth = 4 * mDensity / 2;

        Point p = alphaToPoint(color.getAlpha());

        RectF r = new RectF();
        r.left = p.x - rectWidth;
        r.right = p.x + rectWidth;
        r.top = rect.top - RECTANGLE_TRACKER_OFFSET;
        r.bottom = rect.bottom + RECTANGLE_TRACKER_OFFSET;

        canvas.drawRoundRect(r, 2, 2, mHueTrackerPaint);
    }

    private Point hueToPoint(float hue) {
        final RectF rect = mHueRect;
        final float height = rect.height();

        return new Point((int) rect.left,
                         (int) (height - (hue * height / 360f) + rect.top));
    }

    private Point satValToPoint(float sat, float val) {
        final RectF rect = mSatValRect;
        final float width = rect.width();
        final float height = rect.height();

        return new Point((int) (sat * width + rect.left),
                         (int) ((1f - val) * height + rect.top));
    }

    private Point alphaToPoint(int alpha) {
        final RectF rect = mAlphaRect;
        final float width = rect.width();

        return new Point((int) (width - (alpha * width / 0xff) + rect.left),
                         (int) rect.top);
    }

    private static float clampF(float val, float min, float max) {
        return Math.min(Math.max(val, min), max);
    }

    private float[] pointToSatVal(float x, float y) {
        final RectF rect = mSatValRect;

        float width = rect.width();
        float height = rect.height();

        x = clampF(x - rect.left, 0f, width);
        y = clampF(y - rect.top, 0f, height);

        return new float[]{ x / width, 1f - y / height };
    }

    private float pointToHue(float y) {
        final RectF rect = mHueRect;

        float height = rect.height();
        y = clampF(y - rect.top, 0f, height);

        return 360f - (y * 360f / height);
    }

    private int pointToAlpha(int x) {
        final RectF rect = mAlphaRect;

        final int width = (int) rect.width();
        x = Math.min(Math.max(x - (int)rect.left, 0), width);

        return 0xff - (x * 0xff / width);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean update = false;
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mStartTouchPoint = new Point((int)event.getX(), (int)event.getY());
            update = moveTrackersIfNeeded(event);
            break;
        case MotionEvent.ACTION_MOVE:
            update = moveTrackersIfNeeded(event);
            break;
        case MotionEvent.ACTION_UP:
            mStartTouchPoint = null;
            update = moveTrackersIfNeeded(event);
            break;
        }

        if (update) {
            if (mListener != null)
                mListener.onColorChanged(color.getARGB());
            invalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean moveTrackersIfNeeded(MotionEvent event) {
        if (mStartTouchPoint == null)
            return false;

        boolean update = false;
        int startX = mStartTouchPoint.x;
        int startY = mStartTouchPoint.y;

        float[] hsv = color.getHSV();
        if (mHueRect.contains(startX, startY)) {
            hsv[0] = pointToHue(event.getY());
            color.setHSV(hsv);
            update = true;
        } else if (mSatValRect.contains(startX, startY)) {
            float[] result = pointToSatVal(event.getX(), event.getY());
            hsv[1] = result[0];
            hsv[2] = result[1];
            color.setHSV(hsv);
            update = true;
        } else if (mAlphaRect != null && mAlphaRect.contains(startX, startY)) {
            int alpha = pointToAlpha((int)event.getX());
            color.setAlpha(alpha);
            update = true;
        }

        return update;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        
        int widthAllowed = MeasureSpec.getSize(widthMeasureSpec);
        int heightAllowed = MeasureSpec.getSize(heightMeasureSpec);
        
        widthAllowed = chooseWidth(widthMode, widthAllowed);
        heightAllowed = chooseHeight(heightMode, heightAllowed);

        int width = (int) (heightAllowed - ALPHA_PANEL_HEIGHT + HUE_PANEL_WIDTH);
        int height;
        if (width > widthAllowed) {
            width = widthAllowed;
            height = (int) (widthAllowed - HUE_PANEL_WIDTH + ALPHA_PANEL_HEIGHT);
        } else {
            height = heightAllowed;
        }

        setMeasuredDimension(width, height);
    }

    private int chooseWidth(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            return size;
        } else { // (mode == MeasureSpec.UNSPECIFIED)
            return getPreferredWidth();
        }
    }

    private int chooseHeight(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            return size;
        } else { // (mode == MeasureSpec.UNSPECIFIED)
            return getPreferredHeight();
        }
    }

    private int getPreferredWidth() {
        int width = getPreferredHeight();
        width -= PANEL_SPACING + ALPHA_PANEL_HEIGHT;
        return (int) (width + HUE_PANEL_WIDTH + PANEL_SPACING);
    }

    private int getPreferredHeight() {
        int height = (int)(200 * mDensity);
        height += PANEL_SPACING + ALPHA_PANEL_HEIGHT;
        return height;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mDrawingRect = new RectF();
        mDrawingRect.left = mDrawingOffset + getPaddingLeft();
        mDrawingRect.right  = w - mDrawingOffset - getPaddingRight();
        mDrawingRect.top = mDrawingOffset + getPaddingTop();
        mDrawingRect.bottom = h - mDrawingOffset - getPaddingBottom();

        setUpSatValRect();
        setUpHueRect();
        setUpAlphaRect();
    }

    private void setUpSatValRect() {
        final RectF    dRect = mDrawingRect;
        float panelSide = dRect.height() - BORDER_WIDTH_PX * 2;
        panelSide -= PANEL_SPACING + ALPHA_PANEL_HEIGHT;

        float left = dRect.left + BORDER_WIDTH_PX;
        float top = dRect.top + BORDER_WIDTH_PX;
        float bottom = top + panelSide;
        float right = left + panelSide;

        mSatValRect = new RectF(left,top, right, bottom);
    }

    private void setUpHueRect() {
        final RectF    dRect = mDrawingRect;

        float left = dRect.right - HUE_PANEL_WIDTH + BORDER_WIDTH_PX;
        float top = dRect.top + BORDER_WIDTH_PX;
        float bottom = dRect.bottom - BORDER_WIDTH_PX - (PANEL_SPACING + ALPHA_PANEL_HEIGHT);
        float right = dRect.right - BORDER_WIDTH_PX;

        mHueRect = new RectF(left, top, right, bottom);
    }

    private void setUpAlphaRect() {
        final RectF    dRect = mDrawingRect;

        float left = dRect.left + BORDER_WIDTH_PX;
        float top = dRect.bottom - ALPHA_PANEL_HEIGHT + BORDER_WIDTH_PX;
        float bottom = dRect.bottom - BORDER_WIDTH_PX;
        float right = dRect.right - BORDER_WIDTH_PX;

        mAlphaRect = new RectF(left, top, right, bottom);

        mAlphaPattern = new AlphaPatternDrawable((int) (5 * mDensity));
        mAlphaPattern.setBounds(Math.round(mAlphaRect.left),
                                Math.round(mAlphaRect.top),
                                Math.round(mAlphaRect.right),
                                Math.round(mAlphaRect.bottom));
    }

    /**
     * Set a OnColorChangedListener to get notified when the color
     * selected by the user has changed.
     */
    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    /**
     * Get the current color this view is showing.
     * @return the current color.
     */
    public int getColor() {
        return color.getARGB();
    }

    /**
     * Set the color this view should show.
     * @param colorARGB The color that should be selected.
     * @param callback  If you want to get a callback to your OnColorChangedListener.
     */
    public void setColor(int colorARGB, boolean callback) {
        color.setARGB(colorARGB);
        if (callback && mListener != null)
            mListener.onColorChanged(color.getARGB());
        invalidate();
    }

    /**
     * Get the drawing offset of the color picker view.
     * The drawing offset is the distance from the side of
     * a panel to the side of the view minus the padding.
     * Useful if you want to have your own panel below showing
     * the currently selected color and want to align it perfectly.
     * @return The offset in pixels.
     */
    public float getDrawingOffset() {
        return mDrawingOffset;
    }
}
