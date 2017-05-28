package com.mcmo.easyrefreshlayout;

import android.content.Context;
import android.view.View;

import com.mcmo.easyrefreshlayout.library.IRefreshView;

/**
 * Created by ZhangWei on 2017/5/24.
 */

public class FooterText implements IRefreshView {
    @Override
    public View getView(Context context) {
        return null;
    }

    @Override
    public void scroll(int totalScrollY, int viewHeight, int viewScrollY, int springHeight, int springScrollY) {

    }

    @Override
    public void onRefreshStart() {

    }

    @Override
    public void onRefreshEnd() {

    }

    @Override
    public void onConfirmRefresh() {

    }

    @Override
    public void onCancelRefresh() {

    }
}
