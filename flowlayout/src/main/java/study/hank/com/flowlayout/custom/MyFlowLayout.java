package study.hank.com.flowlayout.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import study.hank.com.flowlayout.R;

/**
 * 我自定义的
 */
public class MyFlowLayout extends ViewGroup {

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


    int gravity;

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
    }

    private int defaultWidth = 100;
    private int defaultHeight = 100;

    private int finalW = 0, finalH = 0;//自身宽高，最终会setMeasureDimension保存起来

    private int currentRowWidth;// 临时变量，当前行的宽度，用于辅助计算我的宽度
    private int currentRowHeight = 0;//临时变量，当前行的高度

    //用二维数组保存每行每列上的子view
    List<View> currentRowViews;//临时保存行
    List<List<View>> totalViews;//保存 行*列
    List<Integer> rowHeights;//保存行高

    private void resetParams() {
        currentRowViews = new ArrayList<>();//临时保存行
        totalViews = new ArrayList<>();//保存行*列
        rowHeights = new ArrayList<>();//保存行高
        finalW = 0;
        finalH = 0;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

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

        final int childCount = getChildCount();
        if (childCount == 0)//当没有子view时，为了表示这里有一个FlowLayout，给个默认宽高呀
            setDefaultSize(widthMode, widthSize, heightMode, heightSize);

        Log.d("onMeasure", finalW + "-" + finalH);

        final int maxWidth = widthSize;//这是我爸爸的宽，也是我的儿子能达到的最大宽度，也是我的最大宽度，超出部分没有意义(除非我加上滑动事件，但是我现在不想加···)
        final int maxHeight = heightSize;

        //拿到了size和mode，现在来测量子

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);

            //现在的问题时如何取得子的margin
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            //测量，决定自己宽度的时候就要考虑leftMargin和rightMargin

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);//测量子view，测量之后，子view就有了自己的宽高
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            //然后是核心逻辑，所有子view的宽高我都拿到了，那我如何用子view的大小来决定 自身的大小？
            //这里有个特殊情况，如果childWidth已经超过了宽最大值
            childWidth = childWidth > maxWidth ? maxWidth : childWidth;//我不能让你无限宽，先进行矫正
            childHeight = childHeight > maxHeight ? maxHeight : childHeight;//我不能让你无限高，先进行矫正

            final int childWidthWithMargins = childWidth + lp.leftMargin + lp.rightMargin;
            final int childHeightWidthMargins = childHeight + lp.topMargin + lp.bottomMargin;

            //制定规则：放置一个child的时候，检查currentRowWidth加上childWidth有没有大于maxWidth，如果没有大于，那就在当前行的list中加上这个子view
            if (currentRowWidth + childWidthWithMargins + paddingLeft + paddingRight > maxWidth) {//判定是否要换行,这里确定要换行之后，把当前行，totalViews.add保存起来,那最后一行呢？也要存起来啊
                saveParams();
            }
            currentRowViews.add(child);
            currentRowWidth += childWidthWithMargins;//然后再累加
            currentRowHeight = Math.max(currentRowHeight, childHeightWidthMargins); //当前行的高度，取大

            //如果循环到了最后一个view，那就直接进行保存
            if (i == childCount - 1) {
                saveParams();
                finalH += paddingTop + paddingBottom;//测量最后一个子view的时候，padding 矫正
                finalW += paddingLeft + paddingRight;//padding 矫正
            }

        }

        setMeasuredDimension(finalW, finalH);//

    }

    private void saveParams() {
        finalW = Math.max(currentRowWidth, finalW);//当前值，和刚刚换行之前的宽，取大
        totalViews.add(currentRowViews);// 把换的那个行存起来
        rowHeights.add(currentRowHeight);
        finalH += currentRowHeight;// 对行高进行累加
        Log.d("saveParams", finalH + "    " + currentRowHeight);
        resetTempFields();
    }

    private void resetTempFields() {
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
}
