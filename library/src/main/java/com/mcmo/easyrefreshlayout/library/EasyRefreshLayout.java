package com.mcmo.easyrefreshlayout.library;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Px;
import android.support.v4.view.MotionEventCompat;
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

public class EasyRefreshLayout extends ViewGroup implements NestedScrollingParent {
    private static final String TAG = "EasyRefreshLayout";
    private NestedScrollingParentHelper mParentHelper;
    private View mTarget;//中间可以滚动的部分
    private RefreshViewHolder mHeader, mFooter;
    private int mScrollContentHeight;
    private ValueAnimator mAnimator;
    private DecelerateInterpolator mDecelerateInterpolator;
    private final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private int mHandMoveY = 0;//手指实际移动的距离
    private float mMoveFactor = 2.0f;//实际移动到停止时，移动距离相对于需要移动距离的倍数
    private EasyRefreshListener mListener;
    private boolean mRefreshAble = true, mLoadMoreAble = true;

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
//    private static final int SCROLL_TYPE_NONE = -1;
//    private static final int SCROLL_TYPE_SPRINGBACK=1;
//    private static final int SCROLL_TYPE_FLING = 2;
//    private int mScrollType = SCROLL_TYPE_NONE;

    public EasyRefreshLayout(Context context) {
        this(context, null);
    }

    public EasyRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public EasyRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();

        mParentHelper = new NestedScrollingParentHelper(this);
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
    }


    private void setRefreshViewVisibility(RefreshViewHolder viewHolder, boolean visibility) {
        if (viewHolder != null && !viewHolder.isEmpty()) {
            viewHolder.layout.setVisibility(visibility ? VISIBLE : INVISIBLE);
        }
    }

    public void setRefreshEnable(boolean enable) {
        mRefreshAble = enable;
        setRefreshViewVisibility(mHeader, enable);
    }

    public void setLoadMoreEnable(boolean enable) {
        mLoadMoreAble = enable;
        setRefreshViewVisibility(mFooter, enable);
    }

    public boolean getRefreshEnable() {
        return mRefreshAble;
    }

    public boolean getLoadMoreEnable() {
        return mLoadMoreAble;
    }

    public void setEasyRefreshListener(EasyRefreshListener mListener) {
        this.mListener = mListener;
    }

    public void addHeaderView(IRefreshView view) {
        addHeaderView(view.getView(getContext()));
        mHeader.iRefresh = view;
        setRefreshViewVisibility(mHeader, mRefreshAble);
    }

    public void addFooterView(IRefreshView view) {
        addFooterView(view.getView(getContext()));
        mFooter.iRefresh = view;
        setRefreshViewVisibility(mFooter, mLoadMoreAble);
    }

    public void addHeaderView(View header) {
        mHeader.addView(getContext(), header);
        LayoutParams lp_main = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(mHeader.layout, lp_main);
        setRefreshViewVisibility(mHeader, mRefreshAble);
    }

    public void addFooterView(View footer) {
        mFooter.addView(getContext(), footer);
        LayoutParams lp_main = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(mFooter.layout, lp_main);
        setRefreshViewVisibility(mFooter, mLoadMoreAble);

    }

