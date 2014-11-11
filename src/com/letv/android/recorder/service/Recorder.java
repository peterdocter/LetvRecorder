package com.letv.android.recorder.service;

import android.content.Context;
//import android.media.MediaPlayer;
//import android.media.MediaPlayer.OnCompletionListener;
//import android.media.MediaPlayer.OnErrorListener;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.letv.android.recorder.RecordApp;

public class Recorder{

	private String TAG = Recorder.class.getSimpleName();

//	private long mSampleStart = 0; // time at which latest record or play

//	public static long mSampleLength = 0; // length of current sample

//	private Context mContext;
	
	private OnStateChangedListener mOnStateChangedListener = null;
	
	private static Recorder mRecorder;
	
	public static Recorder getInstance(){
		if(mRecorder==null){
			mRecorder = new Recorder();
		}
		return mRecorder;
	}

	public enum MediaRecorderState {
		STOPPED, RECORDING, PAUSED, PLAYING, PLAYING_PAUSED, IDLE_STATE;

        public static final String KEY="MediaRecorderState";

		public static MediaRecorderState getState(String state) {
			if (TextUtils.isEmpty(state)) {
				return IDLE_STATE;
			} else if (state.equalsIgnoreCase("STOPPED")) {
				return STOPPED;
			} else if (state.equalsIgnoreCase("RECORDING")) {
				return RECORDING;
			} else if (state.equalsIgnoreCase("PAUSED")) {
				return PAUSED;
			} else if (state.equalsIgnoreCase("PLAYING")) {
				return PLAYING;
			} else if (state.equalsIgnoreCase("PLAYING_PAUSED")) {
				return PLAYING_PAUSED;
			} else {
				return IDLE_STATE;
			}
		}

		public static String getStateString(MediaRecorderState state) {
			if (state == null) {
				return null;
			} else if (state == IDLE_STATE) {
				return "IDLE_STATE";
			} else if (state == STOPPED) {
				return "STOPPED";
			} else if (state == RECORDING) {
				return "RECORDING";
			} else if (state == PAUSED) {
				return "PAUSED";
			} else if (state == PLAYING) {
				return "PLAYING";
			} else if (state == PLAYING_PAUSED) {
				return "PLAYING_PAUSED";
			} else {
				return null;
			}
		}
	}

	private OnRecordTimeChangedListener timeChangedListener;

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 123) {
				if (timeChangedListener != null) {
					timeChangedListener.onRecordTimeChanged(RecorderService.recordRealDuring,RecorderService.getDB());
				}
			}
			
			if(state() == MediaRecorderState.RECORDING){
				handler.sendEmptyMessageDelayed(123, 20);
			}
		};
	};
	
	public void beginUpdateTime(){
		handler.sendEmptyMessage(123);
	}
	
	public void endUpdateTime(){
		handler.removeMessages(123);
	}

	public Recorder(/*Context context*/) {
//		this.mContext = context;
	}

	public OnStateChangedListener getmOnStateChangedListener() {
		return mOnStateChangedListener;
	}

	public void setmOnStateChangedListener(OnStateChangedListener mOnStateChangedListener) {
		this.mOnStateChangedListener = mOnStateChangedListener;
	}

	public int progress() {
		if (state() == MediaRecorderState.RECORDING) {
			return (int) ((/*System.currentTimeMillis() - mSampleStart*/RecorderService.recordRealDuring) / 1000);
		}

		return 0;
	}

	public void startRecording(Context mContext) {
		Log.i(TAG, "startRecording");
		RecorderService.startRecording(mContext);
	}

	public void pauseRecording(Context mContext) {
		Log.i(TAG, "pauseRecording");
		if (RecorderService.isRecording()) {
			RecorderService.pauseRecoring(mContext);
		}
	}

	public void stopRecording(Context mContext) {
		Log.i(TAG, "stopRecording");
		RecorderService.stopRecording(mContext);
	}

	public MediaRecorderState state() {
		return RecordApp.getInstance().getmState();
	}

	public void setState(MediaRecorderState state) {
		if (state == state())
			return;

		if(MediaRecorderState.PAUSED==state||
			MediaRecorderState.STOPPED==state){
			endUpdateTime();
		}else if(MediaRecorderState.RECORDING==state){
			beginUpdateTime();
		}
		
		RecordApp.getInstance().setmState(state);
		signalStateChanged(state);
	}

	private void signalStateChanged(MediaRecorderState state) {
		if (mOnStateChangedListener != null)
			mOnStateChangedListener.onStateChanged(state);
	}

	public void setError(int error) {
		if (mOnStateChangedListener != null)
			mOnStateChangedListener.onError(error);
	}

	public OnRecordTimeChangedListener getTimeChangedListener() {
		return timeChangedListener;
	}

	public void setTimeChangedListener(OnRecordTimeChangedListener timeChangedListener) {
		this.timeChangedListener = timeChangedListener;
	}

	public interface OnStateChangedListener {
		public void onStateChanged(MediaRecorderState state);

		public void onError(int error);
	}

	public interface OnRecordTimeChangedListener {
		public void onRecordTimeChanged(long timeMillils,float db);
	}
}
