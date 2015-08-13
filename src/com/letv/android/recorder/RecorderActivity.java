package com.letv.android.recorder;

import android.content.Intent;
import android.os.Bundle;
import com.letv.android.recorder.fragment.RecordedFragment;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.tool.RecordTool;

public class RecorderActivity extends AbsRecorderActivity {

    private String TAG=RecorderActivity.class.getSimpleName();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        RecordTool.e(TAG,"onCreate");
        recordedFragment = new RecordedFragment();
		recordedFragment.setShowCallingRecordUI(false);
        getFragmentManager().beginTransaction().replace(R.id.container, recordedFragment).commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
        RecordTool.e(TAG,"onCreate");
	}

	@Override
	public void onBackPressed() {

        RecordTool.e(TAG,"onCreate");
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
