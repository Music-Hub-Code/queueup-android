package org.louiswilliams.queueupplayer.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.RelativeLayout;

import org.louiswilliams.queueupplayer.R;

public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

    private boolean checked;

    public CheckableRelativeLayout(Context context) {
        super(context);
    }

    public CheckableRelativeLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CheckableRelativeLayout(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
    }

    @TargetApi(21)
    public CheckableRelativeLayout(Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);
    }

    @Override
    public void setChecked(boolean checked) {
        if (checked) {
            setBackgroundColor(getResources().getColor(R.color.primary_dark));
        } else {
            setBackgroundColor(getResources().getColor(R.color.secondary_light));
        }
        this.checked = checked;
        refreshDrawableState();
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void toggle() {
        setChecked(!checked);
    }
}
