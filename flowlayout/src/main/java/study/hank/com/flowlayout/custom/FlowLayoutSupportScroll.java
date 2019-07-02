package study.hank.com.flowlayout.custom;

import android.content.Context;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import study.hank.com.flowlayout.custom.base.BaseFlowLayout;

/**
 * 支持竖向滚动的流式布局
 */
public class FlowLayoutSupportScroll extends BaseFlowLayout {

    public FlowLayoutSupportScroll(Context context) {
        super(context);
    }

    public FlowLayoutSupportScroll(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayoutSupportScroll(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    protected int mTouchSlop = 0;//系统支持的最小滑动距离
    private float mLastInterceptX = 0;
    private float mLastInterceptY = 0;

    @Override
    protected void init(Context context, AttributeSet attrs) {
        super.init(context, attrs);
        viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(viewConfiguration);

        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
        mFlingRunnable = new FlingRunnable(context);
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
                mFlingRunnable.stop();
                Log.d("", "");
                break;
            case MotionEvent.ACTION_MOVE:
                //假设此次是手指向上滑动，那么y-thisY 是正数
                float dy = thisY - mLastY;//差值 就是即将scrollBy的距离
                yScrollUsingScrollTo(dy);
                break;
            case MotionEvent.ACTION_UP:
                dealActionUp();//up事件由惯性处理的方法处理
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        mLastY = thisY;
        return super.onTouchEvent(e);
    }


    /**
     * y方向上的滚动,使用scrollTo
     *
     * @param deltaY
     */
    private void yScrollUsingScrollTo(float deltaY) {
        // *********** 使用scrollTo直接滑动 不带惯性，不带速度侦测 **************
        //  预测scrollBy之后的getScrollY值,并且进行边界矫正
        float targetY = getScrollY() - deltaY;// 预测已滑动的距离
        if (targetY < 0) {//如果此次move，预测目标小于0，那么强制纠正为0
            Log.d(TAG, "targetY 预测结果，上方边界即将越界，强制纠正为0 ");
            targetY = 0;
        } else if (targetY > canScrollY) {//如果此次move，预测目标大于最大值，那就等于最大值
            targetY = canScrollY;
            Log.d(TAG, "targetY 预测结果，下方边界即将越界，强制纠正为最大可滑动的距离 canScrollY=" + canScrollY);
        } else {
            Log.d(TAG, "正常范围内滑动，不做特殊处理" + targetY);
        }
        scrollTo(0, (int) targetY);//这里的delta 是正数，内容会上移,scroll完了之后，getScrollY会是正值
        //这里是瞬间性的滚动
    }

    //***************** up事件 惯性滑动的核心代码 *****************************
    private ViewConfiguration viewConfiguration;
    private int mMaximumVelocity, mMinimumVelocity;
    private FlingRunnable mFlingRunnable;//惯性滑动线程
    private VelocityTracker mVelocityTracker = null;//速度追踪器


    /**
     * 惯性滚动 runnable
     */
    private class FlingRunnable implements Runnable {

        private Scroller scroller;//惯性滚动的srcoller

        private int mInitY;

        FlingRunnable(Context context) {
            this.scroller = new Scroller(context, null, false);
        }

        void start(int initY,
                   int velocityY,
                   int maxY) {
            this.mInitY = initY;

            // 先停止上一次的滚动
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }

            // 开始 fling
            scroller.fling(0, initY, 0, velocityY, 0, 0, 0, maxY);
            //***************  fling 方法，只是为了 保存一系列参数值，******************
            //** 从左到右依次是 "x初始值"，"y初始值"，"x方向速度"，"y方向速度"，"x最小边界值"，"x最大边界值"，"y最小边界值"，"y最大边界值"  **
            post(this);//this是一个Runnable对象，post就是利用View.post(runnable)来执行runnable的run方法
        }

        /**
         * Scroller的作用原理：
         * 常用方法  startScroll() 平滑移动  fling() 带惯性的平滑移动
         * 但是这两个方法，你如果进去看，它只是保存了一系列参数值而已，并没有主动去 让view发生变化（很容易理解，new Scroller的时候并没有传view）
         * ，其中有一个重要参数，就是  mStartTime = AnimationUtils.currentAnimationTimeMillis();
         * 它利用AnimationUtils类的方法，获得一个当前时间。
         * <p>
         * 要想让view发生变化，滚动。必须调用scroller.computeScrollOffset
         * 这个方法，将会计算时间差值，从而 改变currX或currY的值（这两个值，其实是本次滚动的预计目标位置坐标值），
         * 在我们调用了scroll.computeScrollOffset之后，就能利用 scroller.getCurrY 获得 y方向上本次滚动的目标位置
         * 但是现在还没有滑动（强调！）
         * scroller.getCurrY 得到的这个值，只是预测值， 不代表我们最重要滑动的距离，因为涉及到滑动边界的问题，我们可以对这个值进行边界矫正
         * <p>
         * 矫正之后，我们调用view的scrollTo或者scrollBy来操作view本身的滚动。
         * <p>
         * （强调!）
         * 然后！ 重复调用上面 Scroller.computeScrollOffset之后的过程，直到滚动达到目标!
         * <p>
         * 是的，Scroller的套路就是这样，Scroller类只负责计算，
         * <p>
         * ((((我们要调用它的computeScrollOffset之后，在getCurrY获得预计偏移量，然后边界矫正，))))
         * 然后 重复括号里的内容, 直到达到目标位置.
         * <p>
         * 这个听上去有点像while循环有米有！
         */
        @Override
        public void run() {

            // 如果已经结束，就不再进行
            if (!scroller.computeScrollOffset()) {//上面执行了fling参数，其中有一个重要参数（要去看源码内部 ）.
                return;
            }

            // 计算偏移量
            int currY = scroller.getCurrY();//先保存当前滚动的位置
            int diffY = mInitY - currY;//初始位置和当前位置的差值（因为后面使用的是scrollBy，所以要计算差值）

            // 用于记录是否超出边界，如果已经超出边界，则不再进行回调，即使滚动还没有完成
            boolean isEnd = false;// 是否滚动结束

            if (diffY != 0) {
                // 超出下边界，进行修正
                if (getScrollY() + diffY >= canScrollY) {
                    diffY = (canScrollY - getScrollY());
                    isEnd = true;
                }

                // 超出上边界，进行修正
                if (getScrollY() <= 0) {
                    diffY = -getScrollY();
                    isEnd = true;
                }

                if (!scroller.isFinished()) {
                    Log.d("FlingRunnable", "" + diffY);
                    scrollBy(0, diffY);
                }
                mInitY = currY;
            }

            if (!isEnd) {
                post(this);
            }
        }

        /**
         * 进行停止
         */
        void stop() {
            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }
        }
    }

    /**
     * 处理惯性滑动，要在actionUp时执行
     */
    private void dealActionUp() {
        if (mVelocityTracker != null) {
            //在up，手指抬起的时候，才根据滑动速度来进行惯性滑动
            mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
            //获得y上的速度
            float velocityY = mVelocityTracker.getYVelocity();
            if (Math.abs(velocityY) > mMinimumVelocity) {//比较滑动的最小速度
                //如果那就要根据速度再多滑动一段距离啦
                //其实是用了一个Runnable ,post（runnable）,逻辑全在runnable里面呀！
                int curY = getScrollY();
                if (canScrollY > 0) {
                    mFlingRunnable.start(curY, (int) velocityY, canScrollY);//参数含义，依次为：起始Y，当前Y速率，能够滑动的最大距离限制
                }

            }
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
        }
    }
}
