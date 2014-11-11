package com.letv.android.recorder.service;


public interface PlayEngine {
	public void play(String path);
	public void seekTo(int msecond);
	public void stop();
	public void pause();
	public void next();
	public void prev();
}
