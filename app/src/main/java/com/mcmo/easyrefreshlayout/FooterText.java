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
    TextView tv;
    @Override
    public View getView(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.footer,null);
        tv= (TextView) v.findViewById(R.id.textView2);
        return v;
    }

    @Override
    public void scroll(View view, MotionParams params) {

    }

    @Override
    public void onRefreshingStateChanged(boolean refreshing,boolean touch) {
        tv.setText("刷新"+refreshing);
    }


    @Override
    public void onInScreen() {
        Log.e("refresh", "onInScreen: " );
    }

    @Override
    public void onOutScreen() {
        Log.e("refresh", "onOutScreen: " );
    }

    @Override
    public void onReadyStateChanged(boolean isReady) {

    }

}
