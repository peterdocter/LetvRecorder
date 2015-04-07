package com.letv.android.recorder.widget;

import java.util.*;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.util.LongSparseArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.android.recorder.R;

public class RecordingView extends View {

    private static final String LOG_TAG = RecordingView.class.getSimpleName();

//    private ArrayList<WavePoint> wavePoints = new ArrayList<WavePoint>();

    private static final boolean DEBUG = false;


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
        timePaint.setFakeBoldText(true);
        int timeLineWidth = typedArray.getDimensionPixelSize(R.styleable.RecordingView_timeLineWidth, 2);
        int timePreLength = typedArray.getDimensionPixelSize(R.styleable.RecordingView_timePreLength, 27 * 3);
        int timeTextOffset = typedArray.getDimensionPixelSize(R.styleable.RecordingView_timeTextOffset,0);
        ruleBmpWidth = (timeLineWidth + timePreLength) << 1;
        ruleBmpHeight = tallHeight * 3;
        markItemWidth = ruleBmpWidth >> 1;

        ruleItemHeight = typedArray.getDimensionPixelSize(R.styleable.RecordingView_ruleHight, ruleItemHeightDef);

        typedArray.recycle();

        Rect rect = new Rect();
        timePaint.setTypeface(Typeface.create("sans-serif",0));
        timePaint.getTextBounds("00:00", 0, 5, rect);
        timePaint.setAntiAlias(true);
        textHeight = ruleItemHeight - tallHeight-timeTextOffset;
        if (DEBUG) {
            framePaint = new Paint();
            framePaint.setTextSize(28);
            framePaint.setColor(Color.RED);
        }

    }

    public RecordingView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.recordViewStyle);
    }

    public RecordingView(Context context) {
        this(context, null);
    }

    private Paint bgDotPaint, activeDotPaint, timePaint;

    private Paint flagPaint, framePaint;

    private int ruleBmpWidth = 0;
    private int ruleBmpHeight = 0;
    private int ruleItemHeight = 0;

    private int markItemWidth = 0;

    private int smallHeight = 9;
    private int tallHeight = 15;//15
    private float ringR = 9 / 2f;

    private final float markItemMills = 1000f;
    private final int ruleBmpMills = 2000;
    private int maxDbNum = 200;
//	private long recordTimeMillis = 0;

    private float textHeight;
    private float midY;
    private int width, height;
//	private float offsetX;
//	private float intercaterOffsetX;

    private List<TimeHolder> holders = new ArrayList<RecordingView.TimeHolder>();

    private PointF curPoint = new PointF();

    int bgColor;
    int bgDotColor = 0;
    int activeDotColor = 0;

    private int getTimeMillsByLength(float lengthPx) {
        return (int) (lengthPx / markItemWidth * markItemMills);
    }

