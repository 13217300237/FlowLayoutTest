package study.hank.com.flowlayout.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

import study.hank.com.flowlayout.R;

/**
 * 我自定义的
 */
public class MyFlowLayout extends ViewGroup {

    private static final String TAG = "MyFlowLayoutTag";

    public MyFlowLayout(Context context) {
        this(context, null);
    }

    public MyFlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyFlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private int defaultWidth = Utils.dp2px(200);
    private int defaultHeight = Utils.dp2px(100);

    private int finalW = 0, finalH = 0;//自身宽高，最终会setMeasureDimension保存起来
    private int parentMaxHeight;

    private int currentRowWidth;// 临时变量，当前行的宽度，用于辅助计算我的宽度
    private int currentRowHeight = 0;//临时变量，当前行的高度

    //用二维数组保存每行每列上的子view
    List<View> currentRowViews;//临时保存行
    List<List<View>> totalViews;//保存 行*列
    List<Integer> rowHeights;//保存行高

    private int gravity;

    private void init(Context context, AttributeSet attrs) {
        //获取自定义属性
        TypedArray ta = null;
        try {
            ta = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout);
            gravity = ta.getInt(R.styleable.FlowLayout_android_gravity, Gravity.TOP);
        } finally {
            if (ta != null)
                ta.recycle();
        }

        setClickable(true);//让这个viewGroup事件不穿透
        viewConfiguration = ViewConfiguration.get(context);
        mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(viewConfiguration);

        mMaximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
        mFlingRunnable = new FlingRunnable(context);
    }


    /**
     * 重置参数
     * 测量之前，必须重置，因为测量会有多次
     */
    private void resetParams() {
        currentRowViews = new ArrayList<>();//临时保存行
        totalViews = new ArrayList<>();//保存行*列
        rowHeights = new ArrayList<>();//保存行高
        finalW = 0;
        finalH = 0;
    }

    /**
     * 为了让child.getLayoutParam返回MarginLayoutParam，必须重写这个
     *
     * @param attrs
     * @return
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    private int mTouchSlop = 0;//系统支持的最小滑动距离
    private int mLastInterceptX = 0;
    private int mLastInterceptY = 0;

    /**
     * 目标：所有纵向滑动由我自己处理。down不拦截，
     *
     * @param ev
     * @return
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean ifIntercept = false;
        int interceptX = 0;
        int interceptY = 0;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastInterceptX = (int) ev.getY();
                mLastInterceptY = (int) ev.getY();
                ifIntercept = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = interceptX - mLastInterceptX;
                int dy = interceptY - mLastInterceptY;
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

    int touchY;

    //滑动事件处理
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(e);
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchY = (int) e.getY();
                mFlingRunnable.stop();
                break;
            case MotionEvent.ACTION_MOVE:
                final int thisY = (int) e.getY();//本次event的Y值
                //假设此次是手指向上滑动，那么y-thisY 是正数
                int deltaY = thisY - touchY;//差值 就是即将scrollBy的距离
                yScrollTo(deltaY);
                touchY = thisY;//更新Y
                break;
            case MotionEvent.ACTION_UP:
                dealActionUp();
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        return super.onTouchEvent(e);
    }


    /**
     * y方向上的滚动
     *
     * @param deltaY
     */
    private void yScrollTo(int deltaY) {
        // *********** 使用scrollTo直接滑动 不带惯性，不带速度侦测 **************
        //  预测scrollBy之后的getScrollY值,并且进行边界矫正
        int targetY = getScrollY() - deltaY;// 预测已滑动的距离
        if (targetY < 0) {//如果此次move，预测目标小于0，那么强制纠正为0
            Log.d(TAG, "targetY 预测结果，上方边界即将越界，强制纠正为0 ");
            targetY = 0;
        } else if (targetY > canScrollY) {//如果此次move，预测目标大于最大值，那就等于最大值
            targetY = canScrollY;
            Log.d(TAG, "targetY 预测结果，下方边界即将越界，强制纠正为最大可滑动的距离 canScrollY=" + canScrollY);
        } else {
            Log.d(TAG, "正常范围内滑动，不做特殊处理");
        }
        scrollTo(0, targetY);//这里的delta 是正数，内容会上移,scroll完了之后，getScrollY会是正值
    }

    /**
     * 多次对子进行测量，来兼容子view match_parent的情况，如果宽match_parent，则自己占据一行
     * 如果高match_parent，则给默认高度
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {//子自身的限制条件MeasureSpec

        resetParams();//测量可能发生多次，每一次都要重新设置list

        //取得所有padding,他们将影响测量结果,测量自身，会考虑
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        // 1.解析MeasureSpec,这里的两个值，到底是怎么来的，起什么作用？源头貌似很难查，找不到一个可靠的入口
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);//这个size其实就是父容器的宽
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);//这个size其实就是父容器的高
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        parentMaxHeight = heightSize;

        final int childCount = getChildCount();
        if (childCount == 0)//当没有子view时，为了表示这里有一个FlowLayout，给个默认宽高呀
            setDefaultSize(widthMode, widthSize, heightMode, heightSize);

        Log.d("onMeasure", finalW + "-" + finalH);

        final int maxWidth = widthSize;//这是我爸爸的宽，也是我的儿子能达到的最大宽度，也是我的最大宽度，超出部分没有意义(除非我加上滑动事件，但是我现在不想加···)
        final int maxHeight = heightSize;

        //拿到了size和mode，现在来测量子
        int maxChildLeftRightMargin = 0;
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            //现在的问题时如何取得子的margin
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            Log.d(TAG + "_measure", "" + lp.width);
            //测量，决定自己宽度的时候就要考虑leftMargin和rightMargin

            boolean ifWidthMatchParent = lp.width == LayoutParams.MATCH_PARENT;
            boolean ifHeightMatchParent = lp.height == LayoutParams.MATCH_PARENT;
            // 如果高度是沾满呢，现在系统默认的效果是，好像是搞成了wrap_content测量的，那如果我非要给她设定一个默认高呢？

            if (!ifWidthMatchParent && !ifHeightMatchParent)//如果宽度是matchParent，则这次不与测量，改在后面测
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);//测量子view，测量之后，子view就有了自己的宽高
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            //然后是核心逻辑，所有子view的宽高我都拿到了，那我如何用子view的大小来决定 自身的大小？
            //这里有个特殊情况，如果childWidth已经超过了宽最大值
            childWidth = childWidth > maxWidth ? maxWidth : childWidth;//我不能让你无限宽，先进行矫正
            childHeight = childHeight > maxHeight ? maxHeight : childHeight;//我不能让你无限高，先进行矫正

            final int childWidthWithMargins = childWidth + lp.leftMargin + lp.rightMargin;
            final int childHeightWidthMargins = childHeight + lp.topMargin + lp.bottomMargin;

            maxChildLeftRightMargin = Math.max(lp.leftMargin + lp.rightMargin, maxChildLeftRightMargin);

            //发现，match_parent是-1，那就判断是不是-1，如果是-1，则直接换行

            boolean ifOverLineMaxWidth = currentRowWidth + childWidthWithMargins + paddingLeft + paddingRight > maxWidth;
            //制定规则：放置一个child的时候，检查currentRowWidth加上childWidth有没有大于maxWidth，如果没有大于，那就在当前行的list中加上这个子view
            if (ifOverLineMaxWidth) {//预测view加进去之后，此行的宽，如果超过了最大值，就保存当前行，重置当前行的辅助参数
                saveCurrentRow();
            }
            if (ifWidthMatchParent) {//如果当前view是宽matchParent，那就单独直接换行
                saveCurrentRow();//这里可能会保存空行，因为如果还没有add到currentRowViews中
                //这里换行，是考虑到（当前行已经有元素了，但是下一个view是matchParent，那么就不能让它呆在这一行）
            }

            //在当前行里面增加一个子view
            currentRowViews.add(child);//当前行，增加一个子
            currentRowWidth += childWidthWithMargins;//宽度递增
            currentRowHeight = Math.max(currentRowHeight, childHeightWidthMargins); //当前行的高度，取大

            if (ifWidthMatchParent) {//如果在把view加到 currentRowViews之后，发现它是matchParent，那就换行
                saveCurrentRow();
            }

            //如果循环到了最后一个view，那就直接进行保存最后一行
            if (i == childCount - 1) {
                saveCurrentRow();
                finalH += paddingTop + paddingBottom;//测量最后一个子view的时候，padding 矫正
                finalW += paddingLeft + paddingRight;//padding 矫正
            }
        }

        //再考虑一个情况，如果你全都是matchParent的view呢？那就给一个默认值，
        if (finalW == paddingLeft + paddingRight + maxChildLeftRightMargin)
            finalW = defaultWidth + paddingLeft + paddingRight;

        reMeasureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(finalW, finalH);//
        initCanScrollY();//测量完成之后，Y方向能够滑动的最大距离也就确定了
    }

    /**
     * 重新测量，针对出现宽高为match_parent的情况(因为上面的测量，没有对类进行处理)
     */
    private void reMeasureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        for (int i = 0; i < totalViews.size(); i++) {
            List<View> currentRow = totalViews.get(i);
            Log.d("currentRowTag", "第" + i + "行:" + currentRow.size());
            //找出一行只有一个view的行，对它进行测量
            for (int j = 0; j < currentRow.size(); j++) {
                View v = currentRow.get(j);
                MarginLayoutParams lpT = (MarginLayoutParams) v.getLayoutParams();
                if (lpT.width == LayoutParams.MATCH_PARENT && lpT.height == LayoutParams.MATCH_PARENT) {
                    //如果宽高都是matchParent呢,宽，设定为行宽
                    int wms = MeasureSpec.makeMeasureSpec(finalW - lpT.leftMargin - lpT.rightMargin, MeasureSpec.EXACTLY);
                    //高，设定为默认值
                    int hms = MeasureSpec.makeMeasureSpec(defaultHeight, MeasureSpec.EXACTLY);
                    v.measure(wms, hms);
                } else if (lpT.width == LayoutParams.MATCH_PARENT) {//只有宽matchParent
                    //只有宽是matchParent，则，宽设定为
                    int wms = MeasureSpec.makeMeasureSpec(finalW - lpT.leftMargin - lpT.rightMargin, MeasureSpec.EXACTLY);
                    int hms;
                    if (lpT.height > 0)// 大于0,说明是精确值,直接设置即可
                        hms = MeasureSpec.makeMeasureSpec(lpT.height, MeasureSpec.EXACTLY);//宽matchParent，高200dp的情况下，这里的size
                    else {//否则就是wrap_content或者matchParent
                        hms = getChildMeasureSpec(heightMeasureSpec,
                                getPaddingTop() + getPaddingBottom() + lpT.topMargin + lpT.bottomMargin, lpT.height);//使用系统方法
                    }

                    v.measure(wms, hms);
                } else if (lpT.height == LayoutParams.MATCH_PARENT) {//只有高是matchParent,那就把matchParent设定一个默认值，宽度则按照它自身的处理
                    int wms;
                    if (lpT.width > 0)// 大于0,说明是精确值,直接设置即可
                        wms = MeasureSpec.makeMeasureSpec(lpT.width, MeasureSpec.EXACTLY);//宽matchParent，高200dp的情况下，这里的size
                    else {//否则就是wrap_content或者matchParent
                        wms = getChildMeasureSpec(widthMeasureSpec,
                                getPaddingLeft() + getPaddingRight() + lpT.leftMargin + lpT.rightMargin, lpT.width);//使用系统方法
                    }
                    int hms = MeasureSpec.makeMeasureSpec(defaultHeight, MeasureSpec.EXACTLY);
                    v.measure(wms, hms);
                    //为什么你单独在一行?我只是让你的高matchParent,没道理,其实没有··
                }
            }
        }
        Log.d("currentRowTag", "===============" + finalW);//
    }


    /**
     * 保存当前行
     */
    private void saveCurrentRow() {
        finalW = Math.max(currentRowWidth, finalW);//当前值，和刚刚换行之前的宽，取大
        if (currentRowViews.size() > 0) {
            totalViews.add(currentRowViews);// 把换的那个行存起来
            rowHeights.add(currentRowHeight);
            finalH += currentRowHeight;// 对行高进行累加
        }
        Log.d("saveCurrentRow", finalH + "    " + currentRowHeight);
        resetCurRow();
    }

    /**
     *
     */
    private void resetCurRow() {
        currentRowViews = new ArrayList<>();//当前行重置
        currentRowWidth = 0;//当前行宽度重置
        currentRowHeight = 0;
    }

    /**
     * 当布局xml里面写wrap_content时，需要先给一个默认大小，不然就默认撑满父容器
     *
     * @param widthMode
     * @param widthSize
     * @param heightMode
     * @param heightSize
     */
    private void setDefaultSize(int widthMode, int widthSize, int heightMode, int heightSize) {
        switch (widthMode) {
            case MeasureSpec.EXACTLY://xml中直接指定了100dp，或者match_parent
                finalW = widthSize;
                break;
            case MeasureSpec.AT_MOST://xml中指定了wrap_content
            case MeasureSpec.UNSPECIFIED:// 未指定，意味着我可以随便大小，但是最好超出父容器的部分我也看不到，所以还是算了
                finalW = defaultWidth;
                break;
        }

        switch (heightMode) {
            case MeasureSpec.EXACTLY://xml中直接指定了100dp，或者match_parent
                finalH = heightSize;
                break;
            case MeasureSpec.AT_MOST://xml中指定了wrap_content
            case MeasureSpec.UNSPECIFIED:// 这种未指定，看上去像是异常情况，一般情况下无论是 静态还是动态方式，都不会不指定宽高,也许吧，防止意外情况
                finalH = defaultHeight;
                break;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int yOffset = getPaddingTop();//Y方向偏移量
        //现在我有了，所有行里面的view，所有列里面的行，还有所有的行高，现在对这些子view进行child.layout
        for (int i = 0; i < totalViews.size(); i++) {
            List<View> row = totalViews.get(i);//当前这一行
            //遍历当前行
            int xOffset = getPaddingLeft();//X方向偏移量
            for (int j = 0; j < row.size(); j++) {
                View v = row.get(j);// 第一个view怎么排？
                MarginLayoutParams lp = (MarginLayoutParams) v.getLayoutParams();

                int finalLeft = xOffset + lp.leftMargin;
                int finalTop = yOffset + lp.topMargin;
                int finalRight = xOffset + v.getMeasuredWidth() + lp.leftMargin;
                int finalBottom = yOffset + v.getMeasuredHeight() + lp.topMargin;

                int moveX = rowHeights.get(i) - v.getMeasuredHeight() - lp.topMargin - lp.bottomMargin;//要减去上下margin
                //根据gravity在知道行高的情况下，来计算做标识,想想也就三种情况，上，中下
                switch (gravity) {
                    case Gravity.CENTER | Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL:
                        finalTop += moveX / 2;
                        finalBottom += moveX / 2;
                        break;
                    case Gravity.BOTTOM:
                        //如果是靠下，那么要将TOP和BOTTOM调整一下,都要移动 行高和子高的差值
                        finalTop += moveX;
                        finalBottom += moveX;
                        break;
                    case Gravity.TOP://现在好像就是这个样子的···
                    default:
                        break;
                }

                v.layout(finalLeft,
                        finalTop,
                        finalRight,
                        finalBottom);
                xOffset += v.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            }
            yOffset += rowHeights.get(i);//Y偏移量，按行高增量，行高已经考虑了topMargin和bottomMargin
        }

    }

    private int canScrollY;// 允许滑动的最大距离

    /**
     * 允许滑动的最大距离初始化
     */
    private void initCanScrollY() {
        canScrollY = getMeasuredHeight() - parentMaxHeight;
        if (canScrollY < 0)//不允许越界
            canScrollY = 0;
    }


    //***************** 惯性滑动的核心代码 *****************************
    private ViewConfiguration viewConfiguration;
    private int mMaximumVelocity, mMinimumVelocity;
    private FlingRunnable mFlingRunnable;//惯性滑动线程
    private VelocityTracker mVelocityTracker = null;//速度追踪器

    /**
     * 滚动线程
     */
    private class FlingRunnable implements Runnable {

        private Scroller mScroller;

        private int mInitY;
        private int mMinY;
        private int mMaxY;
        private int mVelocityY;

        FlingRunnable(Context context) {
            this.mScroller = new Scroller(context, null, false);
        }

        void start(int initY,
                   int velocityY,
                   int minY,
                   int maxY) {
            this.mInitY = initY;
            this.mVelocityY = velocityY;
            this.mMinY = minY;
            this.mMaxY = maxY;

            // 先停止上一次的滚动
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }

            // 开始 fling
            mScroller.fling(0, initY, 0,
                    velocityY, 0, 0, 0, maxY);
            post(this);
        }

        @Override
        public void run() {

            // 如果已经结束，就不再进行
            if (!mScroller.computeScrollOffset()) {
                return;
            }

            // 计算偏移量
            int currY = mScroller.getCurrY();//先保存当前滚动的距离
            int diffY = mInitY - currY;//初始位置和当前位置的差值

            Log.i(TAG, "run: [currY: " + currY + "]"
                    + "[diffY: " + diffY + "]"
                    + "[initY: " + mInitY + "]"
                    + "[minY: " + mMinY + "]"
                    + "[maxY: " + mMaxY + "]"
                    + "[velocityY: " + mVelocityY + "]"
            );

            // 用于记录是否超出边界，如果已经超出边界，则不再进行回调，即使滚动还没有完成
            boolean isEnd = false;

            if (diffY != 0) {

                // 超出下边界，进行修正
                if (getScrollY() + diffY >= canScrollY) {
                    diffY = (canScrollY - getScrollY());
                    isEnd = true;
                }

                // 超出左边界，进行修正
                if (getScrollY() <= 0) {
                    diffY = -getScrollY();
                    isEnd = true;
                }

                if (!mScroller.isFinished()) {
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
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
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
                    mFlingRunnable.start(curY, (int) velocityY, curY, canScrollY);
                }

            }
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
        }
    }
}
