package code.wangyuelin.com.simplecode;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.Scroller;


/**
 * 抽取出来的仅仅有滑动和惯性滑动的View
 */

public class CustomOnlyScrollView extends FrameLayout {
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;

    public CustomOnlyScrollView( Context context) {
        this(context, null);
    }

    public CustomOnlyScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomOnlyScrollView( Context context,  AttributeSet attrs,  int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        initVelocityTracker();
        mScroller = new Scroller(getContext());
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    int showHeight;
    private int mMaximumVelocity;
    private int mMinimumVelocity;
    private int mPointerId; // 触点ID
    private Scroller mScroller;
    private float x, y;
    protected float downX = 0, downY = 0;
    protected Boolean isMove = false;
    private boolean isStop;


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        showHeight = getMeasuredHeight();
    }

    private void initVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();//获得VelocityTracker类实例
        } else {
            mVelocityTracker.clear();
        }
    }


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
                Log.d("wt", " onInterceptTouchEvent 是否拦截：" + isIntercept);
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
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
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
            if (mVelocityTracker != null)
                mVelocityTracker.clear();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int action = event.getActionMasked();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // 获取索引为0的手指id
                isStop = false;


                isMove = false;
                mPointerId = event.getPointerId(0);
                x = event.getX();
                y = event.getY();
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();
                break;
            case MotionEvent.ACTION_MOVE:
                isStop = false;
                isMove = true;
                // 获取当前手指id所对应的索引，虽然在ACTION_DOWN的时候，我们默认选取索引为0
                // 的手指，但当有第二个手指触摸，并且先前有效的手指up之后，我们会调整有效手指

                // 屏幕上可能有多个手指，我们需要保证使用的是同一个手指的移动轨迹，
                // 因此此处不能使用event.getActionIndex()来获得索引
                final int pointerIndex = event.findPointerIndex(mPointerId);
                float mx = event.getX(pointerIndex);
                float my = event.getY(pointerIndex);
                Log.d("wt", " onTouchEvent 开始移动：" + (int) (y - my));

                moveBy((int) (x - mx), (int) (y - my));

                x = mx;
                y = my;
                break;
            case MotionEvent.ACTION_UP:
                isStop = false;
                isMove = false;
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                float velocityX = mVelocityTracker.getXVelocity(mPointerId);
                float velocityY = mVelocityTracker.getYVelocity(mPointerId);

                completeMove(-velocityX, -velocityY);
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
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
                    if (mVelocityTracker != null)
                        mVelocityTracker.clear();
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
                scrollBy(0, deltaY);

        }


    }

    @Override
    public void scrollTo(int x, int y) {
        int maxY = getMaxScrollY();
        if (y > maxY) {
            y = maxY;
        }
        if (y < 0) {
            y = 0;
        }
        super.scrollTo(x, y);

    }

    private void completeMove(float velocityX, float velocityY) {

        int mScrollY = getScrollY();
        int maxY = getMaxScrollY();

        if (Math.abs(velocityY) >= mMinimumVelocity && maxY > 0) {
            mScroller.fling(0, mScrollY, 0, (int) (velocityY * 2f), 0, 0, getMinScrollY(), getMaxScrollY());
            invalidate();
        }


    }


    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
    }

    @Override
    public void computeScroll() {
        if(isStop){
            return;
        }
        if (mScroller.computeScrollOffset()) {

            scrollTo(0, mScroller.getCurrY());

            postInvalidate();

        }
    }

    private int getMaxScrollY(){
        int size = getChildCount();
        int totalH = 0;
        for (int i = 0; i < size; i++){
            totalH += getChildAt(i).getMeasuredHeight();
        }

        return totalH  - showHeight;

    }

    private int getMinScrollY(){
        return 0;
    }


    public void stopScroll(){
        isStop = true;
    }
}

