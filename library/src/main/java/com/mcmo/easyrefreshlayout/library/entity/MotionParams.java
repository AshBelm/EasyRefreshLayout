package com.mcmo.easyrefreshlayout.library.entity;

import com.mcmo.easyrefreshlayout.library.RefreshViewHolder;

/**
 * Created by ZhangWei on 2017/7/13.
 */

public class MotionParams {
    /**
     * 可移动的最大距离
     */
    public int maxDistance;
    /**
     * 激活刷新需要移动的距离
     */
    public int activateDistance;
    /**
     * 回弹的最大距离
     */
    public int minInRefreshDistance;
    /**
     * 已经移动的距离(view已经显示的大小)
     */
    public int consumedDistance;
    /**
     * 是否处于正在刷新状态
     */
    public boolean isRefreshing;
    /**
     * 是否处于可以激活刷新状态
     */
    public boolean isReady;

    public static MotionParams create(RefreshViewHolder view,int consumedY){
        MotionParams params = new MotionParams();
        params.consumedDistance = consumedY;
        params.maxDistance = view.getMaxScrollDistance();
        params.activateDistance = view.getActivateDistance();
        params.minInRefreshDistance = view.getMinDistanceInRefreshing();
        params.isReady = view.isRefreshReady();
        params.isRefreshing = view.isRefreshing();
        return params;
    }

    @Override
    public String toString() {
        return "MotionParams{" +
                "maxDistance=" + maxDistance +
                ", activateDistance=" + activateDistance +
                ", minInRefreshDistance=" + minInRefreshDistance +
                ", consumedDistance=" + consumedDistance +
                ", isRefreshing=" + isRefreshing +
                ", isReady=" + isReady +
                '}';
    }
}
