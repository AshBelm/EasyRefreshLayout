package com.mcmo.easyrefreshlayout.library;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.mcmo.easyrefreshlayout.library.entity.SpringDock;
import com.mcmo.easyrefreshlayout.library.impl.IRefreshView;

/**
 * Created by ZhangWei on 2017/5/24.
 */

public class RefreshViewHolder {
    private EasyRefreshLayout mRefreshLayout;
    protected IRefreshView iRefresh;
    protected FrameLayout layout;
    private boolean isFloat=false;//是否悬浮显示
    private boolean enable=true;//是否可用
    private boolean isRefreshViewVisible = true;//刷新的view是否可见
    private int mMaxDis, mActDis, mMinDisInRefreshing;//设置的距离值，主要不能改变
    private int maxDisplayDistance;//最大显示的高
    private int activateDistance;//激活刷新的距离
    private int refreshingMinDistance;//刷新时显示的最小距离
    private int springDistance;//可回弹距离
    protected int height = 0;//整个layout的高
    protected SpringDock springDock = SpringDock.BEFORE;//refresh在有回弹是显示的位置
    private boolean isRefreshing;
    private boolean isRefreshReady;
    private boolean isInScreen;//是否可以在view上看到,用来标识当前view的状态
    private boolean isNeedRefresh=true;//在下拉时是否需要刷新，如果在刷新状态下拉动在本次动作中就不再需要激活刷新了


    public RefreshViewHolder(EasyRefreshLayout refreshLayout) {
        this.mRefreshLayout = refreshLayout;
    }

    /**
     * 设置是否处于可以激活刷新的状态
     * @param ready
     * @return 状态是否有改变
     */
    protected boolean setRefreshReady(boolean ready){
        boolean isReady = false;
        if(enable&&isRefreshViewVisible&&ready){
            isReady = true;
        }else{
            isReady = false;
        }
        if(isReady == isRefreshReady){
            return false;
        }else{
            isRefreshReady = isReady;
            return true;
        }
    }
    public boolean isRefreshReady(){
        return isRefreshReady;
    }
    public boolean isRefreshing(){
        return isRefreshing;
    }

    public boolean isInScreen() {
        return isInScreen;
    }

    /**
     * 状态是否改变
     * @param inScreen
     * @return
     */
    public boolean setInScreen(boolean inScreen) {
        if(this.isInScreen == inScreen){
            return false;
        }else{
            this.isInScreen = inScreen;
            isNeedRefresh = !isRefreshing;
            return true;
        }
    }

    public boolean isNeedRefresh() {
        return isNeedRefresh;
    }

    /**
     * 设置refreshview是否悬浮在EasyRefreshLayout上
     *
     * @param isFloat
     */
    public void setFloated(boolean isFloat) {
        this.isFloat = isFloat;
    }

    public boolean isFloat() {
        return isFloat;
    }

    /**
     * 设置刷新view是否可见
     *
     * @param visible
     */
    private void setVisible(boolean visible) {
        this.isRefreshViewVisible = visible;
    }

    public boolean isVisible() {
        return isRefreshViewVisible;
    }

    /**
     * 设置是否可用
     *
     * @param enable
     */
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isEnable() {
        return enable;
    }

    public int getMaxDistanceSettings() {
        return mMaxDis;
    }

    public void setMaxDistance(int distance) {
        this.mMaxDis = distance;
        measureDistance();
    }
    public int getActivateDistanceSettings(){
        return mActDis;
    }
    public int getActivateDistance() {
        return activateDistance;
    }

    public void setActivateDistance(int distance) {
        this.mActDis = distance;
        measureDistance();
    }
    public int getMinDistanceInRefreshingSettings(){
        return mMinDisInRefreshing;
    }
    public int getMinDistanceInRefreshing() {
        return refreshingMinDistance;
    }

    /**
     * 活动当前最小显示距离
     * @return
     */
    public int getCurMinDistance(){
        if(enable&&!isFloat&&isRefreshViewVisible){
            return refreshingMinDistance;
        }else{
            return 0;
        }
    }

    public void setMinDistanceInRefreshing(int distance) {
        this.mMinDisInRefreshing = distance;
        measureDistance();
    }

    /**
     * 获取最大可滚动距离
     * @return 可滚动的最大像素值 px
     */
    public int getMaxScrollDistance() {
        return (enable&&!isFloat) ? isRefreshViewVisible ? maxDisplayDistance : springDistance : 0;
    }

    public boolean isEmpty() {
        return layout == null;
    }

    public void addView(Context context, View v) {
        if (!isEmpty()) {
            throw new IllegalArgumentException("RefreshView exist you can not set again");
        }
        layout = new FrameLayout(context);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setLayoutParams(lp);
        layout.addView(v);
        if (v instanceof IRefreshView) {
            iRefresh = (IRefreshView) v;
        }
    }

    public void layout(int l, int t, int r, int b) {
        if (!isEmpty()) {
            layout.layout(l, t, r, b);
        }
    }

    @Nullable
    public View getView() {
        return isEmpty()?null:layout.getChildAt(0);
    }

    public void measureDistance() {
        if (mActDis < 0) {
            Log.w("EasyRefreshLayout", "activate distance small than 0");
        }
        if (mActDis > mMaxDis) {
            Log.w("EasyRefreshLayout", "activate distance large than MaxDistance");
        }
        if (mMinDisInRefreshing < 0) {
            Log.w("EasyRefreshLayout", "min distance small than 0");
        }
        if (mMinDisInRefreshing > mMaxDis) {
            Log.w("EasyRefreshLayout", "min distance large than MaxDistance");
        }
        if (!isEmpty()) {
            height = layout.getMeasuredHeight();
        }
        int halfMaxHeight = (mRefreshLayout.getMeasuredHeight() + 1) / 2;
        if (mMaxDis <= 0) {
            maxDisplayDistance = height>halfMaxHeight? (int) (height * 1.5f) :halfMaxHeight;
        }
        if (mActDis <= 0) {
            activateDistance = height;
        } else if (mActDis > maxDisplayDistance) {
            activateDistance = maxDisplayDistance;
        }
        if (mMinDisInRefreshing <= 0) {
            refreshingMinDistance = height;
        } else if (mMinDisInRefreshing > maxDisplayDistance) {
            refreshingMinDistance = maxDisplayDistance;
        }
        springDistance = maxDisplayDistance-refreshingMinDistance;
    }


}
