package com.mcmo.easyrefreshlayout;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mcmo.easyrefreshlayout.library.entity.MotionParams;
import com.mcmo.easyrefreshlayout.library.impl.IRefreshView;

/**
 * Created by ZhangWei on 2017/5/24.
 */

public class FooterText implements IRefreshView {
    private static final String TAG = "FooterRebabc";
    TextView tv;
    @Override
    public View getView(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.footer,null);
        tv= (TextView) v.findViewById(R.id.textView2);
        return v;
    }

    @Override
    public void scroll(View view, MotionParams params) {
        Log.e(TAG, "scroll "+params.toString());
    }

    @Override
    public void onRefreshingStateChanged(boolean refreshing,boolean touch) {
        tv.setText("刷新"+refreshing);
        Log.e(TAG, "onRefreshingStateChanged refreshing = "+refreshing+" touch = "+touch);
    }


    @Override
    public void onInScreen() {
        Log.e(TAG, "onInScreen: " );
    }

    @Override
    public void onOutScreen() {
        Log.e(TAG, "onOutScreen: " );
    }

    @Override
    public void onReadyStateChanged(boolean isReady) {
        Log.e(TAG, "onReadyStateChanged isReady = "+isReady);
    }

}
