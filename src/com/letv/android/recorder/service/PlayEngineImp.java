package com.letv.android.recorder.service;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.tool.AudioManagerUtil;
import com.letv.android.recorder.tool.RecordTool;

@SuppressLint("HandlerLeak")
public class PlayEngineImp implements PlayEngine, OnCompletionListener, OnErrorListener, OnPreparedListener {
	private static MediaPlayer player;
	private static PlayEngineImp playEngineImp;
	
	private PlayEngineListener pEngineListener;

	
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0) {
				pEngineListener.onTrackProgressChange(player.getCurrentPosition());
				handler.sendEmptyMessageDelayed(0, 50);
			}
		};
	};
	
	public PlayEngineListener getpEngineListener() {
		return pEngineListener;
	}

	public void setpEngineListener(PlayEngineListener pEngineListener) {
		this.pEngineListener = pEngineListener;
	}

	private PlayEngineImp() {

	}

	public static PlayEngineImp getInstance() {

		if (playEngineImp == null) {
			playEngineImp = new PlayEngineImp();
		}

		return playEngineImp;
	}

    private String recordPath;

	@Override
	public void play(String path) {
		try {
			if (player == null) {
				player = new MediaPlayer();
//				player.reset();
				player.setDataSource(path);
				player.setOnCompletionListener(this);
				player.setOnErrorListener(this);
				player.prepare();
                recordPath = path;
			}

            int result = AudioManagerUtil.initPrePlayingAudioFocus(afChangeListener);
            if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
			    player.start();
            }

			RecordApp.getInstance().setmState(MediaRecorderState.PLAYING);
			pEngineListener.onTrackStart(player.getCurrentPosition(), player.getDuration());
			handler.sendEmptyMessage(0);
		} catch (IllegalArgumentException e) {
			// setError(INTERNAL_ERROR);
			player = null;
            recordPath = null;
			return;
		} catch (IOException e) {
			// setError(STORAGE_ACCESS_ERROR);
			player = null;
            recordPath =null;
			return;
		}
		// }
	}


//    public static int initPrePlayingAudioFocus(){
//        AudioManager am = (AudioManager) RecordApp.getInstance().getSystemService(Context.AUDIO_SERVICE);
//        int result = am.requestAudioFocus(afChangeListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
//        return result;
//    }
//
//    public static void destroyAudioFocus(){
//        AudioManager am = (AudioManager) RecordApp.getInstance().getSystemService(Context.AUDIO_SERVICE);
//        am.abandonAudioFocus(afChangeListener);
//    }

    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
                pause();
            }else if(focusChange==AudioManager.AUDIOFOCUS_GAIN){
                if(TextUtils.isEmpty(recordPath)) {
                    play(recordPath);
                }
            }else if(focusChange==AudioManager.AUDIOFOCUS_LOSS){
                stop();
            }else if(focusChange==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
                pause();
            }
        }

    };


	@Override
	public void seekTo(int msecond) {
		if(player!=null&&player.isPlaying()){
			player.seekTo(msecond);
		}
	}

	@Override
	public void stop() {
		RecordTool.e("reboot->", "--------------------->2 play engine 1 :" + "player:" + player + ",state:" + RecordApp.getInstance().getmState());
		if (player != null) {
			player.stop();
			player.release();
			player = null;
		}
        recordPath = null;

		handler.removeMessages(0);
		if (pEngineListener != null) {
			pEngineListener.onTrackStop();
		}
		// mSampleStart = 0;
		PlayService.stopPlay(RecordApp.getInstance());
		RecordApp.getInstance().setmState(MediaRecorderState.PLAY_STOP);
		RecordTool.e("reboot->","--------------------->3 play engine 2:" + RecordApp.getInstance().getmState());
        AudioManagerUtil.destroyAudioFocus(afChangeListener);
	}

	@Override
	public void pause() {
		// mSampleStart = player.getCurrentPosition();
		player.pause();
		if (pEngineListener != null) {
			pEngineListener.onTrackPause();
		}
		handler.removeMessages(0);
		RecordApp.getInstance().setmState(MediaRecorderState.PLAYING_PAUSED);
	}

	@Override
	public void next() {
	}

	@Override
	public void prev() {
	}

	int errorNum = 0;


	@Override
	public void onCompletion(MediaPlayer arg0) {
		stop();
	}

	public boolean isPlaying() {

		return player.isPlaying();
	}

	public int getCurrentPosition() {
		return player.getCurrentPosition();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// TODO Auto-generated method stub
		return false;
	}
}
