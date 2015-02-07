package com.letv.android.recorder.receiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.service.PlayService;
import com.letv.android.recorder.service.Recorder;
import com.letv.android.recorder.service.RecorderService;
import com.letv.android.recorder.tool.RecordTool;

import java.util.Set;

/**
 * Created by snile on 14-8-28.
 */
public class PhoneReceiver extends BroadcastReceiver {

    private static final String TAG = PhoneReceiver.class.getSimpleName();

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        RecordTool.loge(this.getClass().getSimpleName(),intent.getAction());
        RecordTool.loge(this.getClass().getSimpleName(),intent.toString());

        Bundle bundle = intent.getExtras();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String stateStr =  sp.getString(Recorder.MediaRecorderState.KEY, Recorder.MediaRecorderState.getStateString(Recorder.MediaRecorderState.IDLE_STATE));


        boolean isCallRecording = RecordTool.getRecordType(context);
//        Recorder.MediaRecorderState mState = Recorder.MediaRecorderState.getState(stateStr);
        Recorder.MediaRecorderState mState = RecordApp.getInstance().getmState();
        String state = bundle.getString("state");

        if(!TextUtils.isEmpty(state)){
            if(state.equals("RINGING")||state.equals("OFFHOOK")){
                if(mState == Recorder.MediaRecorderState.RECORDING){
                    if(!isCallRecording) {
                        RecorderService.pauseRecoring(context);
                    }
                }else if(RecordApp.getInstance().getmState()== Recorder.MediaRecorderState.PLAYING){
                    PlayService.pausePlay(context);
                }
            }else if(state.equals("IDLE")){

                if(mState== Recorder.MediaRecorderState.PAUSED){
                    RecorderService.startRecording(context);
                }
//                else if(RecordApp.getInstance().getmState()== Recorder.MediaRecorderState.PLAYING_PAUSED){
//                    PlayService.play(context);
//                }
            }
        }

    }

}
