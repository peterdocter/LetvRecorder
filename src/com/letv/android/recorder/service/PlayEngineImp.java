package com.letv.android.recorder.service;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.tool.AudioManagerUtil;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.android.recorder.tool.SettingTool;

import java.io.IOException;

@SuppressLint("HandlerLeak")
public class PlayEngineImp implements PlayEngine, OnCompletionListener, OnErrorListener, OnPreparedListener {
    private static MediaPlayer player;
    private static PlayEngineImp playEngineImp;

    private PlayEngineListener pEngineListener;


    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 0) {
                //pEngineListener.onTrackProgressChange(player.getCurrentPosition());
                //handler.sendEmptyMessageDelayed(0, 50);
                //makeCountdownTimerChangeProgress();
            }
        }

        ;
    };

    private CountDownTimer changeProgressTimer;

    private void makeCountdownTimerChangeProgress() {
        if (player != null) {

            int countDownInterval = 50;

            if (player.getDuration() > 3 * 60 * 1000) {
                countDownInterval = 200;
            }

            changeProgressTimer = new CountDownTimer(Integer.MAX_VALUE, countDownInterval) {

                @Override
                public void onTick(long millisUntilFinished) {
                    if(player!=null&&pEngineListener!=null) {
                        int iPos = player.getCurrentPosition();
                        //Log.v("Recorder", "Current Position : " + iPos + "  Duration : " + player.getDuration());
                        pEngineListener.onTrackProgressChange(iPos);
                    }
                }

                @Override
                public void onFinish() {
                }
            };
            changeProgressTimer.start();
        }
    }


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
            synchronized (PlayEngineImp.class) {
                if (playEngineImp == null) {
                    playEngineImp = new PlayEngineImp();
                }
            }
        }

        return playEngineImp;
    }

    private String recordPath;

    @Override
    public void play(String path) {
        //request audio focus
        int result = AudioManagerUtil.initPrePlayingAudioFocus(afChangeListener);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            return;
        }

        try {
            if (player == null) {
                player = new MediaPlayer();
//				player.reset();
                player.setDataSource(path);
                player.setOnCompletionListener(this);
                player.setOnErrorListener(this);
                if (SettingTool.getPlayMode(RecordApp.getInstance().getApplicationContext()) == SettingTool.PlayMode.RECEIVER) {
                    player.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                }
                player.prepare();
                recordPath = path;
            }

            player.start();

            RecordApp.getInstance().setmState(MediaRecorderState.PLAYING);
            if (pEngineListener != null) {
                pEngineListener.onTrackStart(player.getCurrentPosition(), player.getDuration());
            }
            //handler.sendEmptyMessage(0);
            makeCountdownTimerChangeProgress();
        } catch (IllegalArgumentException e) {
            // setError(INTERNAL_ERROR);
            player = null;
            recordPath = null;
            return;
        } catch (IOException e) {
            // setError(STORAGE_ACCESS_ERROR);
            player = null;
            recordPath = null;
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
            Log.v("Recorder", "onAudioFocusChange : " + focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if (!TextUtils.isEmpty(recordPath)) {
                    play(recordPath);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                pause();
            }
        }

    };


    @Override
    public void seekTo(int msecond) {
        if (player != null) {
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

        //handler.removeMessages(0);
        if(changeProgressTimer!=null) {
            changeProgressTimer.cancel();
        }
        if (pEngineListener != null) {
            pEngineListener.onTrackStop();
        }
        // mSampleStart = 0;
        PlayService.stopPlay(RecordApp.getInstance());
        RecordApp.getInstance().setmState(MediaRecorderState.PLAY_STOP);
        RecordTool.e("reboot->", "--------------------->3 play engine 2:" + RecordApp.getInstance().getmState());
        AudioManagerUtil.destroyAudioFocus(afChangeListener);
    }

    @Override
    public void pause() {
        // mSampleStart = player.getCurrentPosition();
        player.pause();
        if (pEngineListener != null) {
            pEngineListener.onTrackPause();
        }
        //handler.removeMessages(0);
        if(changeProgressTimer!=null) {
            changeProgressTimer.cancel();
        }
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
        Log.e("MyMediaPlayer","onCompletion...");
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
