package code.wangyuelin.com.simplecode;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

/**
 *
 * 自定义的View，包括：1、滑动  2、惯性滑动 3、滑到头反弹  4、自己写measure测量 5、自己写layout放置子view
 */
public class CustomScrollView extends ViewGroup {


    private int desireWidth, desireHeight;
    private VelocityTracker velocityTracker;
    private int mPointerId;
    private float x, y;
    private OverScroller mScroller;
    private int maxFlingVelocity, minFlingVelocity;
    private int mTouchSlop;
    protected Boolean isMove = false;
    protected float downX = 0, downY = 0;
    private int top_hight = 0;
    private int scrollYButtom = 0;
    private int nScrollYButtom = 0;

    private int pullDownMin = 0;
    private Boolean isEnablePullDown = true;

    private Boolean isFirst = true;

    public void setEnablePullDown(Boolean isEnablePullDown) {
        this.isEnablePullDown = isEnablePullDown;
    }

    public CustomScrollView(Context context) {
        super(context);
        init(null, 0);
    }

    public CustomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public CustomScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
//        final TypedArray a = getContext().obtainStyledAttributes(
//                attrs, R.styleable.PullTorefreshScrollView, defStyle, 0);
//
//
//        a.recycle();
        mScroller = new OverScroller(getContext());
        maxFlingVelocity = ViewConfiguration.get(getContext()).getScaledMaximumFlingVelocity();
        minFlingVelocity = ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity();
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

    }


    public int getScrollYTop() {
        return top_hight;
    }

    public int getScrollYButtom() {
        return scrollYButtom;
    }

    public int getNScrollYTop() {
        return 0;
    }

    public int getNScrollYButtom() {
        return nScrollYButtom;
    }

    public int measureWidth(int widthMeasureSpec) {
        int result = 0;
        int measureMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        switch (measureMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                result = width;
                break;
            default:
                break;
        }
        return result;
    }

    public int measureHeight(int heightMeasureSpec) {
        int result = 0;
        int measureMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        switch (measureMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                result = height;
                break;
            default:
                break;
        }
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 计算所有child view 要占用的空间
        int width = measureWidth(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);

        desireWidth = 0;
        desireHeight = 0;
        int count = getChildCount();
        for (int i = 0; i < count; ++i) {
            View v = getChildAt(i);

            if (v.getVisibility() != View.GONE) {

                LayoutParams lp = (LayoutParams) v.getLayoutParams();
                measureChildWithMargins(v, widthMeasureSpec, 0,
                        heightMeasureSpec, 0);

                Log.d("rrr", "子view测量的的高度：" + v.getMeasuredHeight());
                //只是在这里增加了垂直或者水平方向的判断
                desireWidth = Math.max(desireWidth, v.getMeasuredWidth()
                        + lp.leftMargin + lp.rightMargin);
                desireHeight += v.getMeasuredHeight() + lp.topMargin
                        + lp.bottomMargin;

            }
        }

        // count with padding
        desireWidth += getPaddingLeft() + getPaddingRight();
        desireHeight += getPaddingTop() + getPaddingBottom();

        // see if the size is big enough
        desireWidth = Math.max(desireWidth, getSuggestedMinimumWidth());
        desireHeight = Math.max(desireHeight, getSuggestedMinimumHeight());


        //处理内容比较少的时候，就添加一定的高度
        int scrollHight = height + top_hight * 2;
        if (scrollHight > desireWidth) {
            int offset = scrollHight - desireHeight;
            View view = new View(getContext());
            view.setBackgroundResource(R.color.colorAccent);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, offset);
            addView(view, getChildCount() - 1, lp);
            desireWidth = scrollHight;
        }


        setMeasuredDimension(resolveSize(desireWidth, widthMeasureSpec),
                resolveSize(desireHeight, heightMeasureSpec));
        Log.d("rrr", "所有子view测量的的高度：" + desireHeight + " 本身的高度：" + getMeasuredHeight());

        //desireWidth:所有子view要占用的高度，getMeasuredHeight()本身的高度  top_hight：头部的高度
        scrollYButtom = desireHeight - getMeasuredHeight() - top_hight; // header 和foot隐藏的状态
        nScrollYButtom = desireHeight - getMeasuredHeight(); // header 和foot显示的状态
        //如果上啦拖出一半的高度，就代表将要执行上啦
        pullDownMin = nScrollYButtom - top_hight / 2;
        if (isFirst) {
            scrollTo(0, top_hight);
            isFirst = false;
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int parentLeft = getPaddingLeft();
        final int parentRight = r - l - getPaddingRight();
        final int parentTop = getPaddingTop();
        final int parentBottom = b - t - getPaddingBottom();

        if (BuildConfig.DEBUG)
            Log.d("onlayout", "parentleft: " + parentLeft + "   parenttop: "
                    + parentTop + "   parentright: " + parentRight
                    + "   parentbottom: " + parentBottom);

        int left = parentLeft;
        int top = parentTop;

        int count = getChildCount();
        for (int i = 0; i < count; ++i) {
            View v = getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                LayoutParams lp = (LayoutParams) v.getLayoutParams();
                final int childWidth = v.getMeasuredWidth();
                final int childHeight = v.getMeasuredHeight();
                final int gravity = lp.gravity;
                final int horizontalGravity = gravity
                        & Gravity.HORIZONTAL_GRAVITY_MASK;
                final int verticalGravity = gravity
                        & Gravity.VERTICAL_GRAVITY_MASK;


                // layout vertical, and only consider horizontal gravity

                left = parentLeft;
                top += lp.topMargin;
                switch (horizontalGravity) {
                    case Gravity.LEFT:
                        break;
                    case Gravity.CENTER_HORIZONTAL:
                        left = parentLeft
                                + (parentRight - parentLeft - childWidth) / 2
                                + lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        left = parentRight - childWidth - lp.rightMargin;
                        break;
                }
                v.layout(left, top, left + childWidth, top + childHeight);
                top += childHeight + lp.bottomMargin;
            }

        }


    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(
            AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(
            ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends MarginLayoutParams {
        public int gravity = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);


        }

        public LayoutParams(int width, int height) {
            this(width, height, -1);
        }

        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }


    /**
     * onInterceptTouchEvent()用来询问是否要拦截处理。 onTouchEvent()是用来进行处理。
     * <p/>
     * 例如：parentLayout----childLayout----childView 事件的分发流程：
     * parentLayout::onInterceptTouchEvent()---false?--->
     * childLayout::onInterceptTouchEvent()---false?--->
     * childView::onTouchEvent()---false?--->
     * childLayout::onTouchEvent()---false?---> parentLayout::onTouchEvent()
     * <p/>
     * <p/>
     * <p/>
     * 如果onInterceptTouchEvent()返回false，且分发的子View的onTouchEvent()中返回true，
     * 那么onInterceptTouchEvent()将收到所有的后续事件。
     * <p/>
     * 如果onInterceptTouchEvent()返回true，原本的target将收到ACTION_CANCEL，该事件
     * 将会发送给我们自己的onTouchEvent()。
     */
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
            // 该事件可能不是我们的
            return false;
        }

        boolean isIntercept = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 如果动画还未结束，则将此事件交给onTouchEvet()处理，
                // 否则，先分发给子View
                isIntercept = !mScroller.isFinished();
                // 如果此时不拦截ACTION_DOWN时间，应该记录下触摸地址及手指id，当我们决定拦截ACTION_MOVE的event时，
                // 将会需要这些初始信息（因为我们的onTouchEvent将可能接收不到ACTION_DOWN事件）
                mPointerId = ev.getPointerId(0);
