package org.louiswilliams.queueupplayer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by Louis on 5/26/2015.
 */
public class SlidingFrameLayout  extends FrameLayout{

    private static final String TAG = SlidingFrameLayout.class.getName();

    public SlidingFrameLayout(Context context) {
        super(context);
    }

    public SlidingFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SlidingFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public float getXFraction() {
        return (getWidth() == 0) ? 0 : (getX() / (float) getWidth());
    }

    public void setXFraction(float xFraction) {
        setX((getWidth() > 0) ? (xFraction * getWidth()): 0);
    }
}
