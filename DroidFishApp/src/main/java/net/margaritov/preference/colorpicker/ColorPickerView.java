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
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
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
    private final static float BORDER_WIDTH_PX = 1;

    /** The width in dp of the hue panel. */
    private float HUE_PANEL_WIDTH = 30f;

    /** The height in dp of the alpha panel */
    private float ALPHA_PANEL_HEIGHT = 20f;

    /** The width or height in dp of one of the red/green/blue panels. */
    private float RGB_PANEL_SIZE = 30f;

    /** The distance in dp between the different color panels. */
    private float PANEL_SPACING = 10f;

    private float mDensity = 1f;

    private OnColorChangedListener mListener;

    private AHSVColor color = new AHSVColor();

    /** Offset from the edge we must have or else the finger tracker will
     *  get clipped when it is drawn outside of the view. */
    private float mDrawingOffset;

    /** Distance form the edges of the view of where we are allowed to draw. */
    private RectF mDrawingRect;

    /** Side of the satValPanel square. */
    private float satValSide;

    private GradientPanel satValPanel;
    private GradientPanel huePanel;
    private GradientPanel alphaPanel;
    private GradientPanel[] rgbPanel = new GradientPanel[3];

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
        HUE_PANEL_WIDTH *= mDensity;
        ALPHA_PANEL_HEIGHT *= mDensity;
        RGB_PANEL_SIZE *= mDensity;
        PANEL_SPACING *= mDensity;

        mDrawingOffset = Math.max(5, BORDER_WIDTH_PX) * mDensity * 1.5f;

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /** Return true if the current orientation is landscape. */
    private boolean landScapeView() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawingRect.width() <= 0 || mDrawingRect.height() <= 0)
            return;

        if (satValPanel != null)
            satValPanel.draw(canvas);
        if (huePanel != null)
            huePanel.draw(canvas);
        if (alphaPanel != null)
            alphaPanel.draw(canvas);
        for (int i = 0; i < 3; i++)
            if (rgbPanel[i] != null)
                rgbPanel[i].draw(canvas);
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

        for (GradientPanel pnl : new GradientPanel[]{satValPanel, huePanel, alphaPanel,
                                                     rgbPanel[0], rgbPanel[1], rgbPanel[2]}) {
            if (pnl != null && pnl.contains(mStartTouchPoint)) {
                Point curPnt = new Point((int)event.getX(),
                                         (int)event.getY());
                pnl.updateColor(curPnt);
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthAllowed = MeasureSpec.getSize(widthMeasureSpec);
        int heightAllowed = MeasureSpec.getSize(heightMeasureSpec);

        widthAllowed = chooseSize(widthMode, widthAllowed, getPreferredWidth());
        heightAllowed = chooseSize(heightMode, heightAllowed, getPreferredHeight());

        float side = getSatValSide(widthAllowed, heightAllowed);
        float width = side + getExtraWidth();
        float height = side + getExtraHeight();

        int newWidth  = widthMode  == MeasureSpec.EXACTLY ? widthAllowed  : (int)width;
        int newHeight = heightMode == MeasureSpec.EXACTLY ? heightAllowed : (int)height;
        setMeasuredDimension(newWidth, newHeight);
    }

    private int getPreferredWidth() {
        return (int)(200 * mDensity + getExtraWidth());
    }

    private int getPreferredHeight() {
        return (int)(200 * mDensity + getExtraHeight());
    }

    private int chooseSize(int mode, int size, int preferred) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY)
            return size;
        return preferred; // MeasureSpec.UNSPECIFIED
    }

    /** Compute side of satValPanel given total available width/height. */
    private float getSatValSide(float width, float height) {
        float side1 = width - getExtraWidth();
        float side2 = height - getExtraHeight();
        return Math.min(side1, side2);
    }

    /** Amount of space to the right of the satVal panel. */
    private float getExtraWidth() {
        float ret = PANEL_SPACING + HUE_PANEL_WIDTH;
        if (landScapeView())
            ret += 3 * (PANEL_SPACING + RGB_PANEL_SIZE);
        return ret;
    }

    /** Amount of space below the satVal panel. */
    private float getExtraHeight() {
        float ret = PANEL_SPACING + ALPHA_PANEL_HEIGHT;
        if (!landScapeView())
            ret += 3 * (PANEL_SPACING + RGB_PANEL_SIZE);
        return ret;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mDrawingRect = new RectF();
        mDrawingRect.left   =     mDrawingOffset + getPaddingLeft();
        mDrawingRect.right  = w - mDrawingOffset - getPaddingRight();
        mDrawingRect.top    =     mDrawingOffset + getPaddingTop();
        mDrawingRect.bottom = h - mDrawingOffset - getPaddingBottom();

        satValSide = getSatValSide(mDrawingRect.width(),
                                   mDrawingRect.height());

        setUpSatValPanel();
        setUpHuePanel();
        setUpAlphaPanel();
        setUpRGBPanels();
    }

    private void setUpSatValPanel() {
        RectF dRect = mDrawingRect;
        float b = BORDER_WIDTH_PX;

        float left   = dRect.left + b;
        float right  = left + satValSide - 2 * b;
        float top    = dRect.top + b;
        float bottom = top + satValSide - 2 * b;

        RectF satValRect = new RectF(left,top, right, bottom);
        satValPanel = new SatValGradientPanel(satValRect, color, mDensity);
    }

    private void setUpHuePanel() {
        RectF dRect = mDrawingRect;
        float b = BORDER_WIDTH_PX;

        float left   = dRect.left + satValSide + PANEL_SPACING + b;
        float right  = left + HUE_PANEL_WIDTH - 2 * b;
        float top    = dRect.top + b;
        float bottom = top + satValSide - 2 * b;

        RectF hueRect = new RectF(left, top, right, bottom);
        huePanel = new HueGradientPanel(hueRect, color, mDensity);
    }

    private void setUpAlphaPanel() {
        RectF dRect = mDrawingRect;
        float b = BORDER_WIDTH_PX;

        float left   = dRect.left  + b;
        float right  = dRect.right - b;
        float top    = dRect.top + satValSide + PANEL_SPACING + b;
        float bottom = top + ALPHA_PANEL_HEIGHT - 2 * b;

        RectF alphaRect = new RectF(left, top, right, bottom);
        alphaPanel = new AlphaGradientPanel(alphaRect, color, mDensity);
    }

    private void setUpRGBPanels() {
        RectF dRect = mDrawingRect;
        float b = BORDER_WIDTH_PX;
        float w = RGB_PANEL_SIZE;
        float s = PANEL_SPACING;
        if (!landScapeView()) {
            float offs = dRect.top + satValSide + s + ALPHA_PANEL_HEIGHT;
            for (int i = 0; i < 3; i++) {
                float left   = dRect.left  + b;
                float right  = dRect.right - b;
                float top    = offs + i * (s + w) + s + b;
                float bottom = top + w - 2 * b;
                RectF rgbRect = new RectF(left, top, right, bottom);
                rgbPanel[i] = new RGBGradientPanel(i, rgbRect, color, mDensity, true);
            }
        } else {
            float offs = dRect.left + satValSide + s + HUE_PANEL_WIDTH;
            for (int i = 0; i < 3; i++) {
                float left   = offs + i * (s + w) + s + b;
                float right  = left + w - 2 * b;
                float top    = dRect.top + b;
                float bottom = top + satValSide - 2 * b;
                RectF rgbRect = new RectF(left, top, right, bottom);
                rgbPanel[i] = new RGBGradientPanel(i, rgbRect, color, mDensity, false);
            }
        }
    }

    /**
     * Set a OnColorChangedListener to get notified when the color
     * selected by the user has changed.
     */
    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    /** Get the current color this view is showing. */
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