//    public boolean hadHeader() {
//        return mHeader != null;
//    }
//
//    public boolean hadFooter() {
//        return mFooter != null;
//    }

    public void dismissRefresh() {
//        if (!hadHeader()) {
//            return;
//        }
//        if (mHeader.iRefresh != null) {
//            mHeader.iRefresh.onOutScreen();
//        }
//        mHeader.isRefreshing = false;
//        int scrollY = getScrollY();
//        if (scrollY != 0)
//            startAnim(300, new RefreshAnimatorUpdateListener(), scrollY, 0);
    }

    public void dismissLoadMore() {
//        if (!hadFooter()) {
//            return;
//        }
//        if (mFooter.iRefresh != null) {
//            mFooter.iRefresh.onOutScreen();
//        }
//        mFooter.isRefreshing = false;
//        int scrollY = getScrollY();
//        if (scrollY != 0)
//            startAnim(300, new RefreshAnimatorUpdateListener(), scrollY, 0);
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
                // TODO: 2017/7/12 nested child
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
                if (springBack(getScrollX(), getScrollY())) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                // TODO: 2017/7/13 nested child
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
        int action = ev.getAction();
        MotionEvent vtev = MotionEvent.obtain(ev);
        switch (action & MotionEvent.ACTION_MASK) {
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
                    mScroller.abortAnimation();
                }
                mLastMotionY = motionY;
                mActivatePointerId = ev.getPointerId(0);
                // TODO: 2017/7/13 nested child
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
                // TODO: 2017/7/13 nested child 重新构造一个MotionEvent 
//                int distanceY = (int) (ev.getY() - mLastMotionY);
//                distanceY = (int) (Math.exp(-ev.getY() / mLastMotionY / 40) * distanceY);
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
                    scrollBy(0, deltaY);
                    processRefresh();
                    mLastMotionY = y;
                }
                break;
            case MotionEvent.ACTION_UP:
                logError("onTouchEvent", "up");
                if (mIsBeginDragged) {
                    mVelocityTracker.addMovement(vtev);
                    mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                    float velocity = VelocityTrackerCompat.getYVelocity(mVelocityTracker, mActivatePointerId);
                    if (Math.abs(velocity) > mMinFlingVelocity) {
                        fling((int) -velocity);
                    } else {
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
        return true;
    }

    private void endDrag() {
        mIsBeginDragged = false;
        recycleVelocityTracker();
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
            int toBottomScrollY = getScrollRange();
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
                MotionParams p = MotionParams.create(mHeader, consumedY);
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
        if (mHeader.isInScreen() && mHeader.isRefreshReady()) {
            minY = -mHeader.getMinDistanceInRefreshing();
        } else {
            minY = 0;
        }
        if (mFooter.isInScreen() && mFooter.isRefreshReady()) {
            maxY = getScrollRange() + mFooter.getMinDistanceInRefreshing();
        } else {
            maxY = getScrollRange();
        }
        return mScroller.springBack(startX, startY, 0, 0, minY, maxY);
    }

    private void fling(int velocity) {
        // TODO: 2017/7/14 考虑悬浮
        int minY = (mHeader.isRefreshing() || mHeader.isRefreshReady()) ? -mHeader.getMinDistanceInRefreshing() : 0;
        int maxY = (mFooter.isRefreshing() || mFooter.isRefreshReady()) ? getScrollRange() + mFooter.getMinDistanceInRefreshing(): getScrollRange();
        Log.e(TAG, "fling max=" + maxY);
        mScroller.fling(getScrollX(), getScrollY(), 0, velocity, 0, 0, minY, maxY, 0, VERTICAL_OVERSCROLL_MAX);
        ViewCompat.postInvalidateOnAnimation(this);
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
            return getScrollY() > getScrollRange();
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
    private int getScrollRange() {
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

    protected boolean overScrollByCompat(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX, int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {
        final int overScrollMode = getOverScrollMode();
        final boolean canScrollHorizontal = computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical = computeVerticalScrollRange() > computeVerticalScrollExtent();
        final boolean overScrollHorizontal = overScrollMode == View.OVER_SCROLL_ALWAYS || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == View.OVER_SCROLL_ALWAYS || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);
        int newScrolledX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }
        int newScrolledY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }
        final int left = -maxOverScrollX;
        final int right = scrollRangeX + maxOverScrollX;
        final int top = -maxOverScrollY;
        final int bottom = scrollRangeY + maxOverScrollY;
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
        Log.e(TAG, "overScrollByCompat "+clampedY);
        if(clampedY){
            springBack(newScrolledX,newScrolledY);
        }
        onOverScrolled(newScrolledX,newScrolledY,clampedX,clampedY);
        return clampedX||clampedY;
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.scrollTo(scrollX,scrollY);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int oldY = getScrollY();
            int oldX = getScrollX();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            Log.e(TAG, "computeScroll " + oldY + " " + y);
            if (oldX != x || oldY != y) {
//                super.scrollTo(x, y);
                overScrollByCompat(x-oldX,y-oldY,oldX,oldY,0,getScrollRange(),0,VERTICAL_OVERSCROLL_MAX,false);
                processRefresh();
            }
        }
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
        // TODO: 2017/5/23 stop animation
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        Log.e(TAG, "onNestedPreScroll " + dy);
        if (dy > 0) {//上拉
            Log.e(TAG, "onNestedPreScroll sc " + getScrollY());
            if (isHeaderInScreen()) {
                int absScrollY = Math.abs(getScrollY());
                int consumedY = Math.min(absScrollY, dy);
                scrollBy(0, consumedY);
                consumed[1] = consumedY;
                processRefreshHeader();
            }
        } else if (dy < 0) {//下拉
            if (isFooterInScreen()) {
                int scrollToBottomY = getScrollRange();
                int dScrollY = scrollToBottomY - getScrollY();
                int consumedY = Math.max(dScrollY, dy);
                scrollBy(0, consumedY);
                consumed[1] = consumedY;
                processRefreshFooter();
            }
        }
    }


    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
//        mParentHelper.onStopNestedScroll(target);
        Log.e(TAG, "onNestedScroll " + dyUnconsumed);

        scrollBy(0, dyUnconsumed);
        processRefresh();
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        Log.e(TAG, "onNestedPreFling");
//        return super.onNestedPreFling(target, velocityX, velocityY);
        return false;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        Log.e(TAG, "onNestedFling "+consumed);
        if(!consumed){
            fling((int) velocityY);
            return true;
        }
        return false;
    }

    @Override
    public void onStopNestedScroll(View child) {
        mParentHelper.onStopNestedScroll(child);
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
