package com.jaygoo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

import static com.jaygoo.widget.SeekBar.INDICATOR_MODE_ALWAYS_SHOW;


public class RangeSeekBar extends View {

    //normal seekBar mode
    public final static int SEEKBAR_MODE_SINGLE = 1;
    //RangeSeekBar
    public final static int SEEKBAR_MODE_RANGE = 2;
    //number according to the actual proportion of the number of arranged;
    public final static int TRICK_MARK_MODE_NUMBER = 0;
    //other equally arranged
    public final static int TRICK_MARK_MODE_OTHER = 1;
    //tick mark text gravity
    public final static int TRICK_MARK_GRAVITY_LEFT = 0;
    public final static int TRICK_MARK_GRAVITY_CENTER = 1;
    public final static int TRICK_MARK_GRAVITY_RIGHT = 2;

    private int seekBarMode;

    //Scale mode: number is arranged according to the actual scale of the number; other is evenly arranged
    private int tickMarkMode;

    //The default is 1, and when it is greater than 1,
    // it will automatically switch back to the scale mode
    private int tickMarkNumber = 1;

    //The spacing between the tick mark and the progress bar
    private int tickMarkTextMargin;

    //tick mark text and prompt text size
    private int tickMarkTextSize;

    private int tickMarkGravity;

    private int tickMarkTextColor;

    private int tickMarkInRangeTextColor;

    //The texts displayed on the scale
    private CharSequence[] tickMarkTextArray;

    //radius of progress bar
    private float progressRadius;

    //the color of seekBar in progress
    private int progressColor;

    //the default color of the progress bar
    private int progressDefaultColor;

    //the progress height
    private int progressHeight;

    //the range interval of RangeSeekBar
    private float rangeInterval;

    private int minRangeCells;

    //True values set by the user
    private float minProgress, maxProgress;

    //SKG: these variable will define min allowed value for thumb and also if its enabled or not.
    private float minAllowedValue;
    private int minAllowedEnabled;

    //****************** the above is attr value  ******************//

    // the progress width
    private int lineWidth;
    protected int lineTop, lineBottom, lineLeft, lineRight;
    private int linePaddingRight;
    private float touchDownX;
    private float cellsPercent;
    private float reservePercent;
    private boolean isEnable = true;
    private boolean isScaleThumb = false;
    private Paint paint = new Paint();
    private RectF backgroundLineRect = new RectF();
    private RectF foregroundLineRect = new RectF();
    private SeekBar leftSB;
    private SeekBar rightSB;
    private SeekBar currTouchSB;
    private OnRangeChangedListener callback;

    private Paint movingTextPaint = new Paint();

    /**
     * Explore by touch helper, used to expose contents for accessibility.
     */
    private SeekBarAccessHelper mSeekBarAccessHelper;

    public RangeSeekBar(Context context) {
        this(context, null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        initPaint();

        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            leftSB = new SeekBar(this, attrs, true);
            rightSB = new SeekBar(this, attrs, false);
        } else {
            leftSB = new SeekBar(this, attrs, true);
            rightSB = null;
        }

        setRange(minProgress, maxProgress, rangeInterval, tickMarkNumber);

        initProgressLine();

        // Set up accessibility helper class.
        mSeekBarAccessHelper = new SeekBarAccessHelper(this);
        ViewCompat.setAccessibilityDelegate(this, mSeekBarAccessHelper);
    }

