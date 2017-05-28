package com.mcmo.easyrefreshlayout.library;

import android.content.Context;
import android.view.View;

/**
 * Created by ZhangWei on 2017/5/24.
 */

public interface IRefreshView {
    public View getView(Context context);
    public void scroll(int totalScrollY,int viewHeight,int viewScrollY,int springHeight,int springScrollY);
    public void onRefreshStart();
    public void onRefreshEnd();
    public void onConfirmRefresh();
    public void onCancelRefresh();
}
