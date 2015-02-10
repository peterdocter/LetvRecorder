package com.letv.android.recorder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.letv.android.recorder.service.Recorder;
import com.letv.android.recorder.service.RecorderService;
import com.letv.android.recorder.tool.RecordTool;

/**
 * Created by snile on 14/11/21.
 */
public class ShutdownReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Recorder.MediaRecorderState state = RecordTool.getRecordState(context);
        RecordTool.e("--------","ShutdownReceiver");
        if(state == Recorder.MediaRecorderState.PAUSED||
                state == Recorder.MediaRecorderState.RECORDING){
            RecorderService.stopRecording(context);
            RecorderService.saveRecording(context,RecordTool.getRecordName(context));
        }
    }
}