//    int waveCount = 0;

    float calculateIndicatorOffsetX(long recordTimeMillis) {
        float indicatorOffsetX;
        long mostOffsetTimeMills = getTimeMillsByLength(getWidth() / 2 - markItemWidth);

        long needOffsetTimeMills = mostOffsetTimeMills - recordTimeMillis;

        if (needOffsetTimeMills > 0) {
            indicatorOffsetX = needOffsetTimeMills / markItemMills * markItemWidth;
        } else {
            indicatorOffsetX = 0;
        }
        return indicatorOffsetX;
    }

    private long preDrawTime;

    private long preTime, postTime;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        width = right - left;
        height = bottom - top;
        midY = (height >> 1) + (ruleItemHeight >> 1);

        if (stickBuffer == null) {
            stickBuffer = new PointsBuffer();
        }

        stickHorizontalPointsBuffer();
        stickVerticalPointsBuffer();
    }

    @Override
    public void draw(Canvas canvas) {
        if (DEBUG) {
            preTime = System.currentTimeMillis();
        }
//        PointsBuffer buffer = queue.poll();
        if (currentBuffer != null) {
            RecordTool.e(LOG_TAG,"draw time:"+currentBuffer.bufferTime);
            float offsetX = currentBuffer.offsetX;
            float indicatorOffsetX = currentBuffer.indicatorOffsetX;

            int offsetTime = getTimeMillsByLength(offsetX);

            canvas.drawColor(bgDotColor);
            canvas.drawRect(0, 0, width, ruleItemHeight, bgDotPaint);

            float dotOffset = currentBuffer.dotOffsetX;
            long recordTimeMillis = currentBuffer.bufferTime;
            canvas.save();
            canvas.translate(-dotOffset, 0);
            canvas.translate(-indicatorOffsetX, 0);

            canvas.drawLines(currentBuffer.verticalPoints, bgDotPaint);
            canvas.drawLines(currentBuffer.wavePaints, activeDotPaint);
            canvas.drawLines(currentBuffer.horizontalPoint, bgDotPaint);

            canvas.restore();

            canvas.save();
            canvas.translate(-offsetX, 0);
            canvas.translate(-indicatorOffsetX, 0);
            // draw time mark begin
            curPoint.set(width >> 1, ruleItemHeight);
            canvas.drawLine(curPoint.x, curPoint.y, curPoint.x, curPoint.y - tallHeight, timePaint);
            canvas.drawLine(0, curPoint.y, getWidth() + offsetX + indicatorOffsetX, curPoint.y, timePaint);
            canvas.drawLine(0, getHeight() - ringR, getWidth() + offsetX + indicatorOffsetX, getHeight() - ringR, timePaint);
            canvas.drawText(RecordTool.ruleTime(recordTimeMillis - offsetTime), curPoint.x, textHeight, timePaint);
            int index = 1;
            while (true) {// left mark
                if (curPoint.x > -ruleBmpWidth + indicatorOffsetX) {
                    curPoint.x -= markItemWidth;
                    if (index % 2 == 0) {
                        canvas.drawLine(curPoint.x, curPoint.y, curPoint.x, curPoint.y - tallHeight, timePaint);
                        canvas.drawText(RecordTool.ruleTime(recordTimeMillis - ruleBmpMills * index / 2 - offsetTime), curPoint.x, textHeight, timePaint);
                    } else {
                        canvas.drawLine(curPoint.x, curPoint.y, curPoint.x, curPoint.y - smallHeight, timePaint);
                    }
                    index++;
                } else {
                    index = 1;
                    break;
                }
            }
            curPoint.set(width >> 1, ruleItemHeight);

            while (true) {// right mark
                if (curPoint.x < width + ruleBmpWidth + indicatorOffsetX) {
                    curPoint.x += markItemWidth;
                    if (index % 2 == 0) {
                        canvas.drawLine(curPoint.x, curPoint.y, curPoint.x, curPoint.y - tallHeight, timePaint);
                        canvas.drawText(RecordTool.ruleTime(recordTimeMillis + ruleBmpMills * index / 2 - offsetTime), curPoint.x, textHeight, timePaint);
                    } else {
                        canvas.drawLine(curPoint.x, curPoint.y, curPoint.x, curPoint.y - smallHeight, timePaint);
                    }
                    index++;
                } else {
                    index = 1;
                    break;
                }
            }

            canvas.restore();

            canvas.save();
            canvas.translate(-offsetX, 0);
            canvas.translate(-indicatorOffsetX, 0);

            ArrayList<Long> flag = RecordApp.getInstance().getFlags();
            if (flag != null && flag.size() > 0) {
                flagPaint.setStrokeWidth(2);
                for (int i = flag.size() - 1; i >= 0; i--) {
                    float startXTemp = (width >> 1) - ((recordTimeMillis - flag.get(i))) / markItemMills * markItemWidth + offsetX;
                    if (startXTemp > -Math.abs(offsetX)) {
                        canvas.drawLine(startXTemp, ruleItemHeight - 20, startXTemp, ruleItemHeight + 20, flagPaint);
                    }
                }
            }

            canvas.restore();
            //draw intercater
            canvas.save();
            canvas.translate(-indicatorOffsetX, 0);
            canvas.drawLine(getWidth() >> 1, ruleItemHeight, getWidth() >> 1, getHeight() - ringR, flagPaint);
            RectF oval = new RectF((getWidth() >> 1) - ringR, ruleItemHeight - ringR,
                    (getWidth() >> 1) + ringR, ruleItemHeight + ringR);
            canvas.drawArc(oval, 0, 360, false, flagPaint);
            oval.set(oval.left, getHeight() - (ringR * 2),
                    oval.right, getHeight());
            canvas.drawArc(oval, 0, 360, false, flagPaint);
            canvas.restore();
        }
        if (DEBUG) {
            long curTime = System.currentTimeMillis();
            long time = curTime - preDrawTime;
            postTime = curTime;
            if (preDrawTime != 0) {
                canvas.drawText("frame:" + 1000f / time, getWidth() * 3 / 4, getHeight() / 2, framePaint);
                canvas.drawText(postTime - preTime + "", getWidth() * 3 / 4, getHeight() / 2 - 100, framePaint);
            }
            preDrawTime = curTime;
        }

    }

    int waveIndex = 0;


    private void getWaveLine(/*Canvas canvas,*/int startX, long curMills) {

        if (curMills <= 0) {
            return;
        }
        if (null != wavePointCache.get(curMills)) {
            WavePoint wavePoint = wavePointCache.get(curMills);
            pointFList.add(new PointF(startX, wavePoint.startPointF.y));
            pointFList.add(new PointF(startX, wavePoint.endPointF.y));
        } else {
            float avgDb = getAvgDB(curMills);

            float startYDown = midY + (height - midY) * (avgDb / 32768);
            float startYUp = midY - (height - midY) * (avgDb / 32768);

            startYDown = startYDown > midY + 3 ? startYDown : midY + 3;
            startYUp = startYUp < midY - 3 ? startYUp : midY - 3;
//        activeDotPaint.setStrokeWidth(5);
            pointFList.add(new PointF(startX, startYUp));
            pointFList.add(new PointF(startX, startYDown));
            WavePoint wavePoint = new WavePoint();
            wavePoint.startPointF = new PointF(startX, startYUp);
            wavePoint.endPointF = new PointF(startX, startYDown);
            wavePointCache.append(curMills, wavePoint);
        }
    }

    public float getAvgDB(long curMills) {

        float totalDb = 0;
        int totalCount = 0;

        for (int i = holders.size() - 1; i >= 0; i--) {
            if ((curMills - holders.get(i).timeMillis <= getTimeMillsByLength(9)) && (curMills - holders.get(i).timeMillis) >= 0) {
                totalDb += holders.get(i).db;
                totalCount++;
            }
        }

        return totalCount == 0 ? totalDb : totalDb / totalCount;

    }

    public void startRecording() {
//        holders.clear();
//        RecordApp.getInstance().clearFlag();
        wavePointCache.clear();
    }

    public void stopRecording() {
        holders.clear();
        wavePointCache.clear();
//        RecordTool.e("RecordApp","recordingView");
//        RecordApp.getInstance().clearFlag();
//		mHandler.removeCallbacks(waveRunnable);

    }

    public void resumeRecording() {
    }

    public void updateRecordUI(final long recordTimeMillis, float db) {

        RecordTool.e(LOG_TAG,"deal time:"+recordTimeMillis);

        if (db > 0) {

            if (holders.size() == 0) {
                holders.add(new TimeHolder(recordTimeMillis - 50, 0));
            }

            holders.add(new TimeHolder(recordTimeMillis, db));

            int extraNum = holders.size() - maxDbNum;
            while (extraNum > 0) {
                holders.remove(0);
                extraNum--;
            }
            extraNum = wavePointCache.size() - 1000;
            while (extraNum > 0) {
                wavePointCache.removeAt(0);
                extraNum--;
            }
        }

        float offsetX = (recordTimeMillis % ruleBmpMills) / markItemMills * markItemWidth;
        float dotOffsetTime = recordTimeMillis % getTimeMillsByLength(9);
        float dotOffset = dotOffsetTime / getTimeMillsByLength(9) * 9;
        float indicatorOffsetX = calculateIndicatorOffsetX(recordTimeMillis);


        currentBuffer = new PointsBuffer();

        if (stickBuffer == null || stickBuffer.horizontalPoint == null)
            stickHorizontalPointsBuffer();
        currentBuffer.horizontalPoint = stickBuffer.horizontalPoint;
        if (stickBuffer == null || stickBuffer.verticalPoints == null)
            stickVerticalPointsBuffer();
        currentBuffer.verticalPoints = stickBuffer.verticalPoints;

        pointFList.clear();

        int times = 0;
        for (int i = (width >> 1) - 2; i >= -10; i -= 9) {
            getWaveLine(i - 4, (recordTimeMillis / getTimeMillsByLength(9) - times) * getTimeMillsByLength(9));
            times++;
        }
        currentBuffer.wavePaints = getPoint(pointFList);


        currentBuffer.offsetX = offsetX;
        currentBuffer.dotOffsetX = dotOffset;
        currentBuffer.indicatorOffsetX = indicatorOffsetX;
        currentBuffer.bufferTime = recordTimeMillis;
//        queue.add(pointsBuffer);
        invalidate();

    }


    private List<PointF> pointFList = new ArrayList<PointF>();

    private float[] getPoint(List<PointF> pointFList) {
        float[] points = new float[pointFList.size() << 1];
        for (int i = 0; i < pointFList.size(); i++) {
            points[(i << 1) + 0] = pointFList.get(i).x;
            points[(i << 1) + 1] = pointFList.get(i).y;
        }
        return points;
    }


    class TimeHolder {
        long timeMillis;
        float db;

        public TimeHolder(long timeMillis, float db) {
            this.timeMillis = timeMillis;
            this.db = db;
        }
    }

    private Handler mHandler = new Handler();

