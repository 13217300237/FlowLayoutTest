package study.hank.com.flowlayout.custom;

import android.content.Context;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import study.hank.com.flowlayout.custom.base.BaseFlowLayout;

/**
 * 使用OverScroller，支持边界回弹 spring
 */
public class FlowLayoutUsingOverScroller extends BaseFlowLayout {

    public FlowLayoutUsingOverScroller(Context context) {
        super(context);
    }

    public FlowLayoutUsingOverScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayoutUsingOverScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected int mTouchSlop = 0;//系统支持的最小滑动距离
    private float mLastInterceptX = 0;
    private float mLastInterceptY = 0;

    private OverScroller mScroller;//支持弹性越界的scroller

    @Override
    protected void init(Context context, AttributeSet attrs) {
        super.init(context, attrs);
        viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(viewConfiguration);

        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();

        mScroller = new OverScroller(context);
    }

    /**
     * 目标：所有纵向滑动由我自己处理。down不拦截，
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean ifIntercept = false;
        float interceptX = ev.getX();//必须每次更新当前XY，而不是只在down的时候
        float interceptY = ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                ifIntercept = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = interceptX - mLastInterceptX;
                float dy = interceptY - mLastInterceptY;
                if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > mTouchSlop) {//纵向滑动并且滑动距离大于TouchSlop
                    ifIntercept = true;//一旦拦截，此次event序列就会交给自身的onTouchEvent处理
                } else {
                    ifIntercept = false;//不拦截，就让下一层收到event，它来决定是否消费，如果消费，则我的onTouchEvent不执行，不消费，则我的onTouchEvent执行
                }

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                ifIntercept = false;
                break;
        }
        mLastInterceptX = interceptX;
        mLastInterceptY = interceptY;
        return ifIntercept;
    }

    float mLastY;

    private void initVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    //滑动事件处理
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (canScrollY <= 0) {// 如果不可滑动，那就不必走下面的逻辑了
            return super.onTouchEvent(e);
        }

        initVelocityTracker();
        mVelocityTracker.addMovement(e);

        float thisY = e.getY();//每一次事件都更新thisY

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //假设此次是手指向上滑动，那么y-thisY 是正数
                float dy = thisY - mLastY;//差值 就是即将scrollBy的距离
                yScrollUsingOverScroller((int) -dy);
                break;
            case MotionEvent.ACTION_UP:
                dealActionUpWithOverScroller();
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        mLastY = thisY;
        return super.onTouchEvent(e);
    }

    private void dealActionUpWithOverScroller() {
        final VelocityTracker velocityTracker = mVelocityTracker;
        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
        int initialVelocity = (int) velocityTracker.getYVelocity();

        if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
            fling(-initialVelocity);//惯性滚动，自带边界回弹
        } else if (mScroller.springBack(getScrollX(), getScrollY(), 0, 0, 0,
                (finalH - parentMaxHeight))) {// 越界回弹
            postInvalidateOnAnimation();
        }

    }

    /**
     * 惯性滚动(fling方法本身就带边界)
     *
     * @param velocityY
     */
    public void fling(int velocityY) {
        if (getChildCount() > 0) {
            int height = parentMaxHeight;
            int bottom = finalH;

            mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0,
                    Math.max(0, bottom - height), 0, height / 2);

            postInvalidateOnAnimation();
        }
    }

    /**
     * 使用scroller来做滚动
     */
    private void yScrollUsingOverScroller(int dy) {
        mScroller.startScroll(0, mScroller.getFinalY(), 0, dy);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {// 返回值,true,就说明滚动未完成
            int targetY = mScroller.getCurrY();
            scrollTo(0, targetY);
            postInvalidate();
        }
    }


    //***************** up事件 惯性滑动的核心代码 *****************************
    private ViewConfiguration viewConfiguration;
    private int mMaximumVelocity, mMinimumVelocity;
    private VelocityTracker mVelocityTracker = null;//速度追踪器


}
