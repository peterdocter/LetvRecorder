package com.letv.android.recorder.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.letv.android.recorder.R;
import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.android.recorder.widget.RecordingView;

public class RecordedFragment2 extends RecordedFragment {

	private View rootView = null;
	private ViewFlipper recordVF;
	private TextView recordTime;
	private TextView recordStartTime;
	private TextView recordName;
	private RecordingView recordingView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_recorded, container, false);
		recordVF = (ViewFlipper) rootView.findViewById(R.id.recordVF);
		recordTime = (TextView) rootView.findViewById(R.id.record_time);
		recordStartTime = (TextView) rootView.findViewById(R.id.record_start_time);
		recordName = (TextView) rootView.findViewById(R.id.record_title);
		recordingView = (RecordingView) rootView.findViewById(R.id.recording_view);
		recordVF.setDisplayedChild(2);
		return rootView;
	}

	/**
	 * 
	 * @param recordTimeMillis
	 */
	public void updateRecordTimeUI(long recordTimeMillis, float db) {
		if(!isDetached()){
			if (recordTime != null) {
				recordTime.setText(RecordTool.recordTimeFormat(recordTimeMillis));
			}
			
			if(recordStartTime!=null){
				recordStartTime.setText(RecordTool.getStartTimeStr());
			}
			
			if(recordName!=null){
				recordName.setText(RecordApp.getInstance().getRecordName());
			}
			
			if (recordingView != null) {
				recordingView.updateRecordUI(recordTimeMillis, db);
			}
		}
		
	}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void stopRecording() {
		if (!isDetached() && recordingView != null) {
			recordingView.stopRecording();
		}
	}

	public void startRecording() {
		if (!isDetached() && recordingView != null) {
			recordingView.startRecording();
		}
	}

	protected void setEnabled(View view,boolean enable){
		if(view instanceof ViewGroup){
			view.setEnabled(enable);
			for(int i=0;i<((ViewGroup)view).getChildCount();i++){
				setEnabled(((ViewGroup)view).getChildAt(i), enable);
			}
		}else{
			view.setEnabled(enable);
		}
	}

}
