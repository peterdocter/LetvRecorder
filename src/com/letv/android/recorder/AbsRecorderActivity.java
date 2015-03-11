package com.letv.android.recorder;

import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.*;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.WindowManager;
import android.animation.ObjectAnimator;

import com.letv.android.recorder.fragment.RecordedFragment;
import com.letv.android.recorder.service.Recorder;
import com.letv.android.recorder.service.RecorderService;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.service.Recorder.OnRecordTimeChangedListener;
import com.letv.android.recorder.service.Recorder.OnStateChangedListener;
import com.letv.android.recorder.tool.LockScreen;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.android.recorder.tool.StatusBarTool;
import com.letv.android.recorder.widget.FlagImageView;

public class AbsRecorderActivity extends Activity implements OnClickListener, OnStateChangedListener, OnRecordTimeChangedListener {

    protected ImageView recordBtn, stopBtn;
    protected FlagImageView flagBtn;
    protected MediaRecorderState mRecorderState ;
    protected RecorderReceiver mReceiver;
    protected Recorder mRecorder;

    public static RecordedFragment  recordedFragment;

//    protected LeTopWidget topWidget;

    private boolean isFistTime=false;

    public boolean isFistTime() {
        return isFistTime;
    }

    public void setFistTime(boolean isFistTime) {
        this.isFistTime = isFistTime;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recorder);
        isFistTime = RecordTool.isFirstLaunch(this);

        mReceiver = new RecorderReceiver();
        mRecorder = Recorder.getInstance();
        recordBtn = (ImageView) findViewById(R.id.recordBtn);
        stopBtn = (ImageView) findViewById(R.id.stopBtn);
        flagBtn = (FlagImageView) findViewById(R.id.flagBtn);
        recordBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        flagBtn.setOnClickListener(this);


