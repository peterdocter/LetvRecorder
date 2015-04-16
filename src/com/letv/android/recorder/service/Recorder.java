package com.letv.android.recorder.service;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.tool.RecordTool;

//import android.media.MediaPlayer;
//import android.media.MediaPlayer.OnCompletionListener;
//import android.media.MediaPlayer.OnErrorListener;

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
		STOPPED, RECORDING, PAUSED, PLAYING, PLAYING_PAUSED,PLAY_STOP, IDLE_STATE;

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
			} else if (state.equalsIgnoreCase("PLAY_STOP")){
				return PLAY_STOP;
			}else{
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
			} else if (state == PLAY_STOP) {
				return "PLAY_STOP";
			}else{
				return null;
			}
		}
	}

	private OnRecordTimeChangedListener timeChangedListener;

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {

            RecordTool.e(TAG,"handleMessage");
			MediaRecorderState state = state();

			if(msg.what==456){
				return;
			}
			if (msg.what == 123) {
				if(state == MediaRecorderState.RECORDING) {
					if (timeChangedListener != null) {
						timeChangedListener.onRecordTimeChanged(RecorderService.recordRealDuring, RecorderService.getDB());
					}
				}
			}

			if(state == MediaRecorderState.RECORDING){
				handler.sendEmptyMessageDelayed(123, 20);
			}
		};
	};
	
	public void beginUpdateTime(){
        RecordTool.e(TAG,"beginUpdateTime");
		handler.sendEmptyMessage(123);
	}
	
	public void endUpdateTime(){
        RecordTool.e(TAG,"endUpdateTime");
		Message message = handler.obtainMessage(456);
		handler.sendMessageAtFrontOfQueue(message);
		handler.removeMessages(123);
	}

	public Recorder(/*Context context*/) {
//		this.mContext = context;
	}

	public void checkRecorderState(){

        RecordTool.e(TAG,"checkRecorderState");
		MediaRecorderState state = state();
		if(MediaRecorderState.PAUSED==state||
				MediaRecorderState.STOPPED==state){
			endUpdateTime();
		}else if(MediaRecorderState.RECORDING==state){
			beginUpdateTime();
		}

		RecordApp.getInstance().setmState(state);
		signalStateChanged(state);
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

		RecordTool.e(TAG, "startRecording");
		RecorderService.startRecording(mContext);
	}

	public void pauseRecording(Context mContext) {
		RecordTool.e(TAG, "pauseRecording");
		if (RecorderService.isRecording()) {
			endUpdateTime();
			RecorderService.pauseRecoring(mContext);
		}
	}

	public void stopRecording(Context mContext) {
		RecordTool.e(TAG, "stopRecording");
		endUpdateTime();
		RecorderService.stopRecording(mContext);
	}

	public MediaRecorderState state() {
		return RecordTool.getRecordState(RecordApp.getInstance());
	}

	public void setState(MediaRecorderState state) {
//		if (state == state())
//			return;
        RecordTool.e(TAG,"setState:"+state);
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

        RecordTool.e(TAG,"signalStateChanged");
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
