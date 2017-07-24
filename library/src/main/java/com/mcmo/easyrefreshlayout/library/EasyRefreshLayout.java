package com.mcmo.easyrefreshlayout.library;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.annotation.Size;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;

import com.mcmo.easyrefreshlayout.library.entity.MotionParams;
import com.mcmo.easyrefreshlayout.library.entity.SpringDock;
import com.mcmo.easyrefreshlayout.library.impl.EasyRefreshListener;
import com.mcmo.easyrefreshlayout.library.impl.IRefreshView;

/**
 * Created by ZhangWei on 2017/5/22.
 */

public class EasyRefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {
    private static final String TAG = "EasyRefreshLayout";
    private NestedScrollingParentHelper mParentHelper;
    private NestedScrollingChildHelper mChildHelper;
    private View mTarget;//中间可以滚动的部分
    private RefreshViewHolder mHeader, mFooter;
    private int mScrollContentHeight;
    private DecelerateInterpolator mDecelerateInterpolator;
    private final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private int mHandMoveY = 0;//手指实际移动的距离
    private float mMoveFactor = 2.0f;//实际移动到停止时，移动距离相对于需要移动距离的倍数
    private EasyRefreshListener mListener;

    //scroll
    public static final int VERTICAL_OVERSCROLL_MAX = 300;
    private static final int INVALID_POINTER = -1;
    private int mTouchSlop;
    private float mMaxFlingVelocity, mMinFlingVelocity;
    private VelocityTracker mVelocityTracker;
    private ScrollerCompat mScroller;
    private int mActivatePointerId = INVALID_POINTER;
    private int mLastMotionY;
    private boolean mIsBeginDragged;

    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedYOffset;

    private static final int SCROLL_TYPE_NONE = -1;
    private static final int SCROLL_TYPE_SPRINGBACK = 1;
    private static final int SCROLL_TYPE_FLING = 2;
    private int mScrollType = SCROLL_TYPE_NONE;

    public EasyRefreshLayout(Context context) {
        this(context, null);
    }

    public EasyRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public EasyRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
        mHeader = new RefreshViewHolder(this);
        mFooter = new RefreshViewHolder(this);
        mHeader.springDock = SpringDock.AFTER;
        mFooter.springDock = SpringDock.AFTER;
        int headerId = -1, footerId = -1;
        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EasyRefreshLayout);
            mHeader.setFloated(ta.getBoolean(R.styleable.EasyRefreshLayout_headOverlap, false));
            mFooter.setFloated(ta.getBoolean(R.styleable.EasyRefreshLayout_footOverlap, false));
            headerId = ta.getResourceId(R.styleable.EasyRefreshLayout_header, -1);
            footerId = ta.getResourceId(R.styleable.EasyRefreshLayout_footer, -1);
            mHeader.setMaxDistance(ta.getDimensionPixelSize(R.styleable.EasyRefreshLayout_headMaxDistance, 0));
            mHeader.setMinDistanceInRefreshing(ta.getDimensionPixelSize(R.styleable.EasyRefreshLayout_headMinDistance, 0));
            mHeader.setActivateDistance(ta.getDimensionPixelSize(R.styleable.EasyRefreshLayout_headActivateDistance, 0));
            mFooter.setMaxDistance(ta.getDimensionPixelSize(R.styleable.EasyRefreshLayout_footMaxDistance, 0));
            mFooter.setMinDistanceInRefreshing(ta.getDimensionPixelSize(R.styleable.EasyRefreshLayout_footMinDistance, 0));
            mFooter.setActivateDistance(ta.getDimensionPixelSize(R.styleable.EasyRefreshLayout_footActivateDistance, 0));
            ta.recycle();
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

    private void init() {
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        mScroller = ScrollerCompat.create(getContext(), null);
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaxFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        mMinFlingVelocity = configuration.getScaledMinimumFlingVelocity();

        mParentHelper = new NestedScrollingParentHelper(this);
        mChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);
    }


    private void setRefreshViewVisibility(RefreshViewHolder viewHolder, boolean visibility) {
        if (viewHolder != null && !viewHolder.isEmpty()) {
            viewHolder.layout.setVisibility(visibility ? VISIBLE : INVISIBLE);
        }
    }

    public void setRefreshEnable(boolean enable) {
        mHeader.setEnable(enable);
    }

    public void setLoadMoreEnable(boolean enable) {
        mFooter.setEnable(enable);
    }

    public boolean getRefreshEnable() {
        return mHeader.isEnable();
    }

    public boolean getLoadMoreEnable() {
        return mFooter.isEnable();
    }

    public void setRefreshJustSpring(boolean justSpring){
        mHeader.setRefreshViewVisible(!justSpring);
    }
    public boolean getRefreshJustSpring(){
        return !mHeader.isRefreshViewVisible();
    }
    public void setLoadMoreJustSpring(boolean justSpring){
        mFooter.setRefreshViewVisible(!justSpring);
    }
    public boolean getLoadMoreJustSpring(){
        return !mFooter.isRefreshViewVisible();
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
        setRefreshViewVisibility(mHeader, mHeader.isEnable());
    }

    public void addFooterView(View footer) {
        mFooter.addView(getContext(), footer);
        LayoutParams lp_main = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(mFooter.layout, lp_main);
        setRefreshViewVisibility(mFooter, mFooter.isEnable());

    }

