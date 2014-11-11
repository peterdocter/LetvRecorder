package com.letv.android.recorder.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.android.recorder.R;

public class RecordingView extends View {

    private static final String LOG_TAG = "LEUI----"+RecordingView.class.getSimpleName();

//    private ArrayList<WavePoint> wavePoints = new ArrayList<WavePoint>();


	public RecordingView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		timePaint = new Paint();

        bgDotColor = context.getResources().getColor(R.color.bgDotColor);

        activeDotColor = context.getResources().getColor(R.color.activeDotColor);

        bgColor = context.getResources().getColor(R.color.actionBarBackground);

        bgDotPaint = new Paint();
        activeDotPaint = new Paint();
        flagPaint = new Paint();

        bgDotPaint.setTextAlign(Paint.Align.CENTER);
        bgDotPaint.setStrokeWidth(4);
        bgDotPaint.setColor(bgColor);

        activeDotPaint.setColor(activeDotColor);
        activeDotPaint.setStrokeWidth(5);
        activeDotPaint.setTextAlign(Paint.Align.CENTER);

        flagPaint.setStrokeWidth(2);
        flagPaint.setColor(activeDotColor);
        activeDotPaint.setTextAlign(Paint.Align.CENTER);

        timePaint.setTextAlign(Paint.Align.CENTER);

		int timeDefaultColor = 0;
		int timeDefaultSize = 0;
		int ruleDrawableResId = 0;
		int ruleItemHeightDef = 0;

		TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.RecordingView, defStyleAttr, 0);
		timeDefaultColor = typedArray.getColor(R.styleable.RecordingView_timeColor, timeDefaultColor);
		timeDefaultSize = typedArray.getDimensionPixelSize(R.styleable.RecordingView_timeSize, timeDefaultSize);

		timePaint.setColor(timeDefaultColor);
		timePaint.setTextSize(timeDefaultSize);


		int resId = typedArray.getResourceId(R.styleable.RecordingView_ruleDrawable, ruleDrawableResId);
		if (resId != 0) {
			ruleBmp = ((BitmapDrawable) getResources().getDrawable(resId)).getBitmap();
			ruleBmpWidth = ruleBmp.getWidth();
			ruleBmpHeight = ruleBmp.getHeight();
			markItemWidth = ruleBmpWidth / 4;
		}

        dotTime = (int)(9f/markItemWidth*500);

		ruleItemHeight = typedArray.getDimensionPixelSize(R.styleable.RecordingView_ruleHight, ruleItemHeightDef);

		typedArray.recycle();

		Rect rect = new Rect();
		timePaint.getTextBounds("00:00", 0, 5, rect);
//		textOffset = (ruleBmpWidth - rect.width()) >> 1;
		textHeight = ruleItemHeight - (ruleBmpHeight >> 0);

	}

	public RecordingView(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.recordViewStyle);
	}

	public RecordingView(Context context) {
		this(context, null);
	}

	private Paint bgDotPaint, activeDotPaint, timePaint, rulePaint = new Paint();

    private Paint flagPaint;

	private Bitmap ruleBmp = null;

    private int dotTime = 0;

	private int ruleBmpWidth = 0;
	private int ruleBmpHeight = 0;
	private int ruleItemHeight = 0;

	private int markItemWidth = 0;

	private final float markItemMills = 500f;
	private final int ruleBmpMills = 2000;

	private int maxDbNum = 80;


	private long recordTimeMillis = 0;

	private float textHeight;
	private float midY;
	private int width, height;
	private float offsetX;

	private List<TimeHolder> holders = new ArrayList<RecordingView.TimeHolder>();

