package com.mcmo.easyrefreshlayout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mcmo.easyrefreshlayout.library.IRefreshView;

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
    public void scroll(int totalScrollY, int actY,int viewHeight, int viewScrollY, int springHeight, int springScrollY) {

    }

    @Override
    public void onRefreshStart() {

    }

    @Override
    public void onRefreshEnd() {

    }

    @Override
    public void onConfirmRefresh() {
        tv.setText("加载");
    }

    @Override
    public void onCancelRefresh() {
        tv.setText("拉" );
    }
}
