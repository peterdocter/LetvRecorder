package com.letv.android.recorder;

import java.io.File;

import android.os.Environment;

public class Constants {
	public static String RECORD_PATH =Environment.getExternalStorageDirectory() + File.separator + "Recorder";

    public static String CALL_RECORD_PATH=Environment.getExternalStorageDirectory() + File.separator + "Recorder/remote";

    public static String TEST_TEMP_FILR_PATH=RECORD_PATH+File.separator+".temp";

	public static String ERROR_LOG_DIR=RECORD_PATH+File.separator+".log";

	public static String RECORD_FORMAT[]={".3gp",".amr"};


	public static String ALERT_ACTION="com.leui.record.alert";
	
	public static int NOTIFICATION_BACK_ID=100001;
	public static int NOTIFICATION_BACK_LED_ID=100002;
    public static final String NEXT_RECORD_INDEX="next_record_index";

	
	public static int SDCARD_UNMOUNT=0x0001;
	public static int START_RECORD_SUCCESS=0x0002;
	public static int CREATE_FILE_FAIL=0x0003;
//	public static int PERMISSION_DENY=0x0004;
    public static int SDCARD_FULL = 0x0004;



}