//    public boolean hadHeader() {
//        return mHeader != null;
//    }
//
//    public boolean hadFooter() {
//        return mFooter != null;
//    }

    public void dismissRefresh() {
        if(mHeader.setRefreshing(false)){
            if (mHeader.iRefresh != null) {
                mHeader.iRefresh.onRefreshingStateChanged(false, false);
            }
        }
        if(springBack(getScrollX(), getScrollY())){
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void dismissLoadMore() {
        if(mFooter.setRefreshing(false)){
            if(mFooter.iRefresh!=null){
                mFooter.iRefresh.onRefreshingStateChanged(false,false);
            }
        }
        if(springBack(getScrollX(),getScrollY())){
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void initVelocityTrackerIfNotExist() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
        }
        mVelocityTracker = null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_MOVE && mIsBeginDragged) {
            Log.e(TAG, "onInterceptTouchEvent trhe");
            return true;
        }
        int y = (int) ev.getY();
        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                logError("onInterceptTouchEvent", "down");
                if (!inContentChild((int) ev.getX(), y)) {
                    Log.e(TAG, "onInterceptTouchEvent out of child");
                    mIsBeginDragged = false;
                    recycleVelocityTracker();
                    break;
                }
                mLastMotionY = y;
                mActivatePointerId = ev.getPointerId(0);
                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);

                 /*
                 * If being flinged and user touches the this layout, initiate drag;
                 * otherwise don't.
                */
                mScroller.computeScrollOffset();
                mIsBeginDragged = !mScroller.isFinished();
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            case MotionEvent.ACTION_MOVE:
                logError("onInterceptTouchEvent", "move");
                final int activePointerId = mActivatePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on scroll content.
                    break;
                }

                final int activatePointerIndex = ev.findPointerIndex(activePointerId);
                if (activatePointerIndex == -1) {
                    Log.e(TAG, "inter Invalid pointerId=" + activePointerId + " in onTouchEvent");
                    break;
                }
                y = (int) ev.getY(activatePointerIndex);
                final int deltaY = Math.abs(mLastMotionY - y);
                /*
                   If deltaY large than touch slop and not have nested scroll initiate drag;
                 */
                if (deltaY > mTouchSlop && (getNestedScrollAxes() & ViewCompat.SCROLL_AXIS_VERTICAL) == 0) {
                    mIsBeginDragged = true;
                    mLastMotionY = y;
                    initVelocityTrackerIfNotExist();
                    mVelocityTracker.addMovement(ev);
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                logError("onInterceptTouchEvent", "up cancel");
                /* stop the drag */
                endDrag();
                confirmRefresh();
                if (springBack(getScrollX(), getScrollY())) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                stopNestedScroll();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                logError("onInterceptTouchEvent", "pointer up");
                break;
        }
        return mIsBeginDragged;
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExist();
        int motionY = (int) ev.getY();
        MotionEvent vtev = MotionEvent.obtain(ev);
        final int actionMask = MotionEventCompat.getActionMasked(ev);
        if (actionMask == MotionEvent.ACTION_DOWN) {
            mNestedYOffset = 0;
        }
        vtev.offsetLocation(0, mNestedYOffset);
        switch (actionMask) {
            case MotionEvent.ACTION_DOWN:
                logError("onTouchEvent", "down");
                if (mTarget == null) {
                    return false;
                }
                if (!inContentChild((int) ev.getX(), motionY)) {
                    Log.e(TAG, "onTouchEvent out of child");
                    mIsBeginDragged = false;
                    recycleVelocityTracker();
                    break;
                }
                /**
                 * If being flinged and touch on screen,initiate drag immediately
                 */
                if ((mIsBeginDragged = !mScroller.isFinished())) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (!mScroller.isFinished()) {
                    abortScroller();
                }
                mLastMotionY = motionY;
                mActivatePointerId = ev.getPointerId(0);
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            case MotionEvent.ACTION_MOVE:
                logError("onTouchEvent", "move");
                final int activePointerIndex = ev.findPointerIndex(mActivatePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "touch Invalid pointerId=" + mActivatePointerId + " in onTouchEvent");
                    break;
                }
                final int y = (int) ev.getY(activePointerIndex);
                int deltaY = mLastMotionY - y;
                Log.e(TAG, "onTouchEvent drag delta=" + deltaY + " slop=" + mTouchSlop);
                if (!mIsBeginDragged && Math.abs(deltaY) > mTouchSlop) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeginDragged = true;
                    /*Wipe off the dash of begin drag.The large touch slop ,the obvious */
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }
                if (mIsBeginDragged) {
                    mLastMotionY = y;
                    int oldY = getScrollY();
                    if (scrollBeforePreScroll(deltaY, mScrollConsumed)) {
                        deltaY -= mScrollConsumed[1];
                    }
                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                        deltaY -= mScrollConsumed[1];
                        vtev.offsetLocation(0, mScrollOffset[1]);
                        mNestedYOffset += mScrollOffset[1];
                        mLastMotionY -= mScrollOffset[1];
                    }
                    if (scrollInContentRange(deltaY, mScrollConsumed)) {
                        deltaY -= mScrollConsumed[1];
                    }
                    if (dispatchNestedScroll(0, oldY - getScrollY(), 0, deltaY, mScrollOffset)) {
                        deltaY += mScrollOffset[1];
                        Log.e(TAG, "onTouchEvent last " + mScrollOffset[1]);
                        mLastMotionY -= mScrollOffset[1];
                        Log.e(TAG, "onTouchEvent last after" + mLastMotionY);
                        vtev.offsetLocation(0, mScrollOffset[1]);
                        mNestedYOffset += mScrollOffset[1];
                    }
                    scrollBy(0, deltaY);
                    processRefresh();
                }
                break;
            case MotionEvent.ACTION_UP:
                logError("onTouchEvent", "up");
                if (mIsBeginDragged) {
                    mVelocityTracker.addMovement(vtev);
                    mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                    float velocity = VelocityTrackerCompat.getYVelocity(mVelocityTracker, mActivatePointerId);
                    if (Math.abs(velocity) > mMinFlingVelocity) {
                        flingWithNestedDispatch(0, (int) -velocity);
                    } else {
                        confirmRefresh();
                        if (springBack(getScrollX(), getScrollY())) {
                            ViewCompat.postInvalidateOnAnimation(this);
                        }
                    }
                }
                endDrag();
                break;
            case MotionEvent.ACTION_CANCEL:
                logError("onTouchEvent", "cancel");
                if (mIsBeginDragged) {
                    if (springBack(getScrollX(), getScrollY())) {
                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivatePointerId));
                break;
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return true;
    }

    private void endDrag() {
        mIsBeginDragged = false;
        recycleVelocityTracker();
        stopNestedScroll();
        mActivatePointerId = INVALID_POINTER;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getActionIndex() & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >> MotionEventCompat.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivatePointerId) {
            final int newPointerId = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = (int) ev.getY(newPointerId);
            mActivatePointerId = newPointerId;
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private void processRefresh() {
        // TODO: 2017/7/13 考虑当正在刷新时再次拉出是否需要激发刷新事件
        processRefreshHeader();
        processRefreshFooter();
    }

    private void confirmRefresh() {
        if (!mHeader.isEmpty() && mHeader.isInScreen() && mHeader.canRefresh()) {
            if (mHeader.setRefreshing(true)) {
                if (mHeader.iRefresh != null) {
                    mHeader.iRefresh.onRefreshingStateChanged(true, false);
                }
                if (mListener != null) {
                    mListener.onRefresh(this);
                }
            }
        }
        if (!mFooter.isEmpty() && mFooter.isInScreen() && mFooter.canRefresh()) {
            if (mFooter.setRefreshing(true)) {
                if (mFooter.iRefresh != null) {
                    mFooter.iRefresh.onRefreshingStateChanged(true, false);
                }
                if (mListener != null) {
                    mListener.onLoadMore(this);
                }
            }
        }
    }

    private void processRefreshHeader() {
        int scrollY = getScrollY();
        boolean inScreen = isHeaderInScreen();
        boolean inScreenChanged = mHeader.setInScreen(inScreen);
        if (mHeader.iRefresh != null && inScreenChanged) {
            if (mHeader.isInScreen())
                mHeader.iRefresh.onInScreen();
            else
                mHeader.iRefresh.onOutScreen();
        }
        if (inScreen) {
            int consumedY = -scrollY;
            boolean readyChanged = mHeader.setRefreshReady(scrollY <= -mHeader.getActivateDistance());
            if (!mHeader.isEmpty() && mHeader.springDock == SpringDock.AFTER) {
                int offsetY = consumedY - mHeader.height;
                if (offsetY > 0) {
                    mHeader.layout.setTranslationY(-offsetY);
                } else {
                    mHeader.layout.setTranslationY(0);
                }
            }
            if (mHeader.iRefresh != null) {
                MotionParams p = MotionParams.create(mHeader, consumedY);
                if (readyChanged) {
                    mHeader.iRefresh.onReadyStateChanged(mHeader.isRefreshReady());
                }
                mHeader.iRefresh.scroll(mHeader.getView(), p);
            }
        }
    }

    private void processRefreshFooter() {
        int scrollY = getScrollY();
        boolean inScreen = isFooterInScreen();
        boolean inScreenChanged = mFooter.setInScreen(inScreen);
        if (mFooter.iRefresh != null && inScreenChanged) {
            if (mFooter.isInScreen())
                mFooter.iRefresh.onInScreen();
            else
                mFooter.iRefresh.onOutScreen();
        }
        if (inScreen) {
            int toBottomScrollY = getScrollContentRange();
            int consumedY = scrollY - toBottomScrollY;
            boolean readyChanged = mFooter.setRefreshReady(consumedY >= mFooter.getActivateDistance());
            // TODO: 2017/7/14 加强个种设置情况下的判断 enable visiavle
            if (!mFooter.isEmpty() && mFooter.springDock == SpringDock.AFTER) {
                int offsetY = consumedY - mFooter.height;
                Log.e(TAG, "processRefreshFooter " + offsetY);
                if (offsetY > 0) {
                    mFooter.layout.setTranslationY(offsetY);
                } else {
                    mFooter.layout.setTranslationY(0);
                }
            }
            if (mFooter.iRefresh != null) {
                MotionParams p = MotionParams.create(mFooter, consumedY);
                if (readyChanged) {
                    mFooter.iRefresh.onReadyStateChanged(mFooter.isRefreshReady());
                }
                mFooter.iRefresh.scroll(mFooter.getView(), p);
            }
        }
    }


    @Override
    public void scrollTo(@Px int x, @Px int y) {
        // TODO: 2017/7/14 考虑悬浮情况 
        if (mTarget == null) {
            super.scrollTo(x, y);
        } else {
            int maxHeader = mHeader.getMaxScrollDistance();
            int maxFooter = mFooter.getMaxScrollDistance();
            if (y < -maxHeader) {
                y = -maxHeader;
            } else if (y > (mScrollContentHeight - getMeasuredHeight()) + maxFooter) {
                y = mScrollContentHeight - getMeasuredHeight() + maxFooter;
            }
            super.scrollTo(x, y);
        }
    }

    private boolean springBack(int startX, int startY) {
        // TODO: 2017/7/14 考虑悬浮的情况
        int minY = 0, maxY = 0;
        if (mHeader.isInScreen() && (mHeader.isRefreshing()||mHeader.canRefresh())) {
            minY = -mHeader.getMinDistanceInRefreshing();
        } else {
            minY = 0;
        }
        if (mFooter.isInScreen() && (mFooter.isRefreshing()||mFooter.canRefresh())) {
            maxY = getScrollContentRange() + mFooter.getMinDistanceInRefreshing();
        } else {
            maxY = getScrollContentRange();
        }
        mScrollType = SCROLL_TYPE_SPRINGBACK;
        return mScroller.springBack(startX, startY, 0, 0, minY, maxY);
    }

    private void fling(int velocityY) {
        // TODO: 2017/7/14 考虑悬浮
//        int minY = (mHeader.isRefreshing() || mHeader.isRefreshReady()) ? -mHeader.getMinDistanceInRefreshing() : 0;
        int minY = -mHeader.getFlingWithContentDistance();
//        int maxY = (mFooter.isRefreshing() || mFooter.isRefreshReady()) ? getScrollContentRange() + mFooter.getMinDistanceInRefreshing() : getScrollContentRange();
        int maxY = getScrollContentRange() + mFooter.getFlingWithContentDistance();
        Log.e(TAG, "fling max=" + maxY);
        mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, minY, maxY, 0, maxY / 2);
        mScrollType = SCROLL_TYPE_FLING;
        ViewCompat.postInvalidateOnAnimation(this);
    }
    private boolean flingOrSpringBack(int velocityY){
        boolean needFling = getScrollY()>=-mHeader.getFlingWithContentDistance()&&getScrollY()<=getScrollContentRange()+mFooter.getFlingWithContentDistance();
        boolean needSpringBack = false;
        if(needFling){
            fling(velocityY);
        }else{
            confirmRefresh();
            if(needSpringBack=springBack(getScrollX(),getScrollY())){
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
        return needSpringBack||needFling;
    }
    private void flingWithNestedDispatch(int velocityX, int velocityY){
        if(!dispatchNestedPreFling(velocityX,velocityY)){
            boolean canFling = flingOrSpringBack(velocityY);
            dispatchNestedFling(velocityX,velocityY,canFling);
        }
    }
    private void abortScroller() {
        Log.e(TAG, "abortAnimation ");
        mScroller.abortAnimation();
        mScrollType = SCROLL_TYPE_NONE;
    }

    private boolean isHeaderInScreen() {
        if (mHeader.isFloat()) {
            return false;
        } else {
            return getScrollY() < 0;
        }
    }

    private boolean isFooterInScreen() {
        if (mFooter.isFloat()) {
            return false;
        } else {
            return getScrollY() > getScrollContentRange();
        }
    }

    /**
     * 获取可以滚动的内容布局高度
     *
     * @return
     */
    private void calcScrollContentHeight() {
        int height = getMeasuredHeight() - getPaddingBottom() - getPaddingTop();
        int targetHeight = 0;
        if (mTarget != null) {
            targetHeight = mTarget.getMeasuredHeight();
        }
        mScrollContentHeight = Math.max(height, targetHeight);
    }

    private int getScrollContentHeight() {
        return mScrollContentHeight;
    }

    private boolean inContentChild(int x, int y) {
        if (mTarget != null) {
            final int scrollY = getScrollY();
            return !(y < mTarget.getTop() - scrollY
                    || y >= getScrollContentHeight() - scrollY
                    || x < mTarget.getLeft()
                    || x >= mTarget.getRight());
        }
        return false;
    }

    private View ensureTarget() {
        int count = getChildCount();
        View target = null;
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view == mHeader.layout || view == mFooter.layout) {
                // TODO: 2017/7/5  noting
            } else {
                if (target == null) {
                    target = view;
                } else {
                    throw new IllegalStateException("EasyRefreshLayout can host only one direct child");
                }
            }
        }
        return target;
    }

    @SuppressWarnings("WrongConstant")
    private int getTargetMeasureSpec(int spec, int padding, int childDimension) {
        int mode = MeasureSpec.getMode(spec);
        int size = MeasureSpec.getSize(spec);
        int maxSize = Math.max(0, size - padding);
        int resultMode = 0;
        int resultSize = 0;
        if (childDimension >= 0) {
            resultSize = childDimension;
            resultMode = MeasureSpec.EXACTLY;
        } else if (childDimension == LayoutParams.MATCH_PARENT) {
            resultSize = maxSize;
            resultMode = MeasureSpec.AT_MOST;
        } else if (childDimension == LayoutParams.WRAP_CONTENT) {
            resultMode = maxSize;
            resultMode = MeasureSpec.UNSPECIFIED;
        }
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        Log.e(TAG, "onScrollChanged " + l + " " + t + " " + oldl + " " + oldt);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Log.e(TAG, "onMeasure: ");
        if (mTarget == null) {
            mTarget = ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final ViewGroup.LayoutParams lp = (LayoutParams) mTarget.getLayoutParams();
        int targetWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(), lp.width);
        int targetHeightMeasureSpec = getTargetMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(), lp.height);
        mTarget.measure(targetWidthMeasureSpec, targetHeightMeasureSpec);

        if (!mHeader.isEmpty()) {
            ViewGroup.LayoutParams headerLayoutParams = mHeader.layout.getLayoutParams();
            int headerWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(), headerLayoutParams.width);
            int headerHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingBottom() - getPaddingTop(), MeasureSpec.UNSPECIFIED);
            mHeader.layout.measure(headerWidthMeasureSpec, headerHeightMeasureSpec);
        }
        mHeader.measureDistance();
        if (!mFooter.isEmpty()) {
            ViewGroup.LayoutParams footerLayoutParams = mFooter.layout.getLayoutParams();
            int footerWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(), footerLayoutParams.width);
            int footerHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingBottom() - getPaddingTop(), MeasureSpec.UNSPECIFIED);
            mFooter.layout.measure(footerWidthMeasureSpec, footerHeightMeasureSpec);
        }
        mFooter.measureDistance();
        calcScrollContentHeight();
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
            mTarget = ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final int left = getPaddingLeft();
        final int right = width - getPaddingRight();
        final int top = getPaddingTop();
        final int bottom = height - getPaddingBottom();
        final int targetHeight = mTarget.getMeasuredHeight();
        mTarget.layout(left, top, right, top + targetHeight);
        mHeader.layout(left, -mHeader.height + top, right, top);
        //计算底部refreshView开始的位置，保证在EasyRefreshLayout的下面或content的下面
        int footTop = top + getScrollContentHeight();
        mFooter.layout(left, footTop, right, footTop + mFooter.height);
    }

    public boolean canChildScrollUp(View view) {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(view, -1) || view.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(view, -1);
        }
    }

    public boolean canChildScrollDown(View view) {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;
                int lastVisiblePosition = absListView.getLastVisiblePosition();
                return absListView.getChildCount() > 0
                        && (lastVisiblePosition > 0 || absListView.getChildAt(lastVisiblePosition)
                        .getBottom() > absListView.getMeasuredHeight() - absListView.getPaddingBottom());
            } else {
                return ViewCompat.canScrollVertically(view, 1);
            }
        } else {
            return ViewCompat.canScrollVertically(view, 1);
        }
    }

    /**
     * 正好滚动到content底部的ScrollY
     *
     * @return
     */
    private int getScrollContentRange() {
        return getScrollContentHeight() - (getMeasuredHeight() - getPaddingTop() - getPaddingBottom());
    }

    @Override
    protected int computeVerticalScrollRange() {
        int height = getMeasuredHeight() - getPaddingBottom() - getPaddingTop();
        if (mTarget == null) {
            return height;
        }
        return getScrollContentHeight();
    }

    protected boolean overScrollByCompat(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollLeft, int maxOverScrollRight, int maxOverScrollTop, int maxOverScrollBottom, boolean isTouchEvent) {
        final int overScrollMode = getOverScrollMode();
        final boolean canScrollHorizontal = computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical = computeVerticalScrollRange() > computeVerticalScrollExtent();
        final boolean overScrollHorizontal = overScrollMode == View.OVER_SCROLL_ALWAYS || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == View.OVER_SCROLL_ALWAYS || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);
        int newScrolledX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollLeft = maxOverScrollRight = 0;
        }
        int newScrolledY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollTop = maxOverScrollBottom = 0;
        }
        final int left = -maxOverScrollLeft;
        final int right = scrollRangeX + maxOverScrollRight;
        final int top = -maxOverScrollTop;
        final int bottom = scrollRangeY + maxOverScrollBottom;
        boolean clampedX = false;
        if (newScrolledX > right) {
            newScrolledX = right;
            clampedX = true;
        } else if (newScrolledX < left) {
            newScrolledX = left;
            clampedX = true;
        }
        boolean clampedY = false;
        if (newScrolledY > bottom) {
            newScrolledY = bottom;
            clampedY = true;
        } else if (newScrolledY < top) {
            newScrolledY = top;
            clampedY = true;
        }
        Log.e(TAG, "overScrollByCompat " + clampedY);
        if (clampedY) {
            springBack(newScrolledX, newScrolledY);
        }
        onOverScrolled(newScrolledX, newScrolledY, clampedX, clampedY);
        return clampedX || clampedY;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.scrollTo(scrollX, scrollY);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            Log.e(TAG, "computeScroll " + oldY + " " + y);
            if (oldX != x || oldY != y) {
                if (mScrollType == SCROLL_TYPE_FLING) {
                    int maxTop = mHeader.getFlingWithContentDistance();
                    int maxBottom = mFooter.getFlingWithContentDistance();
                    overScrollByCompat(x - oldX, y - oldY, oldX, oldY, 0, getScrollContentRange(), 0, 0, maxTop, maxBottom, false);
                } else {
                    super.scrollTo(x, y);
                }
                processRefresh();
            }else{
                /*由于scroller的计算可能引起(oldX=x&&oldY=y)的情况发生，这样会导致动画未完成或无法开始，所以此处加上invalidate.
                * 并且这样也解决了滚动不能到头有时留有一两个像素的问题。（不知所以?）*/
                invalidate();
            }
        }
    }

    private boolean scrollBeforePreScroll(int dyUnconsumed, int[] consumed) {
        resetConsumedArray(consumed);
        if (dyUnconsumed == 0) {
            return false;
        }
        int oldY = getScrollY();
        if (dyUnconsumed > 0) {//pull up
            int minTop = -mHeader.getScrollWithContentDistance();
            if (oldY < minTop) {
                consumed[1] = Math.min(dyUnconsumed, minTop - oldY);
            }
        } else {//pull down
            int maxBottom = getScrollContentRange() + mFooter.getScrollWithContentDistance();
            if (oldY > maxBottom) {
                consumed[1] = Math.max(dyUnconsumed, maxBottom - oldY);
            }
        }
        if (consumed[1] != 0) {
            scrollBy(0, consumed[1]);
        }
        return consumed[1] != 0;
    }

    private boolean scrollInContentRange(int dyUnconsumed, int[] consumed) {
        resetConsumedArray(consumed);
        if (dyUnconsumed == 0) {
            return false;
        }
        int newY = getScrollY() + dyUnconsumed;
        int min = -mHeader.getScrollWithContentDistance();
        int max = getScrollContentRange() + mFooter.getScrollWithContentDistance();
        consumed[1] = dyUnconsumed;
        if (newY < min) {
            consumed[1] = dyUnconsumed - (newY - min);
        } else if (newY > max) {
            consumed[1] = dyUnconsumed - (newY - max);
        }
        scrollBy(0, consumed[1]);
        return consumed[1] != 0;
    }

    private void resetConsumedArray(int[] consumed) {
        consumed[0] = 0;
        consumed[1] = 0;

    }

    //<editor-fold desc="NestedScrollParent Method">

    @Override
    public int getNestedScrollAxes() {
        return mParentHelper.getNestedScrollAxes();
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        mParentHelper.onNestedScrollAccepted(child, target, axes);
        startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        Log.e(TAG, "onNestedPreScroll " + dy);
        dispatchNestedPreScroll(dx, dy, consumed, null);
        dy -= consumed[1];
        if (dy > 0) {//上拉
            if (isHeaderInScreen()) {
                int absScrollY = Math.abs(getScrollY());
                int consumedY = Math.min(absScrollY, dy);
                scrollBy(0, consumedY);
                consumed[1] = consumedY;
                processRefreshHeader();
            }
        } else if (dy < 0) {//下拉
            if (isFooterInScreen()) {
                int scrollRange = getScrollContentRange();
                int dScrollY = scrollRange - getScrollY();
                int consumedY = Math.max(dScrollY, dy);
                scrollBy(0, consumedY);
                consumed[1] = consumedY;
                processRefreshFooter();
            }
        }

    }


    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