        IntentFilter filter = new IntentFilter();
        filter.addAction(RecorderService.RECORDER_SERVICE_BROADCAST_NAME);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStart() {
        RecordTool.e("reboot->","--------------------->Abs record onStart");
        mRecorderState = RecordTool.getRecordState(this);
        mRecorder.setmOnStateChangedListener(this);
        mRecorder.setTimeChangedListener(this);
//        mRecorder.checkRecorderState();
        if(!isFistTime()) {
            updateUI();
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        RecordTool.e("reboot->","--------------------->Abs record onResume"+!isFistTime());
        RecordTool.hideNotificationWhenBack(this);
//        RecordTool.hintNotificationLedWhenBack(this);
//        if(!isFistTime()&&!recordedFragment.recordedAdapter.isActionMode()) {
//            updateUI();
//        }
        RecordTool.isRecordInBack=false;
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        RecordTool.e("reboot->","--------------------->Abs record onStop");
        if(mRecorder!=null){
            mRecorder.setmOnStateChangedListener(null);
            mRecorder.setTimeChangedListener(null);
        }

        RecordTool.e("reboot->","AbsRecorderStopState:"+RecordApp.getInstance().getmState());
        RecordTool.isRecordInBack=true;
        if(RecordApp.getInstance().getmState()==MediaRecorderState.PAUSED||
                RecordApp.getInstance().getmState()==MediaRecorderState.RECORDING){
            RecordTool.showNotificationWhenBack(this,RecordApp.getInstance().getmState());
//            RecordTool.showNotificationLedWhenBack(this);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        RecordTool.e("reboot->","AbsRecorderDestoryState:"+RecordApp.getInstance().getmState());
        RecordTool.e("reboot->","--------------------->Abs record onDestroy");
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View arg0) {

        RecordTool.e("reboot->", "Abs record before onClick:" +"click - in");
        if(!RecordTool.canClick(500))
            return;

        if(recordedFragment!=null){
            recordedFragment.onClick(arg0);
        }

        RecordTool.e("reboot->", "Abs record before onClick:" + mRecorderState.toString());
        switch (arg0.getId()) {
            case R.id.recordBtn:
                if (mRecorderState == MediaRecorderState.IDLE_STATE || mRecorderState == MediaRecorderState.PAUSED) {
                    mRecorder.startRecording(this);
                } else if (mRecorderState == MediaRecorderState.RECORDING) {
                    mRecorder.pauseRecording(this);
                }
                break;
            case R.id.stopBtn:
                mRecorder.stopRecording(this);
                RecorderService.saveRecording(this, RecordApp.getInstance().getRecordName());
                break;
            case R.id.flagBtn:
                if(mRecorderState == MediaRecorderState.RECORDING) {
                    RecordApp.getInstance().addFlag(RecorderService.recordRealDuring);
                }

                break;

            default:
                break;
        }
    }

    protected void updateUI() {
        mRecorderState=RecordApp.getInstance().getmState();
        RecordTool.e("reboot->", "Abs record before updateUI:" + mRecorderState.toString());
        if (MediaRecorderState.RECORDING == mRecorderState) {
            recordBtn.setImageResource(R.drawable.frame_record_pause);
            AnimationDrawable am_record=(AnimationDrawable)recordBtn.getDrawable();
            am_record.start();
            if(stopBtn.getVisibility()==View.INVISIBLE){
                Animation am_stop = AnimationUtils.loadAnimation(AbsRecorderActivity.this, R.anim.anim_in_bottom);
                stopBtn.startAnimation(am_stop);

                Animation am_flag = AnimationUtils.loadAnimation(AbsRecorderActivity.this, R.anim.anim_in_bottom);
                am_flag.setStartOffset(70);
                flagBtn.startAnimation(am_flag);
            }

            recordedFragment.startRecording();
            stopBtn.setVisibility(View.VISIBLE);
            flagBtn.setVisibility(View.VISIBLE);
//            topWidget.setCenterTitle(R.string.new_recorder);
            getActionBar().hide();
        } else if (MediaRecorderState.PAUSED == mRecorderState) {
            recordBtn.setImageResource(R.drawable.frame_pause_record);
            AnimationDrawable am_record=(AnimationDrawable)recordBtn.getDrawable();
            am_record.start();
            stopBtn.setVisibility(View.VISIBLE);
            flagBtn.setVisibility(View.VISIBLE);
//            topWidget.setCenterTitle(R.string.new_recorder);
            getActionBar().hide();
        } else if (MediaRecorderState.IDLE_STATE == mRecorderState) {
            if(stopBtn.getVisibility()==View.VISIBLE){
            	Animation am_stop = AnimationUtils.loadAnimation(AbsRecorderActivity.this, R.anim.anim_out_bottom);
            	stopBtn.startAnimation(am_stop);
            	
            	Animation am_flag = AnimationUtils.loadAnimation(AbsRecorderActivity.this, R.anim.anim_out_bottom);
            	am_flag.setStartOffset(70);
                flagBtn.startAnimation(am_flag);
            }
            recordBtn.setImageResource(R.drawable.frame_pause_record);
            AnimationDrawable am_record=(AnimationDrawable)recordBtn.getDrawable();
            am_record.start();
            stopBtn.setVisibility(View.INVISIBLE);
            flagBtn.setVisibility(View.INVISIBLE);
            getActionBar().show();
//            topWidget.setCenterTitle(R.string.record_note);
//            getActionBar().setTitle(R.string.record_note);
        } else if (MediaRecorderState.STOPPED == mRecorderState) {
             recordBtn.setImageResource(R.drawable.frame_play_record);
            AnimationDrawable am_record=(AnimationDrawable)recordBtn.getDrawable();
            am_record.start();
            recordedFragment.stopRecording();
            stopBtn.setVisibility(View.INVISIBLE);
            flagBtn.setVisibility(View.INVISIBLE);
//            topWidget.setCenterTitle(R.string.record_note)
//            getActionBar().setTitle(R.string.record_note);
        } 
//        else if (MediaRecorderState.PLAYING_PAUSED == mRecorderState) {
//            recordBtn.setImageResource(R.drawable.pause_selector);
////            topWidget.setCenterTitle(R.string.record_note);
//            getActionBar().setTitle(R.string.record_note);
//        } else if (MediaRecorderState.PLAYING == mRecorderState) {
//            recordBtn.setImageResource(R.drawable.start_selector);
////            topWidget.setCenterTitle(R.string.record_note);
//            getActionBar().setTitle(R.string.record_note);
//        }

        if (recordedFragment!=null&&!RecordApp.getInstance().isActionMode()) {
            recordedFragment.refreshRecordList();
        }
    }

    @Override
    public void onStateChanged(MediaRecorderState state) {
        RecordTool.e("reboot->","Abs record call onStateChanged:"+state.toString());
        mRecorderState = state;

        if(mRecorderState == MediaRecorderState.STOPPED||
                mRecorderState == MediaRecorderState.IDLE_STATE){
            recordedFragment.stopRecording();
        }
//
        if(mRecorderState == MediaRecorderState.RECORDING){
            recordedFragment.startRecording();
        }
//
        updateUI();
    }

    @Override
    public void onError(int error) {

    }

    private class RecorderReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(RecordTool.RECORDER_SERVICE_STATE)) {
                String stateStr = intent.getStringExtra(RecordTool.RECORDER_SERVICE_STATE);
                MediaRecorderState state = MediaRecorderState.getState(stateStr);
                mRecorder.setState(state);
            }
        }
    }

    @Override
    public void onRecordTimeChanged(long timeMillils,float db) {
        if(recordedFragment!=null){
            recordedFragment.updateRecordTimeUI(timeMillils,db);
        }
        flagBtn.setFlagCount(RecordApp.getInstance().getFlags().size());
    }

}
