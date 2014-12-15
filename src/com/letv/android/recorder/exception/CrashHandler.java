package com.letv.android.recorder.exception;

import android.content.Context;
import android.os.Environment;
import com.letv.android.recorder.Constants;
import com.letv.android.recorder.service.Recorder;
import com.letv.android.recorder.service.RecorderService;
import com.letv.android.recorder.tool.RecordTool;

import java.io.*;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrashHandler implements UncaughtExceptionHandler {
	private Context context;
	
	private CrashHandler() {}

	private static class CrashHolder {
		static final CrashHandler crashHandler = new CrashHandler();
	}

	public static CrashHandler getInstance() {
		return CrashHolder.crashHandler;
	}

	public void init(Context context) {
		Thread.setDefaultUncaughtExceptionHandler(this);
		this.context = context;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {

		ex.printStackTrace();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ex.printStackTrace(new PrintStream(baos));

		if(Recorder.MediaRecorderState.RECORDING==RecordTool.getRecordState(context)||
				Recorder.MediaRecorderState.PAUSED==RecordTool.getRecordState(context)){
			RecorderService.pauseRecoring(context);
			RecorderService.stopRecording(context);
		}

		RecordTool.saveRecordState(context, Recorder.MediaRecorderState.IDLE_STATE);


		try {
			FileWriter writer = new FileWriter(creatLogFile());
			writer.write(baos.toString());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		android.os.Process.killProcess(android.os.Process.myPid());
		System.exit(0);

	}

	public File creatLogFile() {

		Date date = new Date(System.currentTimeMillis());
		SimpleDateFormat sp = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
		String timeStr= sp.format(date);

		String path = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + Constants.ERROR_LOG_DIR;
		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}
		File fileLog = new File(path, timeStr + ".txt");

		try {
			fileLog.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileLog;
	}

}