//    private List<TimeTable> cacheTimeTable = new ArrayList<TimeTable>();

	private PointF curPoint = new PointF();

    int bgColor;
    int bgDotColor=0;
    int activeDotColor=0;

    private int getTimeMillsByLength(float lengthPx){
        return (int) (lengthPx / markItemWidth * markItemMills);
    }

    int waveCount = 0;

    @Override
	protected void onDraw(Canvas canvas) {
		width = getWidth();
		height = getHeight();

		offsetX = (recordTimeMillis % ruleBmpMills) / markItemMills * markItemWidth;

        int offsetTime = getTimeMillsByLength(offsetX);

        waveCount = (int)Math.min((recordTimeMillis)/getTimeMillsByLength(9),(getWidth()>>1)/9+1);

		midY = (height >> 1) + (ruleItemHeight >> 1);


        canvas.drawColor(bgDotColor);
        canvas.drawRect(0,0,width,ruleItemHeight,bgDotPaint);

		canvas.save();
		canvas.translate(-offsetX, 0);

		// draw time mark begin
		curPoint.set((width >> 1) - (ruleBmpWidth >> 1), ruleItemHeight - ruleBmpHeight);
		canvas.drawBitmap(ruleBmp, curPoint.x, curPoint.y, rulePaint);

		canvas.drawText(RecordTool.ruleTime(recordTimeMillis - offsetTime), curPoint.x + (ruleBmpWidth>>1), textHeight, timePaint);

		int index = 1;
		while (true) {// left mark
			if (curPoint.x > -ruleBmpWidth) {
				curPoint.x -= ruleBmpWidth;
				canvas.drawBitmap(ruleBmp, curPoint.x, curPoint.y, rulePaint);
				canvas.drawText(RecordTool.ruleTime(recordTimeMillis - ruleBmpMills * index - offsetTime), curPoint.x + (ruleBmpWidth>>1), textHeight, timePaint);
				index++;
			} else {
				index = 1;
				break;
			}
		}
		curPoint.set((width >> 1) - (ruleBmpWidth >> 1), ruleItemHeight - ruleBmpHeight);

		while (true) {// right mark
			if (curPoint.x < width + ruleBmpWidth) {
				curPoint.x += ruleBmpWidth;
				canvas.drawBitmap(ruleBmp, curPoint.x, curPoint.y, rulePaint);
				canvas.drawText(RecordTool.ruleTime(recordTimeMillis + ruleBmpMills * index - offsetTime), curPoint.x + (ruleBmpWidth>>1), textHeight, timePaint);
				index++;
			} else {
				index = 1;
				break;
			}
		}

        canvas.restore();

        // draw right

        float dotOffsetTime = recordTimeMillis%getTimeMillsByLength(9);
        float dotOffest = dotOffsetTime/getTimeMillsByLength(9)*9;
        canvas.save();
        canvas.translate(-dotOffest,0);

        for(int i=(width>>1)-2;i<=width+10+offsetX;i+=9){
            canvas.drawLine(i,ruleItemHeight,i,height,bgDotPaint);
        }

        // draw left
        waveIndex = holders.size()-1;
        int times =0;
        for(int i=(width>>1)-2;i>=-10;i-=9){
            canvas.drawLine(i,ruleItemHeight,i,height,bgDotPaint);
            drawWaveLine(canvas,i-4,(recordTimeMillis/getTimeMillsByLength(9)-times)*getTimeMillsByLength(9));
            times++;
        }

        // draw top
        for(int i=(int)(midY-5);i<=height;i+=9){
            canvas.drawLine(0,i,width+offsetX,i,bgDotPaint);
        }

        // draw bottom
        for(int i=(int)(midY-5);i>=ruleItemHeight+5;i-=9){
            canvas.drawLine(0,i,width+offsetX,i,bgDotPaint);
        }

        canvas.restore();


        canvas.save();
        canvas.translate(-offsetX, 0);

        ArrayList<Long> flag = RecordApp.getInstance().getFlags();
        if(flag!=null&&flag.size()>0){
            for(int i = flag.size()-1;i>=0;i-- ){
                float startXTemp = (width >> 1) - ((recordTimeMillis - flag.get(i))) / markItemMills * markItemWidth + offsetX;
                if(startXTemp > -Math.abs(offsetX)) {
                    canvas.drawLine(startXTemp, ruleItemHeight - 20, startXTemp, ruleItemHeight + 20, flagPaint);
                }
            }
        }

        canvas.restore();

	}

    int waveIndex = 0;


    private void drawWaveLine(Canvas canvas,int startX,long curMills){

        if(curMills<=0){
            return;
        }

        int saveCount = canvas.save();

        float avgDb = getAvgDB(curMills);

        float startYDown =midY+(height-midY)*(avgDb/32768);
        float startYUp = midY-(height-midY)*(avgDb/32768);

        startYDown = startYDown>midY+3?startYDown:midY+3;
        startYUp = startYUp<midY-3?startYUp:midY-3;
        activeDotPaint.setStrokeWidth(5);
        canvas.drawLine(startX,startYUp,startX,startYDown,activeDotPaint);
        waveIndex--;
        canvas.restoreToCount(saveCount);
    }

    public float getAvgDB(long curMills){

        float totalDb = 0;
        int totalCount =0;

        for(int i=holders.size()-1;i>=0;i--){
            if((curMills - holders.get(i).timeMillis<=getTimeMillsByLength(9))&&(curMills-holders.get(i).timeMillis)>=0){
                totalDb +=holders.get(i).db;
                totalCount++;
            }
        }

        return totalCount==0?totalDb:totalDb/totalCount;

    }

	public void startRecording() {
//        holders.clear();
//        RecordApp.getInstance().clearFlag();
	}

    public void resumeRecording(){

    }

	public void updateRecordUI(long recordTimeMillis, float db) {
		this.recordTimeMillis = recordTimeMillis;
		if (db > 0) {

            if(holders.size()==0){
                holders.add(new TimeHolder(recordTimeMillis-50,0));
            }

			holders.add(new TimeHolder(recordTimeMillis, db));

			int exrtaNum = holders.size() - maxDbNum;
			if (exrtaNum > 0) {
				for (int i = 0; i < exrtaNum; i++) {
					holders.remove(0);
				}
			}
		}

		invalidate();
	}

	public void stopRecording() {
		holders.clear();
        RecordApp.getInstance().clearFlag();
	}

	class TimeHolder {
		long timeMillis;
		float db;

		public TimeHolder(long timeMillis, float db) {
			this.timeMillis = timeMillis;
			this.db = db;
		}
	}

//    class WavePoint{
//        float x;
//        float upY;
//        float downY;
//
//        WavePoint(float x, float upY, float downY) {
//            this.x = x;
//            this.upY = upY;
//            this.downY = downY;
//        }
//    }
}