//        scrollWithNestedDispatch(dxUnconsumed, dyUnconsumed,mScrollConsumed,mScrollOffset);
//        processRefresh();
        final int oldY = getScrollY();
        Log.e(TAG, "onNestedScroll " + dyUnconsumed + " " + oldY);
//        int myConsumed = 0;
//        int myUnconsumed = dyUnconsumed;
//        if (dyUnconsumed < 0 && oldY > 0) {//pull down
//            myConsumed = Math.max(dyUnconsumed, -oldY);
//            myUnconsumed = dyUnconsumed - myConsumed;
//            scrollBy(0, myConsumed);
//        }
//        if (dyUnconsumed > 0 && oldY < getScrollContentRange()) {//pull up
//            int scrollRange = getScrollContentRange();
//            int dScrollY = scrollRange - oldY;
//            myConsumed = Math.min(dyUnconsumed, dScrollY);
//            myUnconsumed = dyUnconsumed - myConsumed;
//            scrollBy(0, myConsumed);
//        }
        if (scrollInContentRange(dyUnconsumed, mScrollConsumed)) {
            dyUnconsumed -= mScrollConsumed[1];
        }
        if (dispatchNestedScroll(dxConsumed, mScrollConsumed[1], dxUnconsumed, dyUnconsumed, mScrollOffset)) {
            dyUnconsumed += mScrollOffset[1];
        }
        if (dyUnconsumed != 0) {
            scrollBy(0, dyUnconsumed);
        }
        processRefresh();
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        Log.e(TAG, "onNestedPreFling");
        return dispatchNestedPreFling(velocityX,velocityY);
//        return flingWithNestedDispatch((int)velocityX,(int)velocityX);
//        return super.onNestedPreFling(target, velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        Log.e(TAG, "onNestedFling " + consumed);
        if (!consumed) {
            flingOrSpringBack((int) velocityY);
            return true;
        }
        return false;
    }

    @Override
    public void onStopNestedScroll(View child) {
        Log.e(TAG, "onStopNestedScroll");
        mParentHelper.onStopNestedScroll(child);
        stopNestedScroll();
        mScroller.computeScrollOffset();
        confirmRefresh();
        if (mScroller.isFinished()) {
            Log.e(TAG, "onStopNestedScroll start");
            springBack(getScrollX(), getScrollY());
        }
    }
    //</editor-fold>


    //<editor-fold desc="NestedChild Method">
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable @Size(value = 2) int[] consumed, @Nullable @Size(value = 2) int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable @Size(value = 2) int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }
    //</editor-fold>

    //<editor-fold desc="Log">
    private static final boolean LOG_MOVE = true;

    private void logError(String methodName, String errorMsg) {
        if (LOG_MOVE)
            Log.e("EasyRefreshLayout", methodName + " : " + errorMsg);
    }
    //</editor-fold>
}
