package com.letv.android.recorder.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

import com.letv.android.recorder.R;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.leui.util.LeReflectionUtils;

import java.util.ArrayList;

/**
 * Created by snile on 14-9-11.
 */
public class FlagSeekBar extends SeekBar{

    private ArrayList<Long> flags;

    public FlagSeekBar(Context context) {
        super(context);
        init();
    }

    public FlagSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FlagSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }


    private void init(){
        flagDrawable = getContext().getResources().getDrawable(R.drawable.play_flag);
        flagHeight = flagDrawable.getIntrinsicHeight();
        flagWidth = flagDrawable.getIntrinsicWidth();
    }

    Drawable flagDrawable;

    int flagHeight ;
    int flagWidth ;

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Drawable thumb = getThumb();
        if(flags!=null&&flags.size()>0){
            canvas.save();
            canvas.translate(mPaddingLeft,mPaddingTop);
            Rect flagRect = new Rect();
            for(int i=0;i<flags.size();i++){
                int startX = (int)(flags.get(i)/(float)getMax()*(getWidth()-mPaddingLeft-mPaddingRight));
                int endX = startX+flagWidth;
                int top = (getHeight()>>1)-(flagHeight>>1);
                int bottom = top+flagHeight;
                flagRect.set(startX,top,endX,bottom);
                flagDrawable.setBounds(flagRect);
                flagDrawable.draw(canvas);
            }
            canvas.restore();
            if(thumb!=null) {
                canvas.save();
                canvas.translate(mPaddingLeft + getThumbOffset(), mPaddingTop);
                thumb.draw(canvas);
                canvas.restore();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			LeReflectionUtils.setFieldValue(this, "mMinHeight", 12);
			LeReflectionUtils.setFieldValue(this, "mMaxHeight", 12);
			setProgressDrawable(getContext().getDrawable(R.drawable.play_seekbar_touch));
			requestLayout();
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			LeReflectionUtils.setFieldValue(this, "mMinHeight", 6);
			LeReflectionUtils.setFieldValue(this, "mMaxHeight", 6);
			setProgressDrawable(getContext().getDrawable(R.drawable.play_seekbar));
			requestLayout();
			break;
		case MotionEvent.ACTION_MOVE:
			
			break;
		default:
			break;
		}
    	return super.onTouchEvent(event);
    }

    public ArrayList<Long> getFlags() {
        return flags;
    }

    public void setFlags(ArrayList<Long> flags) {
        this.flags = flags;
    }
}
