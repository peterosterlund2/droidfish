package org.petero.droidfish;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

/** A ScrollView that uses at most 75% of the parent height. */
public class MyScrollView extends ScrollView {

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        if (getParent() instanceof View) {
            int parentHeight = ((View)getParent()).getHeight();
            if (parentHeight > 0)
                height = Math.min(height, parentHeight * 3 / 4);
        }
        setMeasuredDimension(width, height);
    }
}
