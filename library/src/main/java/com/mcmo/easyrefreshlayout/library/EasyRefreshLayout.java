package com.mcmo.easyrefreshlayout.library;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;

/**
 * Created by ZhangWei on 2017/5/22.
 */

public class EasyRefreshLayout extends ViewGroup implements NestedScrollingParent {
    private static final String TAG = "EasyRefreshLayout";
    private NestedScrollingParentHelper mParentHelper;
    private View mTarget;//中间滚动的那个view一般是RecycleView
    private RefreshViewHolder mHeader, mFooter;
    private ValueAnimator mAnimator;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private int mHandMoveY = 0;//手指实际移动的距离
    private float mMoveFactor = 2.0f;//实际移动到停止时，移动距离相对于需要移动距离的倍数
    private EasyRefreshListener mListener;

    public EasyRefreshLayout(Context context) {
        this(context, null);
    }

    public EasyRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public EasyRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);

        mParentHelper = new NestedScrollingParentHelper(this);
        mHeader = new RefreshViewHolder(this);
        mFooter = new RefreshViewHolder(this);
//        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
        mHeader.springDock = SpringDock.BEFORE;
        mFooter.springDock = SpringDock.BEFORE;
        int headerId = -1, footerId = -1;
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EasyRefreshLayout);
            mHeader.isFloat = ta.getBoolean(R.styleable.EasyRefreshLayout_headOverlap, false);
            mFooter.isFloat = ta.getBoolean(R.styleable.EasyRefreshLayout_footOverlap, false);
            headerId = ta.getResourceId(R.styleable.EasyRefreshLayout_header, -1);
            footerId = ta.getResourceId(R.styleable.EasyRefreshLayout_footer, -1);
            mHeader.maxSpringDistance = ta.getDimensionPixelSize(R.styleable.EasyRefreshLayout_headSpring, 0);
            mFooter.maxSpringDistance = ta.getDimensionPixelSize(R.styleable.EasyRefreshLayout_footSpring, 0);
            mHeader.activateDistance = ta.getDimensionPixelSize(R.styleable.EasyRefreshLayout_headActivateDistance, 0);
            mFooter.activateDistance = ta.getDimensionPixelSize(R.styleable.EasyRefreshLayout_footActivateDistance, 0);
        }
        if (headerId != -1) {
            View headerView = LayoutInflater.from(context).inflate(headerId, null);
            addHeaderView(headerView);
        }
        if (footerId != -1) {
            View footerView = LayoutInflater.from(context).inflate(footerId, null);
            addFooterView(footerView);
        }
    }

    public void setEasyRefreshListener(EasyRefreshListener mListener) {
        this.mListener = mListener;
    }

    public void addHeaderView(IRefreshView view) {
        addHeaderView(view.getView(getContext()));
        mHeader.iRefresh = view;
    }

    public void addFooterView(IRefreshView view) {
        addFooterView(view.getView(getContext()));
        mFooter.iRefresh = view;
    }

    public void addHeaderView(View header) {
        mHeader.addView(getContext(), header);
        LayoutParams lp_main = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(mHeader.layout, lp_main);
    }

    public void addFooterView(View footer) {
        mFooter.addView(getContext(), footer);
        LayoutParams lp_main = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(mFooter.layout, lp_main);
    }

    private void ensureTarget() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view == mHeader.layout || view == mFooter.layout) {

            } else {
                mTarget = view;
            }
        }
    }

    public boolean hadHeader() {
        return mHeader != null;
    }

    public boolean hadFooter() {
        return mFooter != null;
    }

    public void dismissRefresh() {
        if (!hadHeader()) {
            return;
        }
        if (mHeader.iRefresh != null) {
            mHeader.iRefresh.onRefreshEnd();
        }
        mHeader.isRefreshing = false;
        int scrollY = getScrollY();
        if (scrollY != 0)
            startAnim(300, new RefreshAnimatorUpdateListener(), scrollY, 0);
    }

    public void dismissLoadMore() {
        if (!hadFooter()) {
            return;
        }
        if (mFooter.iRefresh != null) {
            mFooter.iRefresh.onRefreshEnd();
        }
        mFooter.isRefreshing = false;
        int scrollY = getScrollY();
        if (scrollY != 0)
            startAnim(300, new RefreshAnimatorUpdateListener(), scrollY, 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.e(TAG, "onLayout: ");
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final int left = getPaddingLeft();
        final int right = width - getPaddingRight();
        final int top = getPaddingTop();
        final int bottom = height - getPaddingBottom();
        final View target = mTarget;
        target.layout(left, top, right, bottom);
        if (hadHeader())
            mHeader.layout(left, -mHeader.height + top, right, top);
        if (hadFooter())
            mFooter.layout(left, bottom, right, bottom + mFooter.height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.e(TAG, "onMeasure: ");
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        if (hadHeader())
            mHeader.measure(MeasureSpec.makeMeasureSpec(
                    getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                    MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                    getMeasuredHeight(), MeasureSpec.UNSPECIFIED));
        if (hadFooter())
            mFooter.measure(MeasureSpec.makeMeasureSpec(
                    getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                    MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                    getMeasuredHeight(), MeasureSpec.UNSPECIFIED));
    }

    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    public boolean canChildScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                int lastVisiblePosition = absListView.getLastVisiblePosition();
                return absListView.getChildCount() > 0
                        && (lastVisiblePosition > 0 || absListView.getChildAt(lastVisiblePosition)
                        .getBottom() > absListView.getMeasuredHeight() - absListView.getPaddingBottom());
            } else {
                return ViewCompat.canScrollVertically(mTarget, 1);
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, 1);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        if (!mHeader.isEmpty() && !mHeader.isFloat && y < -mHeader.getScrollDistance()) {
            y = -mHeader.getScrollDistance();
        }
        if (!mFooter.isEmpty() && !mFooter.isFloat && y > mFooter.getScrollDistance()) {
            y = mFooter.getScrollDistance();
        }
        super.scrollTo(x, y);
    }

    //<editor-fold desc="NestedScroll Method">
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
//        return super.onStartNestedScroll(child, target, nestedScrollAxes);
//        return !mHeader.isRefreshing && !mFooter.isRefreshing && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        mParentHelper.onNestedScrollAccepted(child, target, axes);
        // TODO: 2017/5/23 stop animation
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {

        int totalScrollY = getScrollY();
//        Log.e(TAG, "onNestedPreScroll: " + dy + " " + totalScrollY );
        //dy<0 下拉
        if (dy < 0) {
            if (totalScrollY <= 0) {//idle或下拉状态
                boolean childCanScroll = canChildScrollUp();
                if (mHeader.isFloat) {

                } else {
                    if (totalScrollY > -mHeader.getScrollDistance() && !childCanScroll) {
                        int deltaY = -mHeader.getScrollDistance() - totalScrollY;
                        int scrollY = deltaY > dy ? deltaY : dy;
                        boolean inSpring = -(totalScrollY + scrollY) > mHeader.height;
                        int viewScrollY = inSpring ? mHeader.height : -(totalScrollY + scrollY);//refreshview移动的距离
                        int springScrollY = 0;//回弹滑动的距离
                        if (inSpring) {
                            springScrollY = totalScrollY + scrollY + mHeader.height;
                        } else {
                            springScrollY = 0;
                        }
                        if (!mHeader.isEmpty() && mHeader.springDock == SpringDock.BEFORE) {//header锁定在最上头的效果
                            mHeader.layout.setTranslationY(springScrollY);
                        }
                        scrollBy(0, scrollY);
                        consumed[1] = scrollY;
                        callbackIHeader(-totalScrollY, -viewScrollY, -springScrollY);
                    }
                }
            } else {//上拉状态中
                if (mFooter.isFloat) {

                } else {
                    if (totalScrollY > 0) {
                        int deltaY = totalScrollY - 0;
                        int scrollY = deltaY < dy ? deltaY : dy;
                        boolean inSpring = (totalScrollY + scrollY) > mFooter.height;
                        int viewScrollY = inSpring ? (totalScrollY + scrollY) - mFooter.height : totalScrollY;
                        int springScrollY = 0;
                        if (inSpring) {
                            springScrollY = totalScrollY + scrollY - mFooter.height;
                        } else {
                            springScrollY = 0;
                        }
                        if (!mFooter.isEmpty() && mFooter.springDock == SpringDock.AFTER) {
                            mFooter.layout.setTranslationY(springScrollY);
                        }
                        callbackIFooter(totalScrollY, viewScrollY, springScrollY);
                        scrollBy(0, scrollY);
                        consumed[1] = scrollY;
                    }
                }
            }
        } else {
            if (totalScrollY < 0) {//下拉状态
//                boolean childCanScroll = ViewCompat.canScrollVertically(target,-1);
                if (mHeader.isFloat) {

                } else {
                    if (totalScrollY < 0) {
                        int deltaY = 0 - totalScrollY;
                        int scrollY = deltaY < dy ? deltaY : dy;
                        boolean inSpring = -(totalScrollY + scrollY) > mHeader.height;
                        int viewScrollY = inSpring ? mHeader.height : -(totalScrollY + scrollY);//refreshview移动的距离
                        int springScrollY = 0;//回弹滑动的距离
                        if (inSpring) {
                            springScrollY = totalScrollY + scrollY + mHeader.height;
                        } else {
                            springScrollY = 0;
                        }
                        if (!mHeader.isEmpty() && mHeader.springDock == SpringDock.BEFORE) {
                            mHeader.layout.setTranslationY(springScrollY);
                        }
                        callbackIHeader(-totalScrollY, -viewScrollY, -springScrollY);
                        scrollBy(0, scrollY);
                        consumed[1] = scrollY;
                    }
                }
            } else {//idle或上拉状态中
                if (mFooter.isFloat) {

                } else {
                    boolean childCanScroll = canChildScrollDown();
                    if (totalScrollY < mFooter.getScrollDistance() && !childCanScroll) {
                        int deltaY = mFooter.getScrollDistance() - totalScrollY;
                        int scrollY = deltaY > dy ? dy : deltaY;
                        boolean inSpring = (totalScrollY + scrollY) > mFooter.height;
                        int viewScrollY = inSpring ? (totalScrollY + scrollY) - mFooter.height : totalScrollY;
                        int springScrollY = 0;
                        if (inSpring) {
                            springScrollY = totalScrollY + scrollY - mFooter.height;
                        } else {
                            springScrollY = 0;
                        }
                        if (!mFooter.isEmpty() && mFooter.springDock == SpringDock.AFTER) {
                            mFooter.layout.setTranslationY(springScrollY);
                        }
                        callbackIFooter(totalScrollY, viewScrollY, springScrollY);
                        scrollBy(0, scrollY);
                        consumed[1] = scrollY;
                    }
                }
            }
        }
    }

    private void callbackIFooter(int totalScrollY, int viewScrollY, int springScrollY) {
        if (mFooter.iRefresh != null) {
            boolean refresh = totalScrollY >= mFooter.activateDistance;
            if (mFooter.refreshReady != refresh) {
                mFooter.refreshReady = refresh;
                if (refresh)
                    mFooter.iRefresh.onConfirmRefresh();
                else
                    mFooter.iRefresh.onCancelRefresh();
            }
            mFooter.iRefresh.scroll(totalScrollY, mFooter.activateDistance, mFooter.height, viewScrollY, mFooter.maxSpringDistance, springScrollY);
        }
    }

    private void callbackIHeader(int totalScrollY, int viewScrollY, int springScrollY) {
        if (mHeader.iRefresh != null) {
            boolean refresh = totalScrollY >= mHeader.activateDistance;
            if (mHeader.refreshReady != refresh) {
                mHeader.refreshReady = refresh;
                if (refresh) {
                    mHeader.iRefresh.onConfirmRefresh();
                } else {
                    mHeader.iRefresh.onCancelRefresh();
                }
            }
            mHeader.iRefresh.scroll(totalScrollY, mHeader.activateDistance, mHeader.height, viewScrollY, mHeader.maxSpringDistance, springScrollY);
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        mParentHelper.onStopNestedScroll(target);
    }

    @Override
    public void onStopNestedScroll(View child) {
//        super.onStopNestedScroll(child);
        int scrollY = getScrollY();
        if (scrollY > 0) {//上拉
            if (scrollY >= mFooter.activateDistance) {
                int springScrollY = scrollY - mFooter.height;
                startAnim(200, new RefreshAnimatorUpdateListener(), springScrollY, 0);
                if (!mFooter.isRefreshing) {
                    mFooter.isRefreshing = true;
                    if (mFooter.iRefresh != null) {
                        mFooter.iRefresh.onRefreshStart();
                    }
                    if (mListener != null && !mFooter.isEmpty()) {
                        mListener.onLoadMore(this);
                    }
                }
            } else {
                startAnim(300, new RefreshAnimatorUpdateListener(), scrollY, 0);
            }
        } else if (scrollY < 0) {//下拉
            if (scrollY <= -mHeader.activateDistance) {
                int springScrollY = scrollY + mHeader.height;
                startAnim(200, new RefreshAnimatorUpdateListener(), springScrollY, 0);
                if (!mHeader.isRefreshing) {
                    mHeader.isRefreshing = true;
                    if (mHeader.iRefresh != null) {
                        mHeader.iRefresh.onRefreshStart();
                    }
                    if (mListener != null && !mHeader.isEmpty()) {
                        mListener.onRefresh(this);
                    }
                }
            } else {
                startAnim(300, new RefreshAnimatorUpdateListener(), scrollY, 0);
            }
        }
    }


    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
//        return super.onNestedPreFling(target, velocityX, velocityY);
        return false;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
//        return super.onNestedFling(target, velocityX, velocityY, consumed);
        return false;
    }

    @Override
    public int getNestedScrollAxes() {
        return mParentHelper.getNestedScrollAxes();
    }
    //</editor-fold>

    //<editor-fold desc="Animation">
    private void startAnim(long duration, ValueAnimator.AnimatorUpdateListener listener, int... values) {
        if (mAnimator == null)
            mAnimator = new ValueAnimator();
        mAnimator.cancel();
        mAnimator.setIntValues(values);
        mAnimator.setDuration(duration);
        mAnimator.removeAllUpdateListeners();
        mAnimator.addUpdateListener(listener);
        mAnimator.start();
    }

    private class RefreshAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private boolean isFirst = true;
        private int prevValues;
        private int[] com = new int[2];

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (isFirst) {
                prevValues = (int) animation.getAnimatedValue();
                isFirst = false;
            } else {
                int dv = (int) animation.getAnimatedValue() - prevValues;
                prevValues = (int) animation.getAnimatedValue();
                onNestedPreScroll(mTarget, 0, dv, com);
            }
        }
    }
    //</editor-fold>

}
