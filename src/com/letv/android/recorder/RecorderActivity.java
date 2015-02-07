package com.letv.android.recorder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.letv.android.recorder.fragment.RecordedFragment;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import android.app.Notification;

public class RecorderActivity extends AbsRecorderActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        recordedFragment = new RecordedFragment();
        recordedFragment.setCallRecordUI(false);
        getFragmentManager().beginTransaction().replace(R.id.container, recordedFragment).commit();

	}


	@Override
	public void onBackPressed() {
		if (RecordApp.getInstance().getmState() != MediaRecorderState.IDLE_STATE) {
			
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addCategory(Intent.CATEGORY_HOME);
			startActivity(intent);
            return;
		}

        if(recordedFragment!=null){
            boolean used =  recordedFragment.onBackPressed();
            if(used){
                return;
            }
        }
		finish();
        super.onBackPressed();

	}

}
