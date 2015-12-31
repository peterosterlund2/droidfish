package org.petero.droidfish;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/** A RelativeLayout with the addition that child widgets can ask
 * about the new parent size during onMeasure(). */
public class MyRelativeLayout extends RelativeLayout {
    private int newWidth;
    private int newHeight;

    public MyRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        newWidth = MeasureSpec.getSize(widthMeasureSpec);
        newHeight = MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public int getNewWidth() {
        return newWidth;
    }

    public int getNewHeight() {
        return newHeight;
    }
}
