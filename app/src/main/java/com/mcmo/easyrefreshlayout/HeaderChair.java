package com.mcmo.easyrefreshlayout;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.mcmo.easyrefreshlayout.library.IRefreshView;

/**
 * Created by ZhangWei on 2017/5/24.
 */

public class HeaderChair implements IRefreshView {
    private ChairImageView civ;
    private boolean isRefresh=false;
    @Override
    public View getView(Context context) {
        View v= LayoutInflater.from(context).inflate(R.layout.header_chair,null);
        civ= (ChairImageView) v.findViewById(R.id.civ);
        return v;
    }

    @Override
    public void scroll(int totalScrollY, int viewHeight, int viewScrollY, int springHeight, int springScrollY) {
        if(isRefresh)
            return;
        float percent=1.0f*springScrollY/springHeight;
        civ.setPercent(percent);
    }

    @Override
    public void onRefreshStart() {
        isRefresh=true;
        civ.setImageResource(R.drawable.chair_anim);
        AnimationDrawable ad= (AnimationDrawable) civ.getDrawable();
        civ.setPercent(0);
        ad.start();
    }

    @Override
    public void onRefreshEnd() {
        isRefresh=false;
        Drawable d=civ.getDrawable();
        if(d instanceof AnimationDrawable){
            ((AnimationDrawable) d).stop();
        }
        civ.setImageResource(R.drawable.chair_white);
    }

    @Override
    public void onConfirmRefresh() {

    }

    @Override
    public void onCancelRefresh() {

    }
}
