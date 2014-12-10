package com.letv.android.recorder;

import android.app.ActionBar;
import android.app.Activity;
import android.content.*;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.letv.android.recorder.fragment.RecordedFragment;
import com.letv.android.recorder.service.Recorder;
import com.letv.android.recorder.service.RecorderService;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.service.Recorder.OnRecordTimeChangedListener;
import com.letv.android.recorder.service.Recorder.OnStateChangedListener;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.android.recorder.tool.StatusBarTool;

public class AbsRecorderActivity extends Activity implements OnClickListener, OnStateChangedListener, OnRecordTimeChangedListener {

    protected ImageView recordBtn, stopBtn,flagBtn;
    protected MediaRecorderState mRecorderState ;
    protected RecorderReceiver mReceiver;
    protected Recorder mRecorder;

    protected RecordedFragment recordedFragment;

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

        getActionBar().setTitle(R.string.record_note);
        mReceiver = new RecorderReceiver();
        mRecorder = Recorder.getInstance();
        recordBtn = (ImageView) findViewById(R.id.recordBtn);
        stopBtn = (ImageView) findViewById(R.id.stopBtn);
        flagBtn = (ImageView) findViewById(R.id.flagBtn);
        recordBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        flagBtn.setOnClickListener(this);


        IntentFilter filter = new IntentFilter();
        filter.addAction(RecorderService.RECORDER_SERVICE_BROADCAST_NAME);
        registerReceiver(mReceiver, filter);


    }

    @Override
    protected void onStart() {
        mRecorderState = RecordApp.getInstance().getmState();
        mRecorder.setmOnStateChangedListener(this);
        mRecorder.setTimeChangedListener(this);
        super.onResume();
    }

    @Override
    protected void onResume() {

        RecordTool.hideNotificationWhenBack(this);
        if(!isFistTime()) {
            updateUI();
        }
        super.onResume();
    }

    @Override
    protected void onStop() {

        if(mRecorder!=null){
            mRecorder.setmOnStateChangedListener(null);
            mRecorder.setTimeChangedListener(null);
        }

        if(RecordApp.getInstance().getmState()==MediaRecorderState.PAUSED||
                RecordApp.getInstance().getmState()==MediaRecorderState.RECORDING){
            RecordTool.showNotificationWhenBack(this);
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View arg0) {

        if(!RecordTool.canClick(500))
            return;

        if(recordedFragment!=null){
            recordedFragment.onClick(arg0);
        }

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
        Log.i("state", "updateUI");
        if (MediaRecorderState.RECORDING == mRecorderState) {
            recordBtn.setImageResource(R.drawable.pause_selector);
            recordedFragment.startRecording();
            stopBtn.setVisibility(View.VISIBLE);
            flagBtn.setVisibility(View.VISIBLE);
//            topWidget.setCenterTitle(R.string.new_recorder);
            getActionBar().hide();
        } else if (MediaRecorderState.PAUSED == mRecorderState) {
            recordBtn.setImageResource(R.drawable.start_selector);
            stopBtn.setVisibility(View.VISIBLE);
            flagBtn.setVisibility(View.VISIBLE);
//            topWidget.setCenterTitle(R.string.new_recorder);
            getActionBar().hide();
        } else if (MediaRecorderState.IDLE_STATE == mRecorderState) {
            recordBtn.setImageResource(R.drawable.start_selector);
            stopBtn.setVisibility(View.INVISIBLE);
            flagBtn.setVisibility(View.INVISIBLE);
            getActionBar().show();
//            topWidget.setCenterTitle(R.string.record_note);
            getActionBar().setTitle(R.string.record_note);
        } else if (MediaRecorderState.STOPPED == mRecorderState) {
            recordBtn.setImageResource(R.drawable.start_selector);
            recordedFragment.stopRecording();
            stopBtn.setVisibility(View.INVISIBLE);
            flagBtn.setVisibility(View.INVISIBLE);
//            topWidget.setCenterTitle(R.string.record_note)
            getActionBar().setTitle(R.string.record_note);
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

        if (recordedFragment!=null) {
            recordedFragment.refreshRecordList();
        }
    }

    @Override
    public void onStateChanged(MediaRecorderState state) {

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
                Log.i("state", stateStr);
                mRecorder.setState(state);
            }
        }
    }

    @Override
    public void onRecordTimeChanged(long timeMillils,float db) {
        if(recordedFragment!=null){
            recordedFragment.updateRecordTimeUI(timeMillils,db);
        }
    }

}
