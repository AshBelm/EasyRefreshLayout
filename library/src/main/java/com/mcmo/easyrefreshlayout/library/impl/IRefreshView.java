package com.mcmo.easyrefreshlayout.library.impl;

import android.content.Context;
import android.view.View;

import com.mcmo.easyrefreshlayout.library.entity.MotionParams;

/**
 * Created by ZhangWei on 2017/5/24.
 */

public interface IRefreshView {
    public View getView(Context context);
    public void scroll(View view, MotionParams params);

    /**
     * 刷新的状态改变
     * @param refreshing 是否正在刷新
     */
    public void onRefreshingStateChanged(boolean refreshing,boolean touch);

    /**
     * 刷新的view刚刚可见时调用
     */
    public void onInScreen();

    /**
     * 刷新的view刚刚看不到时
     */
    public void onOutScreen();

    /**
     * 可以激活刷新的状态改变时调用
     * @param isReady
     */
    public void onReadyStateChanged(boolean isReady);
}
