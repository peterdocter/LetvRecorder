package com.letv.android.recorder;

import com.letv.android.recorder.service.Recorder.MediaRecorderState;

import android.app.Application;

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
		return mState;
	}

	public void setmState(MediaRecorderState mState) {
		this.mState = mState;
	}

	@Override
	public void onCreate() {
		instance = this;
        flags = new ArrayList<Long>();
		super.onCreate();
	}

}
