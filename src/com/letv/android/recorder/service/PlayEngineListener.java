package com.letv.android.recorder.service;

public interface PlayEngineListener {
	public void onTrackStart(int miTime,int totalTime);
	public void onTrackChange();
	public void onTrackProgressChange(int miTime);
	public void onTrackPause();
	public void onError();
	public void onStop();
}
