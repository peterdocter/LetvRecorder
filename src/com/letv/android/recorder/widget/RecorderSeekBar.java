package com.letv.android.recorder.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.ArrayList;

import com.letv.android.recorder.R;
import com.letv.android.recorder.tool.RecordTool;

/**
 * Created by snile on 14-12-8.
 */
public class RecorderSeekBar extends View{


    public RecorderSeekBar(Context context) {
        this(context, null);
    }

    public RecorderSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecorderSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private OnSeekBarChangeListener changeListener;
    
    int progressBgColor = Color.parseColor("#2f2f33");
    int progressColor = Color.parseColor("#ff0000");
    int progressFlagColor = Color.parseColor("#b4b9bd");
    int progressHeight = 2*3;
    int progressHeightTouch = 4*3;
    boolean isTouch = false;
    int progressRealHeight = progressHeight;

    ShapeDrawable thumbDrawable;
    BitmapDrawable thumbTouchDrawable;
    Drawable flagDrawable;

    private int max = 1000;
    private int progress = 50;
    private ArrayList<Long> flags ;

    private Rect progressBgRect = new Rect();
    private Rect progressRect = new Rect();
    private Paint mPaint = new Paint();
    private Rect thumbRect = new Rect();
    private Rect thumbTouchRect = new Rect();

    private ValueAnimator animator ;
    int flagHeight ;
    int flagWidth=1*3 ;

    void init(){
        float [] outer = {thumbR,thumbR,thumbR,thumbR,thumbR,thumbR,thumbR,thumbR};
        thumbDrawable = new ShapeDrawable(new RoundRectShape(outer,null,null));

        thumbTouchDrawable = (BitmapDrawable)getContext().getResources().getDrawable(R.drawable.ic_seekbar_drag);
        animator = ValueAnimator.ofInt(progressHeight,progressHeightTouch);
        animator.addUpdateListener(updateListener);
        
        flagDrawable = getContext().getResources().getDrawable(R.drawable.play_flag);
//        flagHeight = flagDrawable.getIntrinsicHeight();
//        flagWidth = flagDrawable.getIntrinsicWidth();
    }

    private ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            progressRealHeight = (Integer)valueAnimator.getAnimatedValue();
            invalidate();
        }
    };

    int thumbR = 10;
    private void computeThumbRect(){
        int midThumbX = (int)(thumbR+(getWidth() - thumbR*2)*(1f*progress/max));
        thumbRect.set(midThumbX-thumbR,getHeight()/2 - thumbR,midThumbX+thumbR,getHeight()/2 + thumbR);
    }

    private void computeThumbTouchRect(){
        int midThumbX = (int)((getWidth())*(1f*progress/max));
        thumbTouchRect.set(midThumbX-thumbTouchDrawable.getIntrinsicWidth()/2,
                getHeight()/2+progressRealHeight/2,
                           midThumbX+thumbTouchDrawable.getIntrinsicWidth()/2,
                getHeight()/2+progressRealHeight/2+thumbTouchDrawable.getIntrinsicHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        progressBgRect.set(0, getHeight()/2-progressRealHeight/2, getWidth(), getHeight()/2+progressRealHeight/2);
        mPaint.setColor(progressBgColor);
        canvas.drawRect(progressBgRect,mPaint);
        progressRect.set(0,getHeight()/2-progressRealHeight/2,
                (int)((1f)*progress/max*getWidth()),getHeight()/2+progressRealHeight/2);
        mPaint.setColor(progressColor);
        canvas.drawRect(progressRect,mPaint);
        
        if(flags!=null&&flags.size()>0){
            canvas.save();
            Rect flagRect = new Rect();
            for(int i=0;i<flags.size();i++){
                int startX = (int)(flags.get(i)/(float)getMax()*getWidth());
                int endX = startX+flagWidth;
                int top = (getHeight()>>1)-(progressRealHeight>>1);
                int bottom = top+progressRealHeight;
                flagRect.set(startX,top,endX,bottom);
                flagDrawable.setBounds(flagRect);
                flagDrawable.draw(canvas);
            }
            canvas.restore();
        }
        if(isTouch){
            computeThumbTouchRect();
            thumbTouchDrawable.setBounds(thumbTouchRect);
            thumbTouchDrawable.draw(canvas);
        }else{
            computeThumbRect();
            thumbDrawable.setBounds(thumbRect);
            thumbDrawable.getPaint().setColor(progressColor);
            thumbDrawable.draw(canvas);
        }
        if(isTouch){
        	mPaint.setColor(Color.WHITE);
        	mPaint.setTextSize(48);
        	mPaint.setTextAlign(Align.CENTER);
        	String time = RecordTool.recordTimeFormat(progress);
        	Rect bounds = new Rect();
        	mPaint.getTextBounds(time, 0, time.length(), bounds);
        	int textX = (int)(1f*progress/max*getWidth()+0.5);
        	if(textX<bounds.width()/2+9){
        		textX = bounds.width()/2+9;
        	}else if(textX>getWidth()-bounds.width()/2-9){
        		textX = getWidth() -bounds.width()/2-9;
        	}
        	canvas.drawText(time,textX , getHeight()/2-15, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()&MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isTouch = false;
                if(animator.isRunning()){
                    animator.cancel();
                }
                animator = ValueAnimator.ofInt(progressRealHeight,progressHeight);
                animator.addUpdateListener(updateListener);
                animator.setDuration(150);
                animator.start();
                break;
            case MotionEvent.ACTION_DOWN:
                isTouch = true;
                if(animator.isRunning()) {
                    animator.cancel();
                }
                animator = ValueAnimator.ofInt(progressRealHeight,progressHeightTouch);
                animator.addUpdateListener(updateListener);
                animator.setDuration(150);
                animator.start();

                progress = (int) (event.getX()/getWidth()*max);
                changeListener.onProgressChanged(null, progress, true);
                break;
            case MotionEvent.ACTION_MOVE:
                isTouch = true;
                progress = (int) (event.getX()/getWidth()*max);
                changeListener.onProgressChanged(null, progress, true);
                invalidate();
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
		invalidate();
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
		invalidate();
	}

	public ArrayList<Long> getFlags() {
		return flags;
	}

	public void setFlags(ArrayList<Long> flags) {
		this.flags = flags;
		invalidate();
	}

	public void setOnSeekBarChangeListener(
			OnSeekBarChangeListener changeListener) {
		this.changeListener = changeListener;
	}
  
}
