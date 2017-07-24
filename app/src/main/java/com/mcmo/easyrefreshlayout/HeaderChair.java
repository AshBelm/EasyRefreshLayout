package com.mcmo.easyrefreshlayout;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mcmo.easyrefreshlayout.library.entity.MotionParams;
import com.mcmo.easyrefreshlayout.library.impl.IRefreshView;

/**
 * Created by ZhangWei on 2017/5/24.
 */

public class HeaderChair implements IRefreshView {
    private static final String TAG = "HeaderRebabc";
    private ChairImageView civ;
    TextView tv;


    @Override
    public View getView(Context context) {
        View v= LayoutInflater.from(context).inflate(R.layout.header_chair,null);
        civ= (ChairImageView) v.findViewById(R.id.civ);
        tv= (TextView) v.findViewById(R.id.tv_head);
        return v;
    }

    @Override
    public void scroll(View view, MotionParams params) {
        Log.e(TAG, "scroll "+params.toString());
        if(params.isRefreshing) {
            return;
        }
        int spring = params.consumedDistance-params.activateDistance;
        if(spring<0) spring=0;
        float percent=1.0f*spring/(params.maxDistance-params.activateDistance);
        civ.setPercent(percent);
    }

    @Override
    public void onRefreshingStateChanged(boolean refreshing,boolean touch) {
        Log.e(TAG, "onRefreshingStateChanged refreshing = "+refreshing+" touch = "+touch);
        if(refreshing){
            civ.setImageResource(R.drawable.chair_anim);
            AnimationDrawable ad= (AnimationDrawable) civ.getDrawable();
            civ.setPercent(0);
            ad.start();
        }else{
            Drawable d=civ.getDrawable();
            if(d instanceof AnimationDrawable){
                ((AnimationDrawable) d).stop();
            }
            civ.setImageResource(R.drawable.chair_white);
        }
    }


    @Override
    public void onInScreen() {
        Log.e(TAG, "onInScreen header");
    }

    @Override
    public void onOutScreen() {
        Log.e(TAG, "onOutScreen header");
    }

    @Override
    public void onReadyStateChanged(boolean isReady) {
        Log.e(TAG, "onReadyStateChanged isReady = "+isReady);
        if(isReady){
            tv.setText("松开加载");

        }else{
            tv.setText("下拉刷新");
        }
    }

}
