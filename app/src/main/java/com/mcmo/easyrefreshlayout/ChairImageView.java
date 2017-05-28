package com.mcmo.easyrefreshlayout;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by ZhangWei on 2017/5/24.
 */

public class ChairImageView extends ImageView {
    private int color=0xff000000;
    private float percent;
    private Rect rect=new Rect();
    public ChairImageView(Context context) {
        super(context);
    }

    public ChairImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChairImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    protected void onDraw(Canvas canvas) {
        int height=getHeight();
        int width=getWidth();
        rect.right=width;
        rect.bottom= (int) (percent*height);
        canvas.save();
        canvas.clipRect(rect);
        canvas.drawColor(color);
        canvas.restore();
        super.onDraw(canvas);
    }

    public void setPercent(float percent) {
        this.percent = percent;
        postInvalidate();
    }
}