    private void initProgressLine() {
        //Android 7.0以后，优化了View的绘制，onMeasure和onSizeChanged调用顺序有所变化
        //Android7.0以下：onMeasure--->onSizeChanged--->onMeasure
        //Android7.0以上：onMeasure--->onSizeChanged
        if (rightSB == null){
            lineTop = (int) (leftSB.getIndicatorHeight() + leftSB.getIndicatorArrowSize() + leftSB.getThumbSize() * leftSB.getThumbScaleRatio() / 2 - progressHeight / 2);
        }else {
            lineTop = (int) (Math.max(leftSB.getIndicatorHeight() + leftSB.getIndicatorArrowSize() + leftSB.getThumbSize() * leftSB.getThumbScaleRatio() / 2, rightSB.getIndicatorHeight() + rightSB.getIndicatorArrowSize() + rightSB.getThumbSize() / 2) - progressHeight / 2);
        }
        lineBottom = lineTop + progressHeight;
        //default value
        if (progressRadius < 0){
            progressRadius = (int) ((getLineBottom() - getLineTop()) * 0.45f);
        }
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);
        seekBarMode = t.getInt(R.styleable.RangeSeekBar_rsb_mode, SEEKBAR_MODE_RANGE);
        minProgress = t.getFloat(R.styleable.RangeSeekBar_rsb_min, 0);
        maxProgress = t.getFloat(R.styleable.RangeSeekBar_rsb_max, 100);
        minAllowedEnabled = t.getInt(R.styleable.RangeSeekBar_rsb_min_allowed_enabled, SEEKBAR_MODE_RANGE);
        minAllowedValue = t.getFloat(R.styleable.RangeSeekBar_rsb_min_allowed, 0);
        rangeInterval = t.getFloat(R.styleable.RangeSeekBar_rsb_range_interval, 0);
        progressColor = t.getColor(R.styleable.RangeSeekBar_rsb_progress_color, 0xFF4BD962);
        progressRadius = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_progress_radius, -1);
        progressDefaultColor = t.getColor(R.styleable.RangeSeekBar_rsb_progress_default_color, 0xFFD7D7D7);
        progressHeight = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_progress_height, Utils.dp2px(getContext(),2));
        tickMarkMode = t.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_mode, TRICK_MARK_MODE_NUMBER);
        tickMarkGravity = t.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_gravity, TRICK_MARK_GRAVITY_CENTER);
        tickMarkNumber = t.getInt(R.styleable.RangeSeekBar_rsb_tick_mark_number, 1);
        tickMarkTextArray = t.getTextArray(R.styleable.RangeSeekBar_rsb_tick_mark_text_array);
        tickMarkTextMargin = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_tick_mark_text_margin, Utils.dp2px(getContext(),7));
        tickMarkTextSize = (int) t.getDimension(R.styleable.RangeSeekBar_rsb_tick_mark_text_size, Utils.dp2px(getContext(),12));
        tickMarkTextColor = t.getColor(R.styleable.RangeSeekBar_rsb_tick_mark_text_color, progressDefaultColor);
        tickMarkInRangeTextColor = t.getColor(R.styleable.RangeSeekBar_rsb_tick_mark_text_color, progressColor);
        t.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightNeeded = 2 * getLineTop() + progressHeight;

        /*
         * onMeasure传入的widthMeasureSpec和heightMeasureSpec不是一般的尺寸数值，而是将模式和尺寸组合在一起的数值
         * MeasureSpec.EXACTLY 是精确尺寸
         * MeasureSpec.AT_MOST 是最大尺寸
         * MeasureSpec.UNSPECIFIED 是未指定尺寸
         */
        if (heightMode == MeasureSpec.EXACTLY) {
            heightSize = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            heightSize = MeasureSpec.makeMeasureSpec(heightNeeded, MeasureSpec.AT_MOST);
        } else {
            heightSize = MeasureSpec.makeMeasureSpec(
                    heightNeeded, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //计算进度条的位置，并根据它初始化两个按钮的位置
        // Calculates the position of the progress bar and initializes the positions of
        // the two buttons based on it
        lineLeft = leftSB.getThumbSize() / 2 + getPaddingLeft() ;
        lineRight = w - lineLeft - getPaddingRight();
        lineWidth = lineRight - lineLeft;
        linePaddingRight = w - lineRight;
        backgroundLineRect.set(getLineLeft(), getLineTop(), getLineRight(), getLineBottom());
        leftSB.onSizeChanged(getLineLeft(), getLineBottom(), lineWidth);
        if (rightSB != null) {
            rightSB.onSizeChanged(getLineLeft(), getLineBottom(), lineWidth);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制刻度，并且根据当前位置是否在刻度范围内设置不同的颜色显示
        // Draw the scales, and according to the current position is set within
        // the scale range of different color display
        if (tickMarkTextArray != null) {
            int trickPartWidth = lineWidth / (tickMarkTextArray.length - 1);
            for (int i = 0; i < tickMarkTextArray.length; i++) {
                final String text2Draw = tickMarkTextArray[i].toString();
                float x;
                paint.setColor(tickMarkTextColor);
                //平分显示
                if (tickMarkMode == TRICK_MARK_MODE_OTHER) {
                    if (tickMarkGravity == TRICK_MARK_GRAVITY_RIGHT){
                        x = getLineLeft() + i * trickPartWidth - paint.measureText(text2Draw);
                    }else if (tickMarkGravity == TRICK_MARK_GRAVITY_CENTER){
                        x = getLineLeft() + i * trickPartWidth - paint.measureText(text2Draw) / 2;
                    }else {
                        x = getLineLeft() + i * trickPartWidth;
                    }
                } else {
                    float num = Float.parseFloat(text2Draw);
                    SeekBarState[] states = getRangeSeekBarState();
                    if (Utils.compareFloat(num, states[0].value) != -1 && Utils.compareFloat(num, states[1].value) != 1 && seekBarMode == SEEKBAR_MODE_RANGE) {
                        paint.setColor(tickMarkInRangeTextColor);
                    }
                    //按实际比例显示
                    x = getLineLeft() + lineWidth * (num - minProgress) / (maxProgress - minProgress)
                            - paint.measureText(text2Draw) / 2;
                }
                float y = getLineTop() - tickMarkTextMargin;
                canvas.drawText(text2Draw, x, y, paint);
            }
        }

        //绘制进度条
        // draw the progress bar
        paint.setColor(progressDefaultColor);
        canvas.drawRoundRect(backgroundLineRect, progressRadius, progressRadius, paint);
        paint.setColor(progressColor);
        if (seekBarMode == SEEKBAR_MODE_RANGE) {
            foregroundLineRect.top = getLineTop();
            foregroundLineRect.left = leftSB.left + leftSB.getThumbSize() / 2 + lineWidth * leftSB.currPercent;
            foregroundLineRect.right = rightSB.left + rightSB.getThumbSize() / 2 + lineWidth * rightSB.currPercent;
            foregroundLineRect.bottom = getLineBottom();
            canvas.drawRoundRect(foregroundLineRect, progressRadius, progressRadius, paint);
        } else {
            foregroundLineRect.top = getLineTop();
            foregroundLineRect.left = leftSB.left + leftSB.getThumbSize() / 2;
            foregroundLineRect.right = leftSB.left + leftSB.getThumbSize() / 2 + lineWidth * leftSB.currPercent;
            foregroundLineRect.bottom = getLineBottom();
            canvas.drawRoundRect(foregroundLineRect, progressRadius, progressRadius, paint);
        }

        //SKG: Draw Text Blocked between the two thumbs
        if (leftSB != null && rightSB != null) {
            float a = leftSB.left + leftSB.getThumbSize() / 2 + lineWidth * leftSB.currPercent;
            float b = rightSB.left + rightSB.getThumbSize() / 2 + lineWidth * rightSB.currPercent;
            float middle = (a + b) / 2;
            final String text2Draw = "Blocked";
            movingTextPaint.setColor(leftSB.getIndicatorTextColor());
            float x = 0;

            if (tickMarkMode == TRICK_MARK_MODE_OTHER) {
                if (tickMarkGravity == TRICK_MARK_GRAVITY_RIGHT) {
                    x = middle - movingTextPaint.measureText(text2Draw);
                } else if (tickMarkGravity == TRICK_MARK_GRAVITY_CENTER) {
                    x = middle - movingTextPaint.measureText(text2Draw) / 2;
                } else {
                    x = middle;
                }
            }

            //SKG: Dont draw on edge or else text will be chopped off
            if (x < 0) {
                //Draw text at start with 5 offset
                x = 0;
            } else if (x + (movingTextPaint.measureText(text2Draw) / 2) > lineWidth) {
                x = lineWidth - (movingTextPaint.measureText(text2Draw) / 2);
            }

            float y = getLineTop() + tickMarkTextMargin;
            canvas.drawText(text2Draw, x, y, movingTextPaint);

        }

        //SKG: avoid thumb text overlap,
        //If distance between two thumbs is less than the size of the left thumb text + right thumb text, that means its overlapping.
        boolean addOffsetToLeft = true;
        float thumbTextOffset = 0;
        if (leftSB != null && rightSB != null) {
            float a = leftSB.left + leftSB.getThumbSize() / 2 + lineWidth * leftSB.currPercent;
            float b = rightSB.left + rightSB.getThumbSize() / 2 + lineWidth * rightSB.currPercent;
            float diff = b - a;
            float offset = 130;
            if (diff < (offset / 2)) {
                thumbTextOffset = offset + 10;
            } else if (diff < offset) {
                thumbTextOffset = (offset / 2) + 10;
            }
            //This will determine if we want to add offset to left or right thumb.
            if (b > lineWidth - (offset/2)) {
                addOffsetToLeft = true;
            } else if (a < offset) {
                addOffsetToLeft = false;
            }
        }

        //draw left SeekBar
        if (leftSB.getIndicatorShowMode() == INDICATOR_MODE_ALWAYS_SHOW) {
            leftSB.setShowIndicatorEnable(true);
        }
        if (addOffsetToLeft) {
            leftSB.draw(canvas, thumbTextOffset);
        } else {
            leftSB.draw(canvas, 0);
        }

        //draw right SeekBar
        if (rightSB != null) {
            if (rightSB.getIndicatorShowMode() == INDICATOR_MODE_ALWAYS_SHOW) {
                rightSB.setShowIndicatorEnable(true);
            }
            if (!addOffsetToLeft) {
                rightSB.draw(canvas, -thumbTextOffset);
            } else {
                rightSB.draw(canvas, 0);
            }
        }
    }

    private void initPaint() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(progressDefaultColor);
        paint.setTextSize(tickMarkTextSize);

        //SKG: this paint is used for drawing word "blocked" which is also a moving text and centers between two thumb.
        movingTextPaint.setStyle(Paint.Style.FILL);
        movingTextPaint.setTextSize(Utils.dp2px(getContext(),14));
    }

    public void setValue(float value) {
        setValue(value, maxProgress);
    }

    public void setValue(float leftValue, float rightValue) {
        leftValue = Math.min(leftValue, rightValue);
        rightValue = Math.max(leftValue, rightValue);
        if (rightValue - leftValue < rangeInterval){
            leftValue = rightValue - rangeInterval;
        }
        if (leftValue < minProgress) {
            throw new IllegalArgumentException("setValue() min < (preset min - offsetValue) . #min:" + leftValue + " #preset min:" + rightValue );
        }
        if (rightValue > maxProgress) {
            throw new IllegalArgumentException("setValue() max > (preset max - offsetValue) . #max:" + rightValue + " #preset max:"  + rightValue );
        }

        float range = maxProgress - minProgress;

        if (tickMarkNumber > 1) {
            //SKG: we dont need this since we have range of 6 and tickmark 12.
//            int percent = (int)(range / tickMarkNumber);
//            if ((int)Math.abs(leftValue - minProgress) % percent != 0 || (int)Math.abs(rightValue - minProgress) % percent != 0 ){
//                throw new IllegalArgumentException("The current value must be at the equal point");
//            }
            leftSB.currPercent = Math.abs(leftValue - minProgress) / range;
            if (rightSB != null) {
                rightSB.currPercent = Math.abs(rightValue - minProgress) / range;
            }
        } else {
            leftSB.currPercent = Math.abs(leftValue - minProgress) / range;
            if (rightSB != null) {
                rightSB.currPercent = Math.abs(rightValue - minProgress) / range;
            }
        }

        if (callback != null) {
            callback.onRangeChanged(this, leftValue, rightValue, false);
        }
        invalidate();
    }


    /**
     * 设置范围
     * @param min 最小值
     * @param max 最大值
     */
    public void setRange(float min, float max) {
        setRange(min, max, rangeInterval, tickMarkNumber);
    }

    /**
     * 设置范围
     * @param min 最小值
     * @param max 最大值
     * @param interval 最小间隔 minimum interval
     */
    public void setRange(float min, float max, float interval) {
        setRange(min, max, interval, tickMarkNumber);
    }

    /**
     * 设置范围
     * @param min 最小值
     * @param max 最大值
     * @param interval 最小间隔
     * @param tickMarkNumber  默认为1，当大于1时自动切回刻度模式
     * The default is 1, and when it is greater than 1
     * it will automatically switch back to the scale mode
     */
    public void setRange(float min, float max, float interval, int tickMarkNumber) {
        if (max <= min) {
            throw new IllegalArgumentException("setRange() max must be greater than min ! #max:" + max + " #min:" + min);
        }
        if (interval < 0) {
            throw new IllegalArgumentException("setRange() interval must be greater than zero ! #interval:" + interval);
        }
        if (interval >= max - min) {
            throw new IllegalArgumentException("setRange() interval must be less than (max - min) ! #interval:" + interval + " #max - min:" + (max - min));
        }
        if (tickMarkNumber < 1) {
            throw new IllegalArgumentException("setRange() tickMarkNumber must be greater than 1 ! #tickMarkNumber:" + tickMarkNumber);
        }
        maxProgress = max;
        minProgress = min;
        this.tickMarkNumber = tickMarkNumber;
        cellsPercent = 1f / tickMarkNumber;
        rangeInterval = interval;
        reservePercent = interval / (max - min);
        minRangeCells = (int) (reservePercent / cellsPercent + (reservePercent % cellsPercent != 0 ? 1 : 0));

        if (tickMarkNumber > 1) {
            if (rightSB != null) {
                if (leftSB.currPercent + cellsPercent * minRangeCells  <= 1 && leftSB.currPercent + cellsPercent * minRangeCells  > rightSB.currPercent) {
                    rightSB.currPercent = leftSB.currPercent + cellsPercent * minRangeCells ;
                } else if (rightSB.currPercent - cellsPercent * minRangeCells  >= 0 && rightSB.currPercent - cellsPercent * minRangeCells  < leftSB.currPercent) {
                    leftSB.currPercent = rightSB.currPercent - cellsPercent * minRangeCells ;
                }
            } else {
                if (1 - cellsPercent * minRangeCells  >= 0 && 1 - cellsPercent * minRangeCells  < leftSB.currPercent) {
                    leftSB.currPercent = 1 - cellsPercent * minRangeCells;
                }
            }
        } else {
            if (rightSB != null) {
                if (leftSB.currPercent + reservePercent <= 1 && leftSB.currPercent + reservePercent > rightSB.currPercent) {
                    rightSB.currPercent = leftSB.currPercent + reservePercent;
                } else if (rightSB.currPercent - reservePercent >= 0 && rightSB.currPercent - reservePercent < leftSB.currPercent) {
                    leftSB.currPercent = rightSB.currPercent - reservePercent;
                }
            } else {
                if (1 - reservePercent >= 0 && 1 - reservePercent < leftSB.currPercent) {
                    leftSB.currPercent = 1 - reservePercent;
                }
            }
        }
        invalidate();
    }

    /**
     * @return the two seekBar state , see {@link SeekBarState}
     */
    public SeekBarState[] getRangeSeekBarState() {
        float range = maxProgress - minProgress;
        SeekBarState leftSeekBarState = new SeekBarState();
        leftSeekBarState.value = minProgress + range * leftSB.currPercent;
        if (tickMarkNumber > 1){
            int index = (int)Math.floor(leftSB.currPercent * tickMarkNumber);
            if (tickMarkTextArray != null && index >= 0 && index < tickMarkTextArray.length) {
                leftSeekBarState.indicatorText = tickMarkTextArray[index].toString();
            }
            if (index == 0){
                leftSeekBarState.isMin = true;
            }else if (index == tickMarkNumber){
                leftSeekBarState.isMax = true;
            }

        }else {
            leftSeekBarState.indicatorText = (new StringBuffer().append(leftSeekBarState.value)).toString();
            if (Utils.compareFloat(leftSB.currPercent, 0f) == 0){
                leftSeekBarState.isMin = true;
            }else if (Utils.compareFloat(leftSB.currPercent, 1f) == 0){
                leftSeekBarState.isMax = true;
            }
        }

        SeekBarState rightSeekBarState = new SeekBarState();
        if (rightSB != null) {
            rightSeekBarState.value = minProgress + range * rightSB.currPercent;
            if (tickMarkNumber > 1){
                int index = (int)Math.floor(rightSB.currPercent * tickMarkNumber);
                if (tickMarkTextArray != null && index >= 0 && index < tickMarkTextArray.length) {
                    rightSeekBarState.indicatorText = tickMarkTextArray[index].toString();
                }
                if (index == 0){
                    rightSeekBarState.isMin = true;
                }else if (index == tickMarkNumber){
                    rightSeekBarState.isMax = true;
                }
            }else {
                rightSeekBarState.indicatorText = (new StringBuffer().append(rightSeekBarState.value)).toString();
                if (Utils.compareFloat(rightSB.currPercent, 0f) == 0){
                    rightSeekBarState.isMin = true;
                }else if (Utils.compareFloat(rightSB.currPercent, 1f) == 0){
                    rightSeekBarState.isMax = true;
                }
            }
        }

        return new SeekBarState[]{leftSeekBarState, rightSeekBarState};
    }


    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.isEnable = enabled;
    }

    public void setIndicatorText(String progress) {
        if (leftSB != null) {
            leftSB.setIndicatorText(progress);
        }
        if (rightSB != null) {
            rightSB.setIndicatorText(progress);
        }
    }

    /**
     * format number indicator text
     * @param formatPattern format rules
     */
    public void setIndicatorTextDecimalFormat(String formatPattern){
        if (leftSB != null){
            leftSB.setIndicatorTextDecimalFormat(formatPattern);
        }
        if (rightSB != null){
            rightSB.setIndicatorTextDecimalFormat(formatPattern);
        }
    }

    /**
     * format string indicator text
     * @param formatPattern format rules
     */
    public void setIndicatorTextStringFormat(String formatPattern){
        if (leftSB != null){
            leftSB.setIndicatorTextStringFormat(formatPattern);
        }
        if (rightSB != null){
            rightSB.setIndicatorTextStringFormat(formatPattern);
        }
    }

    private void changeThumbActivateState(boolean hasActivate){
        if (hasActivate && currTouchSB != null){
            boolean state = currTouchSB == leftSB;
            if (leftSB != null) leftSB.setActivate(state);
            if (rightSB != null) rightSB.setActivate(!state);
        }else {
            if (leftSB != null) leftSB.setActivate(false);
            if (rightSB != null) rightSB.setActivate(false);
        }
    }

    protected float getEventX(MotionEvent event){
        return event.getX();
    }

    protected float getEventY(MotionEvent event){
        return event.getY();
    }

    /**
     * scale the touch seekBar thumb
     */
    private void scaleCurrentSeekBarThumb(){
        if (currTouchSB != null && currTouchSB.getThumbScaleRatio() > 1f && !isScaleThumb) {
            isScaleThumb = true;
            currTouchSB.setThumbSize((int) (currTouchSB.getThumbSize() * currTouchSB.getThumbScaleRatio()));
            currTouchSB.onSizeChanged(getLineLeft(), getLineBottom(), lineWidth);
        }
    }

    /**
     * reset the touch seekBar thumb
     */
    private void resetCurrentSeekBarThumb(){
        if (currTouchSB != null && currTouchSB.getThumbScaleRatio() > 1f && isScaleThumb) {
            isScaleThumb = false;
            currTouchSB.setThumbSize((int) (currTouchSB.getThumbSize() / currTouchSB.getThumbScaleRatio()));
            currTouchSB.onSizeChanged(getLineLeft(), getLineBottom(), lineWidth);

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnable) return true;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = getEventX(event);
                boolean touchResult = false;
                if (rightSB != null && rightSB.currPercent >= 1 && leftSB.collide(getEventX(event), getEventY(event))) {
                    currTouchSB = leftSB;
                    touchResult = true;
                    scaleCurrentSeekBarThumb();
                } else if (rightSB != null && rightSB.collide(getEventX(event), getEventY(event))) {
                    currTouchSB = rightSB;
                    touchResult = true;
                    scaleCurrentSeekBarThumb();
                } else if (leftSB.collide(getEventX(event), getEventY(event))) {
                    currTouchSB = leftSB;
                    touchResult = true;
                    scaleCurrentSeekBarThumb();
                }
                //Intercept parent TouchEvent
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (callback != null) {
                    callback.onStartTrackingTouch(this, currTouchSB == leftSB);
                }
                changeThumbActivateState(true);
                return touchResult;
            case MotionEvent.ACTION_MOVE:
                float percent;
                float x = getEventX(event);
                if (rightSB != null && leftSB.currPercent == rightSB.currPercent) {
                    currTouchSB.materialRestore();
                    if (callback != null) {
                        callback.onStopTrackingTouch(this, currTouchSB == leftSB);
                    }
                    if (x - touchDownX > 0) {
                        //method to move right
                        if (currTouchSB != rightSB) {
                            resetCurrentSeekBarThumb();
                            currTouchSB = rightSB;
                            scaleCurrentSeekBarThumb();
                        }else {
                            currTouchSB = rightSB;
                        }
                    } else {
                        //method to move left
                        if (currTouchSB != leftSB) {
                            resetCurrentSeekBarThumb();
                            currTouchSB = leftSB;
                            scaleCurrentSeekBarThumb();
                        }else {
                            currTouchSB = leftSB;
                        }
                    }
                    if (callback != null) {
                        callback.onStartTrackingTouch(this, currTouchSB == leftSB);
                    }
                }
                touchDownX = x;
                currTouchSB.material = currTouchSB.material >= 1 ? 1 : currTouchSB.material + 0.1f;
                if (currTouchSB == leftSB) {
                    if (tickMarkNumber > 1) {
                        if (x < getLineLeft()) {
                            percent = 0;
                        } else {
                            percent = (x - getLineLeft()) * 1f / (lineWidth);
                        }
                        int touchLeftCellsValue = Math.round(percent / cellsPercent);
                        int currRightCellsValue;
                        if (rightSB != null) {
                            currRightCellsValue = Math.round(rightSB.currPercent / cellsPercent);
                        } else {
                            currRightCellsValue = Math.round(1.0f / cellsPercent);
                        }
                        percent = touchLeftCellsValue * cellsPercent;
                        while (touchLeftCellsValue > currRightCellsValue - minRangeCells) {
                            touchLeftCellsValue--;
                            if (touchLeftCellsValue < 0) break;
                            percent = touchLeftCellsValue * cellsPercent;
                        }
                    } else {
                        if (x < getLineLeft()) {
                            percent = 0;
                        } else {
                            percent = (x - getLineLeft()) * 1f / (lineWidth);
                        }
                        if (rightSB != null) {
                            if (percent > rightSB.currPercent - reservePercent) {
                                percent = rightSB.currPercent - reservePercent;
                            }
                        } else {
                            if (percent > 1.0f - reservePercent) {
                                percent = 1.0f - reservePercent;
                            }
                        }
                    }
                    //SKG: since we have to not letting the thumb go less than 30 mins, which is 1 cell in terms of rangeseekbar,
                    //we should ignore move less than cellsPercent * minAllowedValue.
                    //Making it dynamic so if in future business say min should be an hour then we just change in XML minAllowedValue to 2 and done.
                    if (minAllowedEnabled == SEEKBAR_MODE_RANGE || percent >= cellsPercent * minAllowedValue) {
                        leftSB.slide(percent);
                    }
                    leftSB.setShowIndicatorEnable(true);
                    //Intercept parent TouchEvent
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                } else if (currTouchSB == rightSB) {
                    if (tickMarkNumber > 1) {
                        if (x > getLineRight()) {
                            percent = 1;
                        } else {
                            percent = (x - getLineLeft()) * 1f / (lineWidth);
                        }
                        int touchRightCellsValue = Math.round(percent / cellsPercent);
                        int currLeftCellsValue = Math.round(leftSB.currPercent / cellsPercent);
                        percent = touchRightCellsValue * cellsPercent;

                        while (touchRightCellsValue < currLeftCellsValue + minRangeCells) {
                            touchRightCellsValue++;
                            if (touchRightCellsValue > (maxProgress - minProgress)) break;
                            percent = touchRightCellsValue * cellsPercent;
                        }
                    } else {
                        if (x > getLineRight()) {
                            percent = 1;
                        } else {
                            percent = (x - getLineLeft()) * 1f / (lineWidth);
                        }
                        if (percent < leftSB.currPercent + reservePercent) {
                            percent = leftSB.currPercent + reservePercent;
                        }
                    }
                    rightSB.slide(percent);
                    rightSB.setShowIndicatorEnable(true);
                }
                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, true);
                }
                invalidate();
                //Intercept parent TouchEvent
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                changeThumbActivateState(true);
                break;
            case MotionEvent.ACTION_CANCEL:
                if (rightSB != null) {
                    rightSB.setShowIndicatorEnable(false);
                }
                if (currTouchSB == leftSB){
                    resetCurrentSeekBarThumb();
                }else if (currTouchSB == rightSB){
                    resetCurrentSeekBarThumb();
                }
                leftSB.setShowIndicatorEnable(false);
                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, false);
                }
                //Intercept parent TouchEvent
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                changeThumbActivateState(false);
                break;
            case MotionEvent.ACTION_UP:
                if (rightSB != null) {
                    rightSB.setShowIndicatorEnable(false);
                }
                leftSB.setShowIndicatorEnable(false);
                currTouchSB.materialRestore();
                resetCurrentSeekBarThumb();
                if (callback != null) {
                    SeekBarState[] states = getRangeSeekBarState();
                    callback.onRangeChanged(this, states[0].value, states[1].value, false);
                }
                //Intercept parent TouchEvent
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (callback != null) {
                    callback.onStopTrackingTouch(this, currTouchSB == leftSB);
                }
                changeThumbActivateState(false);
                break;
        }
        mSeekBarAccessHelper.invalidateVirtualViewId(1);
        //   mSeekBarAccessHelper.getEventForVirtualViewId(1, event.getAction());
        return super.onTouchEvent(event);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.minValue = minProgress;
        ss.maxValue = maxProgress;
        ss.rangeInterval = rangeInterval;
        ss.tickNumber = tickMarkNumber;
        SeekBarState[] results = getRangeSeekBarState();
        ss.currSelectedMin = results[0].value;
        ss.currSelectedMax = results[1].value;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        float min = ss.minValue;
        float max = ss.maxValue;
        float rangeInterval = ss.rangeInterval;
        int tickNumber = ss.tickNumber;
        setRange(min, max, rangeInterval, tickNumber);
        float currSelectedMin = ss.currSelectedMin;
        float currSelectedMax = ss.currSelectedMax;
        setValue(currSelectedMin, currSelectedMax);
    }

    public void setOnRangeChangedListener(OnRangeChangedListener listener) {
        callback = listener;
    }

    /**
     * if is single mode, please use it to get the SeekBar
     * @return left seek bar
     */
    public SeekBar getLeftSeekBar(){
        return leftSB;
    }

    public SeekBar getRightSeekBar(){
        return rightSB;
    }

    public int getLineTop() {
        return lineTop;
    }

    public void setLineTop(int lineTop) {
        this.lineTop = lineTop;
    }

    public int getLineBottom() {
        return lineBottom;
    }

    public void setLineBottom(int lineBottom) {
        this.lineBottom = lineBottom;
    }

    public int getLineLeft() {
        return lineLeft;
    }

    public void setLineLeft(int lineLeft) {
        this.lineLeft = lineLeft;
    }

    public int getLineRight() {
        return lineRight;
    }

    public void setLineRight(int lineRight) {
        this.lineRight = lineRight;
    }

    public int getLinePaddingRight() {
        return linePaddingRight;
    }

    public int getProgressHeight() {
        return progressHeight;
    }

    public void setProgressHeight(int progressHeight) {
        this.progressHeight = progressHeight;
    }

    public float getMinProgress() {
        return minProgress;
    }

    public float getMaxProgress() {
        return maxProgress;
    }

    public void setProgressColor(int progressDefaultColor, int progressColor) {
        this.progressDefaultColor = progressDefaultColor;
        this.progressColor = progressColor;
    }

    public int getTickMarkTextColor() {
        return tickMarkTextColor;
    }

    public void setTickMarkTextColor(int tickMarkTextColor) {
        this.tickMarkTextColor = tickMarkTextColor;
    }

    public int getTickMarkInRangeTextColor() {
        return tickMarkInRangeTextColor;
    }

    public void setTickMarkInRangeTextColor(int tickMarkInRangeTextColor) {
        this.tickMarkInRangeTextColor = tickMarkInRangeTextColor;
    }

    public int getSeekBarMode() {
        return seekBarMode;
    }

    public int getTickMarkMode() {
        return tickMarkMode;
    }

    public int getTickMarkNumber() {
        return tickMarkNumber;
    }

    public int getTickMarkTextMargin() {
        return tickMarkTextMargin;
    }

    public void setTickMarkTextMargin(int tickMarkTextMargin) {
        this.tickMarkTextMargin = tickMarkTextMargin;
    }

    public int getTickMarkTextSize() {
        return tickMarkTextSize;
    }

    public void setTickMarkTextSize(int tickMarkTextSize) {
        this.tickMarkTextSize = tickMarkTextSize;
    }

    public int getTickMarkGravity() {
        return tickMarkGravity;
    }

    public void setTickMarkGravity(int tickMarkGravity) {
        this.tickMarkGravity = tickMarkGravity;
    }

    public CharSequence[] getTickMarkTextArray() {
        return tickMarkTextArray;
    }

    public void setTickMarkTextArray(CharSequence[] tickMarkTextArray) {
        this.tickMarkTextArray = tickMarkTextArray;
    }

    public float getRangeInterval() {
        return rangeInterval;
    }

    public void setRangeInterval(float rangeInterval) {
        this.rangeInterval = rangeInterval;
    }

    public float getProgressRadius() {
        return progressRadius;
    }

    public void setProgressRadius(float progressRadius) {
        this.progressRadius = progressRadius;
    }

    public int getProgressColor() {
        return progressColor;
    }

    public void setProgressColor(int progressColor) {
        this.progressColor = progressColor;
    }

    public int getProgressDefaultColor() {
        return progressDefaultColor;
    }

    public void setProgressDefaultColor(int progressDefaultColor) {
        this.progressDefaultColor = progressDefaultColor;
    }

    public void setTickMarkNumber(int tickMarkNumber) {
        this.tickMarkNumber = tickMarkNumber;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public void setTickMarkMode(int tickMarkMode) {
        this.tickMarkMode = tickMarkMode;
    }

    public void setSeekBarMode(int seekBarMode) {
        this.seekBarMode = seekBarMode;
    }

    public void setTypeface(Typeface typeFace){
        paint.setTypeface(typeFace);
    }


    /**
     * Implementation of {@link ExploreByTouchHelper} that exposes the contents
     * of a to accessibility services by mapping each bar
     * to a virtual view.
     */
    private class SeekBarAccessHelper extends ExploreByTouchHelper {
        public SeekBarAccessHelper(View parentView) {
            super(parentView);
        }

        @Override
        protected int getVirtualViewIdAt(float x, float y) {
            // We already map (x,y) to bar index for onTouchEvent().

            if(leftSB.collide(x,y)){
                return  1;
            } if (rightSB != null &&rightSB.collide(x,y)){
                return 2;
            }

            return ExploreByTouchHelper.INVALID_ID;
        }

        @Override
        protected void getVisibleVirtualViewIds(List<Integer> virtualViewIds) {
            virtualViewIds.add(1);
            if(rightSB !=null){
                virtualViewIds.add(2);
            }
        }

        private String getDescriptionForIndex(int index) {
            String description = "thumb";
            try {
                if(index == 1){
                    description =  "left" + description + leftSB.getThumbValue();
                    if(!isEnable){
                        description = description + "disabled";
                    }
                }
                else if (rightSB!=null && index == 2){
                    description = "right" + description + rightSB.getThumbValue();
                    if(!isEnable){
                        description = description + "disabled";
                    }
                }
            } catch (Exception e) {
                //do nothing
            }
            return description;
        }

        @Override
        protected void populateEventForVirtualViewId(int virtualViewId, AccessibilityEvent event) {
            final String desc = getDescriptionForIndex(virtualViewId);
            event.setContentDescription(desc);
        }

        @Override
        protected void populateNodeForVirtualViewId(
                int virtualViewId, AccessibilityNodeInfoCompat node) {
            // Node and event descriptions are usually identical.
            final String desc = getDescriptionForIndex(virtualViewId);
            node.setContentDescription(desc);

            //SKG: add actions only if enabled.
            if(isEnable) {
                // Since the user can tap a bar, add the CLICK action.
                node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
                node.addAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
                node.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
                node.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
            }

            Rect bounds = null;
            if(virtualViewId == 1){
                bounds = leftSB.getThumbBounds();
            }
            else if (rightSB!=null && virtualViewId == 2){
                bounds = rightSB.getThumbBounds();
            }
            // Reported bounds should be consistent with onDraw().
            node.setBoundsInParent(bounds);
        }

        @Override
        protected boolean performActionForVirtualViewId(int virtualViewId, int action) {
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_CLICK:
                    // Click handling should be consistent with onTouchEvent().
                    // onBarClicked(virtualViewId);
                    return true;
            }
            // Only need to handle actions added in populateNode.
            return false;
        }
    }



}