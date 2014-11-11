package com.letv.android.recorder.service;

public interface RecorderInterface {

	int startRecording();

	boolean pauseRecording();

	boolean stopRecording();
	
	boolean deleRecording();
	
	/**
	 * if recordName is null or empty  ,use default name "录音-yyMMdd-HH-mm-ss"
	 * @param recordName
	 */
	boolean saveRecording(String recordName);

}