//          if (!isIntercept) {
                downX = x = ev.getX();
                downY = y = ev.getY();
//          }
                break;
            case MotionEvent.ACTION_MOVE:
                int pointerIndex = ev.findPointerIndex(mPointerId);
                float mx = ev.getX(pointerIndex);
                float my = ev.getY(pointerIndex);
                // 根据方向进行拦截，（其实这样，如果我们的方向是水平的，里面有一个ScrollView，那么我们是支持嵌套的）
                if (Math.abs(y - my) >= mTouchSlop) {
                    isIntercept = true;
                }
                //如果不拦截的话，我们不会更新位置，这样可以通过累积小的移动距离来判断是否达到可以认为是Move的阈值。
                //这里当产生拦截的话，会更新位置（这样相当于损失了mTouchSlop的移动距离，如果不更新，可能会有一点点跳的感觉）
                if (isIntercept) {
                    x = mx;
                    y = my;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // 这是触摸的最后一个事件，无论如何都不会拦截
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                solvePointerUp(ev);

                break;
        }
        return isIntercept;
    }

    private void solvePointerUp(MotionEvent event) {
        // 获取离开屏幕的手指的索引
        int pointerIndexLeave = event.getActionIndex();
        int pointerIdLeave = event.getPointerId(pointerIndexLeave);
        if (mPointerId == pointerIdLeave) {
            // 离开屏幕的正是目前的有效手指，此处需要重新调整，并且需要重置VelocityTracker
            int reIndex = pointerIndexLeave == 0 ? 1 : 0;
            mPointerId = event.getPointerId(reIndex);
            // 调整触摸位置，防止出现跳动
            x = event.getX(reIndex);
            y = event.getY(reIndex);
            if (velocityTracker != null)
                velocityTracker.clear();
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int action = event.getActionMasked();

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 获取索引为0的手指id


                isMove = false;
                mPointerId = event.getPointerId(0);
                x = event.getX();
                y = event.getY();
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
                break;
            case MotionEvent.ACTION_MOVE:
                isMove = true;
                // 获取当前手指id所对应的索引，虽然在ACTION_DOWN的时候，我们默认选取索引为0
                // 的手指，但当有第二个手指触摸，并且先前有效的手指up之后，我们会调整有效手指

                // 屏幕上可能有多个手指，我们需要保证使用的是同一个手指的移动轨迹，
                // 因此此处不能使用event.getActionIndex()来获得索引
                final int pointerIndex = event.findPointerIndex(mPointerId);
                float mx = event.getX(pointerIndex);
                float my = event.getY(pointerIndex);

                moveBy((int) (x - mx), (int) (y - my));

                x = mx;
                y = my;
                break;
            case MotionEvent.ACTION_UP:
                isMove = false;
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                float velocityX = velocityTracker.getXVelocity(mPointerId);
                float velocityY = velocityTracker.getYVelocity(mPointerId);

                completeMove(-velocityX, -velocityY);
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // 获取离开屏幕的手指的索引
                isMove = false;
                int pointerIndexLeave = event.getActionIndex();
                int pointerIdLeave = event.getPointerId(pointerIndexLeave);
                if (mPointerId == pointerIdLeave) {
                    // 离开屏幕的正是目前的有效手指，此处需要重新调整，并且需要重置VelocityTracker
                    int reIndex = pointerIndexLeave == 0 ? 1 : 0;
                    mPointerId = event.getPointerId(reIndex);
                    // 调整触摸位置，防止出现跳动
                    x = event.getX(reIndex);
                    y = event.getY(reIndex);
                    if (velocityTracker != null)
                        velocityTracker.clear();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                isMove = false;
                break;
        }
        return true;
    }

    private Boolean isPull = false;

    //此处的moveBy是根据水平或是垂直排放的方向，
    //来选择是水平移动还是垂直移动
    public void moveBy(int deltaX, int deltaY) {
        if (Math.abs(deltaY) >= Math.abs(deltaX)) {
            int mScrollY = getScrollY();
            if (mScrollY <= 0) {
                scrollTo(0, 0);
            } else if (mScrollY >= getNScrollYButtom()) {
                scrollTo(0, getNScrollYButtom());


            } else {
                scrollBy(0, deltaY);
            }

        }


    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void completeMove(float velocityX, float velocityY) {

        int mScrollY = getScrollY();
        int maxY = getScrollYButtom();
        int minY = getScrollYTop();

        if (Math.abs(velocityY) >= minFlingVelocity && maxY > 0) {//大于1页的时候
            mScroller.fling(0, mScrollY, 0, (int) (velocityY * 2f), 0, 0, getNScrollYTop(), getNScrollYButtom());
            invalidate();
        }


    }


    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {

            scrollTo(0, mScroller.getCurrY());

            postInvalidate();

        }
    }


    public void onPullSuccess() {

        soomToBack();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void soomToBack() {
        int mScrollY = getScrollY();

    }

    private PullDownListem pullDownListem;

    public void setPullDownListem(PullDownListem pullDownListem) {
        this.pullDownListem = pullDownListem;
    }

    public interface PullDownListem {

        public void onPullDown();

    }
}
