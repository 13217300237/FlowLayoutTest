package study.hank.com.flowlayout.custom;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import study.hank.com.flowlayout.custom.base.BaseFlowLayout;

/**
 * 利用属性动画进行平移，制造滚动效果
 * <p>
 * 目前的计划是：ValueAnimator 只是一个计算器的作用，
 * 就像 Scroller/OverScroller 他们也只是充当计算器，
 * 真正去平移，还是要用到scrollTo或者scrollBy.
 * <p>
 * 那么属性动画，其实自带了平移方法 TranslationAnimator
 */
public class FlowLayoutUsingPropertyAnimator extends BaseFlowLayout {


    public FlowLayoutUsingPropertyAnimator(Context context) {
        super(context);
    }

    public FlowLayoutUsingPropertyAnimator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayoutUsingPropertyAnimator(Context context, AttributeSet attrs, int defStyleAttr) {
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
                if (flingAnimator != null && ifAnimating) {
                    flingAnimator.cancel();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                //假设此次是手指向上滑动，那么y-thisY 是正数
                float dy = thisY - mLastY;//差值 就是即将scrollBy的距离
                Log.d("dyTag", "" + dy);
                yScrollUsingSetTranslationY(dy);//这是这一次move的dy
                break;
            case MotionEvent.ACTION_UP:
                dealActionUp();
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
    private void yScrollUsingSetTranslationY(float deltaY) {
        if (Math.abs(deltaY) > mTouchSlop) {
            float targetY = getTranslationY() + deltaY;// 预测已滑动的距离
            Log.d("getTranslationY", getTranslationY() + "");
            if (judgeOverEdge(targetY))
                return;
            setTranslationY(targetY);//这里的translationY 到底是什么含义。 从现象上来看，它兼容了内部的画布滚动。
        }
    }

    /**
     * 越界判定
     *
     * @param targetY
     * @return
     */
    private boolean judgeOverEdge(float targetY) {
        if (targetY > 0) return true;// 上边越界
        if (Math.abs(targetY) > canScrollY) return true;//下边越界
        return false;
    }

    private boolean ifAnimating = false;
    private ValueAnimator flingAnimator;

    /**
     * 测试失败，用属性动画直接做move的移动，貌似不太合适，因为move事件是连续的，时间间隔不确定，
     * 如果用属性动画来做，动画时长不好把控。
     * 所以放弃此方案
     * <p>
     * 值用它来做惯性滑动,和边界回弹.
     *
     * @param dy
     */
    public void moveUsingPropertyAnimator(float dy) {
        final float startY = getTranslationY();//当前 translationY
        final float targetY = startY + dy;

        flingAnimator = ValueAnimator.ofFloat(startY, targetY);
        flingAnimator.setInterpolator(new LinearOutSlowInInterpolator());
        flingAnimator.setDuration(500);
        flingAnimator.addUpdateListener(animation -> {
            float curY = (float) animation.getAnimatedValue();
            if (judgeOverEdge(curY)) return;
            setTranslationY(curY); // 这个东西的意义，我好像有点不明白
            Log.d("scrollYTag", "" + getTranslationY());
        });

        flingAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ifAnimating = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                ifAnimating = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                ifAnimating = false;
            }
        });
        flingAnimator.start();
    }

    /**
     * 处理惯性滑动，要在actionUp时执行
     */
    private void dealActionUp() {//在up，手指抬起的时候，才根据滑动速度来进行惯性滑动
        if (mVelocityTracker != null) {
            mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
            float velocityY = mVelocityTracker.getYVelocity();//获得y上的速度
            if (Math.abs(velocityY) > mMinimumVelocity) {//大于最小滑动速度，才让你惯性
                int curY = getScrollY();
                if (canScrollY > 0) {//只有能够滑动，才执行下面的
                    Log.d("velocityY", "" + velocityY);
                    //这里使用属性动画进行滚动，对比之前的方案，入参是：速度 ，只有速度。。然后惯性滑动多少，应该是由速度决定的.
                    // 先做简单的，如果速度满足最低限制，就滑动固定距离,并且手指再次down，则停止动画
                    moveUsingPropertyAnimator(velocityY / 5);
                }

            }
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
        }
    }

    //***************** up事件 惯性滑动的核心代码 *****************************
    private ViewConfiguration viewConfiguration;
    private int mMaximumVelocity, mMinimumVelocity;
    private VelocityTracker mVelocityTracker = null;//速度追踪器


}
