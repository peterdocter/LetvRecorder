package com.letv.android.recorder.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.*;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.letv.android.recorder.R;

/**
 * Created by snile on 15-1-12.
 */
public class FlagImageView extends ImageView {


    public FlagImageView(Context context) {
        super(context);
        init();
    }

    public FlagImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FlagImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FlagImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void  init(){
        flagTextColor = getContext().getResources().getColor(R.color.flag_text_color);
        flagTextSize = getContext().getResources().getDimensionPixelSize(R.dimen.flagTextSize);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(flagTextColor);
        mPaint.setTextSize(flagTextSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
        //mPaint.setTypeface();//sans-serif-medium
        mPaint.setTypeface(Typeface.create("sans-serif-medium",0));
    }

    Paint mPaint;
    int flagTextSize;
    int flagTextColor;
    private int flagCount;

    public void setFlagCount(int flagCount){
        this.flagCount = flagCount;
        if(flagCount>0){
            setImageResource(R.drawable.ic_rec_flag_no);
        }else{
            setImageResource(R.drawable.ic_rec_flag);
        }
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if(flagCount>0) {

            String countStr = getCountStr();

            Rect rectText =new Rect();
            mPaint.getTextBounds(countStr,0,countStr.length(),rectText);
            canvas.save();
            canvas.translate(0,rectText.height()/2);
            canvas.drawText(countStr,canvas.getWidth()>>1,canvas.getHeight()>>1,mPaint);
            canvas.restore();
        }
    }

    private String getCountStr(){
        if(flagCount<=0){
            return "";
        }else {
            return String.format("%02d",flagCount);
        }
    }

}