//    private Queue<PointsBuffer> queue = new LinkedList<PointsBuffer>();

    private PointsBuffer stickBuffer = new PointsBuffer();
    private PointsBuffer currentBuffer ;

//	private LinkedHashMap<Long,WavePoint> wavePointCache = new LinkedHashMap<Long, WavePoint>();

    private LongSparseArray<WavePoint> wavePointCache = new LongSparseArray<WavePoint>();

    private void stickVerticalPointsBuffer() {
        pointFList.clear();

        for (int i = (width >> 1) - 2; i <= (width << 1); i += 9) {
            pointFList.add(new PointF(i, ruleItemHeight));
            pointFList.add(new PointF(i, height));
        }

        for (int i = (width >> 1) - 2; i >= -10; i -= 9) {
            pointFList.add(new PointF(i, ruleItemHeight));
            pointFList.add(new PointF(i, height));
        }
        stickBuffer.verticalPoints = getPoint(pointFList);
    }

    private void stickHorizontalPointsBuffer() {
        pointFList.clear();
        for (int i = (int) (midY - 5); i <= height; i += 9) {
            pointFList.add(new PointF(0, i));
            pointFList.add(new PointF(width << 1, i));
        }

        // draw top
        for (int i = (int) (midY - 5); i >= ruleItemHeight + 5; i -= 9) {
            pointFList.add(new PointF(0, i));
            pointFList.add(new PointF(width << 1, i));
        }

        stickBuffer.horizontalPoint = getPoint(pointFList);

    }

    class WavePoint {
        PointF startPointF;
        PointF endPointF;
    }

    class PointsBuffer {
        float[] verticalPoints;
        float[] horizontalPoint;
        float[] wavePaints;
        float offsetX;
        float indicatorOffsetX;
        float dotOffsetX;
        long bufferTime;
    }
}
