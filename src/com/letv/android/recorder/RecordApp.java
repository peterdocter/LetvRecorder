package com.letv.android.recorder;

import com.letv.android.recorder.exception.CrashHandler;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;

import android.app.Application;
import com.letv.android.recorder.tool.RecordTool;

import java.util.ArrayList;

public class RecordApp extends Application {


	private static RecordApp instance;
	private long startTimeMills = 0;
	
	private String recordName;

    private ArrayList<Long> flags;

    public void addFlag(long flag){
        if(flags.size()==0){
            flags.add(flag);
            return;
        }
        long lastFlag = flags.get(flags.size() - 1);
        if(lastFlag-flag<-1000) {
            flags.add(flag);
        }
    }

    public void deleteFlag(long flag){
        flags.remove(new Long(flag));
    }

    public void clearFlag(){
        flags.clear();
    }

    public ArrayList<Long> getFlags() {
        return flags;
    }

    public void setFlags(ArrayList<Long> flags) {
        this.flags = flags;
    }

    public long getStartTimeMills() {
		return startTimeMills;
	}

	public void setStartTimeMills(long startTimeMills) {
		this.startTimeMills = startTimeMills;
	}

	public String getRecordName() {
		return recordName;
	}

	public void setRecordName(String recordName) {
		this.recordName = recordName;
	}

	private MediaRecorderState mState = MediaRecorderState.IDLE_STATE;
	
	public static  RecordApp getInstance(){
		return instance;
	}
	
	public MediaRecorderState getmState() {
//		return mState;
		return RecordTool.getRecordState(this);
	}


	public void setmState(MediaRecorderState mState) {
		this.mState = mState;
		RecordTool.saveRecordState(this,mState);
	}

	@Override
	public void onCreate() {
		instance = this;
        flags = new ArrayList<Long>();
		CrashHandler.getInstance().init(this);
		super.onCreate();
	}

	private static boolean isActionMode=false;
	public void setActionMode(boolean isActionMode){
		this.isActionMode=isActionMode;
	}
	public boolean isActionMode(){
		return this.isActionMode;
	}
}
