package com.letv.android.recorder.receiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
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
import com.letv.android.recorder.tool.SettingTool;

import java.util.Set;

/**
 * Created by snile on 14-8-28.
 */
public class PhoneReceiver extends BroadcastReceiver {

    private static final String TAG = PhoneReceiver.class.getSimpleName();

    private Context context;

    private AudioManager am;
    @Override
    public void onReceive(Context context, Intent intent) {
        if(!RecordTool.canClick(1000)){
            return;
        }
        this.context = context;
        am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        RecordTool.loge(this.getClass().getSimpleName(),intent.getAction());
        RecordTool.loge(this.getClass().getSimpleName(),intent.toString());

        Bundle bundle = intent.getExtras();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String stateStr =  sp.getString(Recorder.MediaRecorderState.KEY, Recorder.MediaRecorderState.getStateString(Recorder.MediaRecorderState.IDLE_STATE));


        boolean isCallRecording = RecordTool.getRecordType(context);
//        Recorder.MediaRecorderState mState = Recorder.MediaRecorderState.getState(stateStr);
        Recorder.MediaRecorderState mState = RecordApp.getInstance().getmState();
        String state = bundle.getString("state");
        RecordTool.e(TAG,"phone:state:"+state+" ->mState:"+mState);
        if(!TextUtils.isEmpty(state)){
            if(state.equals("RINGING")||state.equals("OFFHOOK")){
                if(mState == Recorder.MediaRecorderState.RECORDING){
                    if(!isCallRecording) {
                        if(SettingTool.isSilence(context)) {
                            RecordTool.e("phone_rec","1:"+am.getRingerMode());
                            RecordTool.saveRingMode(context, am.getRingerMode());
                            am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                            RecordTool.e("phone_rec", "2:" + am.getRingerMode());
                        }
                        RecordTool.e(TAG,"pauseRecoring");
                        RecorderService.pauseRecoring(context);
                    }
                }else if(RecordApp.getInstance().getmState()== Recorder.MediaRecorderState.PLAYING){
                    PlayService.pausePlay(context);
                }
            }else if(state.equals("IDLE")){

                if(mState== Recorder.MediaRecorderState.PAUSED){
                    RecordTool.e("phone_rec","3:"+am.getRingerMode());
                    am.setRingerMode(RecordTool.getRingMode(context));
                    RecorderService.startRecording(context);
                    RecordTool.e("phone_rec","4:"+am.getRingerMode());
                }
//                else if(RecordApp.getInstance().getmState()== Recorder.MediaRecorderState.PLAYING_PAUSED){
//                    PlayService.play(context);
//                }
            }
        }
    }

}
