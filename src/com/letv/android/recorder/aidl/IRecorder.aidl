package com.letv.android.recorder.aidl;

import com.letv.android.recorder.aidl.IRecorderCallBack;

interface IRecorder{
	boolean rename(String newName);
	/*
	 *return
	 *  SDCARD_UNMOUNT          =   0x0001;
     *	START_RECORD_SUCCESS    =   0x0002;
     *	CREATE_FILE_FAIL        =   0x0003;
     *  SDCARD_FULL             =   0x0004;
	 *
	 */
	int startRecorder(String recordName);

	boolean stopRecorder();


	void register(IRecorderCallBack callback);
    void unregister(IRecorderCallBack callback);
}