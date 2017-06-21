package com.mcmo.easyrefreshlayout.library;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by ZhangWei on 2017/5/24.
 */

public class RefreshViewHolder {
    private EasyRefreshLayout mRefreshLayout;
    protected FrameLayout layout;
    protected boolean isFloat;//是否悬浮显示
    protected int height=0;
    protected int maxSpringDistance=0;//最大可回弹距离
    protected int activateDistance=0;//激活刷新的距离 ，>=height && <=height+maxSpringDistance
    protected SpringDock springDock=SpringDock.BEFORE;//refresh在有回弹是显示的位置
    protected boolean isRefreshing;
    protected IRefreshView iRefresh;
    protected boolean refreshReady=false;

    public RefreshViewHolder(EasyRefreshLayout refreshLayout) {
        this.mRefreshLayout = refreshLayout;
    }

    public EasyRefreshLayout getRefreshLayout() {
        return mRefreshLayout;
    }

    public boolean isEmpty(){
        return layout==null;
    }
    public void addView(Context context,View v){
        if(!isEmpty()){
            throw new IllegalArgumentException("RefreshView exist you can set again");
        }
        layout = new FrameLayout(context);
        ViewGroup.LayoutParams lp=new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(lp);
        layout.addView(v);
        if(v instanceof IRefreshView){
            iRefresh = (IRefreshView) v;
        }
    }
    public void layout(int l,int t,int r,int b){
        if(!isEmpty()){
            layout.layout(l, t, r, b);
        }
    }
    public void measure(int widthMeasureSpec,int heightMeasureSpec){
        if(!isEmpty()){
            layout.measure(widthMeasureSpec, heightMeasureSpec);
            height = layout.getMeasuredHeight();
        }
        if(activateDistance<height){
            activateDistance = height;
        }
        if(activateDistance>(height+maxSpringDistance)){
            activateDistance = height+maxSpringDistance;
        }
    }
    public int getScrollDistance(){
        return height+maxSpringDistance;
    }

}
