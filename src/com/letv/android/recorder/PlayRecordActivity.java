package com.letv.android.recorder;

import java.io.File;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import android.widget.Toast;
import com.letv.android.recorder.fragment.RecorderAdapter;
import com.letv.android.recorder.provider.ProviderTool;
import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.receiver.RecordFileObserver;
import com.letv.android.recorder.service.PlayEngineImp;
import com.letv.android.recorder.service.PlayEngineListener;
import com.letv.android.recorder.service.PlayService;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.tool.FileSyncContentProvider;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.android.recorder.tool.SettingTool;
import com.letv.android.recorder.widget.EditRecordNameDialog;
import com.letv.android.recorder.widget.RecorderSeekBar;
import com.letv.leui.widget.LeTopSlideToastHelper;

public class PlayRecordActivity extends Activity implements
        OnClickListener, SensorEventListener, PlayEngineListener, RecordFileObserver.OnRecordFileChangeListener {
    private final String TAG = "PlayRecordActivity";
    public static final String RECORD_ENTRY = "record_entry";
    private RecordEntry mEntry;

    private TextView curTime, totalTime;
    private RecorderSeekBar mSeekBar;
    private ImageView shareBtn, playBtn, editBtn;

    private HeadsetPlugReceiver mHeadset;

    private AudioManager audioManager;
    private SensorManager sensorManager;
    private Sensor sensor;

    private RecorderAdapter instance = RecorderAdapter.getInstance();

    private boolean restart = false;
    private long mOldItime, mTimeOffset;
    private RecordFileObserver fileObserver;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                PlayRecordActivity.this.finish();
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEntry = (RecordEntry) getIntent().getSerializableExtra(RECORD_ENTRY);
        setContentView(R.layout.activity_play);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        findView();

        initView();

        initPlayService();

        playAnim();

        initListener();
    }

    private void initListener() {
        findViewById(R.id.empty_part).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlay();
            }
        });

        //init file observer,when file was deleted,stop it
        fileObserver = new RecordFileObserver(this, this);
        fileObserver.startWatching();
    }

    private void playAnim() {
        Animation an_edit = AnimationUtils.loadAnimation(PlayRecordActivity.this, R.anim.anim_in_bottom);
        an_edit.setStartOffset(70);
        shareBtn.startAnimation(AnimationUtils.loadAnimation(PlayRecordActivity.this, R.anim.anim_in_bottom));
        editBtn.startAnimation(an_edit);
    }

    private void initPlayService() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        sensorManager = (SensorManager) RecordApp.getInstance().getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSeekBar.setMax((int) mEntry.getRecordDuring());
        mSeekBar.setFlags(mEntry.getFlags());
        PlayEngineImp.getInstance().setpEngineListener(this);
        PlayService.startPlay(this, mEntry.getFilePath());
    }

    private void initView() {
        curTime.setText(RecordTool.recordTimeFormat(0));
        RecordTool.e(TAG, "onCreate:" + RecordTool.timeFormat(0, "mm:ss"));
        totalTime.setText(RecordTool.recordTimeFormat(mEntry.getRecordDuring()));
        mSeekBar.setMax((int) mEntry.getRecordDuring());
    }

    private void findView() {
        curTime = (TextView) findViewById(R.id.current_time);
        totalTime = (TextView) findViewById(R.id.total_time);
        mSeekBar = (RecorderSeekBar) findViewById(R.id.play_seekbar);
        shareBtn = (ImageView) findViewById(R.id.shareBtn);
        playBtn = (ImageView) findViewById(R.id.playBtn);
        editBtn = (ImageView) findViewById(R.id.editBtn);
    }

    @Override
    protected void onStop() {
        RecordTool.e(TAG, TAG + ":onStop");
//		if (RecordApp.getInstance().getmState()== MediaRecorderState.PLAYING) {
//			PlayService.pausePlay(this);
//		}
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        RecordTool.e(TAG, TAG + "onDestroy");
        unregisterHeadsetPlugReceiver();
//		unregisterSensorListener();
//		PlayEngineImp.getInstance().setpEngineListener(null);
//		PlayEngineImp.getInstance().stop();
        stopPlay();
        //stop watching file
        fileObserver.stopWatching();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        RecordTool.e(TAG, TAG + "onResume");
        shareBtn.setOnClickListener(this);
        playBtn.setOnClickListener(this);
        editBtn.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(getChangeListener());
//		if(RecordApp.getInstance().getmState()
//				==MediaRecorderState.PLAYING_PAUSED){
//			PlayService.startPlay(this,mEntry.getFilePath());
//		}
        super.onResume();
    }

    private OnSeekBarChangeListener getChangeListener() {
        return new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mOldItime = -1;
                    mTimeOffset = 0;
                    PlayEngineImp.getInstance().seekTo(progress);
                }
            }
        };
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.shareBtn:
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("audio/*");
                share.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share));
                share.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.record_files));
                share.putExtra(Intent.EXTRA_STREAM, ProviderTool.getShareUri(mEntry.getFilePath()));
//                share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(share);
                break;
            case R.id.playBtn:
                RecordTool.e(TAG, "Play record before onClick:" + RecordApp.getInstance().getmState());
                if (RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING) {
                    PlayService.pausePlay(this);
                } else if (RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING_PAUSED) {
                    PlayService.startPlay(this, mEntry.getFilePath());
                } else {
                    PlayService.startPlay(this, mEntry.getFilePath());
                }
                RecordTool.e(TAG, "Play record after onClick:" + RecordApp.getInstance().getmState());
                break;
            case R.id.editBtn:
            case R.id.record_title:

                final EditRecordNameDialog mDialog = new EditRecordNameDialog(this);
                mDialog.setPositiveButton(new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File file = new File(mEntry.getFilePath());
                        String fileName = RecordTool.getRecordName(mEntry.getFilePath());
                        if (fileName.equalsIgnoreCase(mDialog.getText())) {
                            LeTopSlideToastHelper.getToastHelper(PlayRecordActivity.this, LeTopSlideToastHelper.LENGTH_SHORT,
                                    getResources().getString(R.string.no_change_recordname), null,
                                    null, null,
                                    null).show();
                        } else if (RecordTool.canSave(PlayRecordActivity.this, mDialog.getText())) {
                            String oldPath = mEntry.getFilePath();
                            String newPath = mEntry.getFilePath().replace(fileName, mDialog.getText());

                            RecordDb recordDb = RecordDb.getInstance(PlayRecordActivity.this);
                            recordDb.update(oldPath, newPath);
                            RecordDb.destroyInstance();
                            if (file.renameTo(new File(newPath))) {
                                AbsRecorderActivity.recordedFragment.refreshRecordList();
                                FileSyncContentProvider.renameFile(PlayRecordActivity.this, oldPath, newPath);
                                mEntry.setFilePath(newPath);
                            }
                        }
                        dialog.dismiss();
                    }
                });
                mDialog.setNegativeButton(new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                mDialog.show(mEntry, true);
                break;
            default:
                break;
        }

    }

    @Override
    public void onBackPressed() {
        RecordTool.e(TAG, "Play record onBackPressed");
        stopPlay();
    }

    @Override
    protected void onActivityResult(int arg0, int arg1, Intent arg2) {
        if (arg0 == 12345 && arg1 == RESULT_OK) {
            if (arg2 != null && arg2.hasExtra(SaveRecordActivity.DATA_CHANGED)) {
                if (arg2.getBooleanExtra(SaveRecordActivity.DATA_CHANGED, false)) {
                    finish();
                }
            }
        }
        super.onActivityResult(arg0, arg1, arg2);
    }


    @Override
    public void finish() {
        RecordTool.e(TAG, "Play record finish");
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private boolean shouldChangePlayMode() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        boolean bluetooth = BluetoothProfile.STATE_CONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET);
        boolean headset = audioManager.isWiredHeadsetOn();
        RecordTool.e(TAG, "playmode：bluetooth:" + bluetooth + " headset:" + headset);
        return !bluetooth && !headset;
    }

    private void registerHeadsetPlugReceiver() {
        mHeadset = new HeadsetPlugReceiver();
        IntentFilter filter = new IntentFilter("android.intent.action.HEADSET_PLUG");
        registerReceiver(mHeadset, filter);
    }

    private void unregisterHeadsetPlugReceiver() {
        if (mHeadset != null) {
            try {
                unregisterReceiver(mHeadset);
            } catch (Exception e) {
//				e.printStackTrace();
            }
        }
    }

    @Override
    public void onDeleted(String path) {
        if (mEntry.getFilePath().equals(path)) {
            stopPlay();
        }
    }

    /**
     * 耳机插拔广播
     *
     * @author snile
     */
    class HeadsetPlugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.hasExtra("state")) {
                if (arg1.getIntExtra("state", 0) == 0) {
                    System.out.println("耳机拔掉了");
//					registerSensorListener();
                } else if (arg1.getIntExtra("state", 0) == 1) {
                    System.out.println("耳机插上了");
//					unregisterSensorListener();
                }
            }
        }
    }

//	private PlayEngineListener getPlayListener() {
//		return new PlayEngineListener() {

    @Override
    public void onTrackStart(final int miTime, final int totalTime) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!restart) {
                    playBtn.setImageResource(R.drawable.frame_record_pause);
                    AnimationDrawable am_record = (AnimationDrawable) playBtn.getDrawable();
                    am_record.start();
                    restart = true;
                } else {
                    playBtn.setImageResource(R.drawable.frame_play_pause);
                    AnimationDrawable am_record = (AnimationDrawable) playBtn.getDrawable();
                    am_record.start();
                }
                mSeekBar.setMax(totalTime);
                RecordTool.e(TAG, "1:onTrackStart:totalTime" + totalTime + ":miTime:" + miTime);
                curTime.setText(RecordTool.timeFormat(miTime, "mm:ss"));
                PlayRecordActivity.this.totalTime.setText(RecordTool.recordTimeFormat(totalTime));
                registerHeadsetPlugReceiver();
                setPlayMode();
//				RecorderAdapter instance = RecorderAdapter.getInstance();
                if (instance != null) {
                    instance.notifyDataSetChanged(mEntry);
                }
            }
        });

    }

    @Override
    public void onTrackProgressChange(final int miTime) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int max = mSeekBar.getMax();
                //if (mOldItime >= miTime) {
                //    mTimeOffset += max>=3*60*1000?200:50;
                //}
                //miTime += mTimeOffset;
                //mOldItime = miTime;
                RecordTool.e(TAG, "onTrackProgressChange" + miTime);
                //Log.e("onProgressChanged","progress=="+miTime);
                //if (miTime >= max) {
                //    miTime = max;
                //}
                mSeekBar.setProgress(miTime);
                curTime.setText(RecordTool.recordTimeFormat(miTime));
            }
        });
    }

    @Override
    public void onTrackPause() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecordTool.e(TAG, "onTrackPause");
                receiverMode();
                unregisterHeadsetPlugReceiver();
                playBtn.setImageResource(R.drawable.frame_pause_play);
                AnimationDrawable am_record = (AnimationDrawable) playBtn.getDrawable();
                am_record.start();
                if (instance != null) {
                    instance.notifyDataSetChanged(mEntry);
                }
            }
        });
    }

    @Override
    public void onTrackChange() {

    }

    @Override
    public void onTrackStop() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecordTool.e(TAG, "onTrackStop");
                //speakerMode();
                receiverMode();
                unregisterHeadsetPlugReceiver();
                playBtn.setImageResource(R.drawable.frame_pause_play);
                AnimationDrawable am_record = (AnimationDrawable) playBtn.getDrawable();
                am_record.start();
                mSeekBar.setProgress(mSeekBar.getMax());
                if (instance != null) {
                    instance.notifyDataSetChanged(mEntry);
                }
                mOldItime = -1;
                mTimeOffset = 0;

            }
        });

    }

    @Override
    public void onTrackError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PlayRecordActivity.this.finish();
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        RecordTool.e(TAG, "paly mode传感起变化");
        if (RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING ||
                RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING_PAUSED) {

            float range = event.values[0];
            if (range == sensor.getMaximumRange()) {// 强制使用扬声器
                RecordTool.e(TAG, "onSensorChanged:playing record mode 正常模式");
                audioManager.setMicrophoneMute(false);
                audioManager.setSpeakerphoneOn(true);// 使用扬声器外放，即使已经插入耳机
                // setVolumeControlStream(AudioManager.STREAM_MUSIC);//控制声音的大小
                audioManager.setMode(AudioManager.MODE_NORMAL);
            } else {// 强制使用听筒
                RecordTool.e(TAG, "onSensorChanged:playing record mode 听筒模式");
                audioManager.setSpeakerphoneOn(false);
                // setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                audioManager.setMode(AudioManager.MODE_IN_CALL);
            }
        }
    }


    public void setPlayMode() {

        if (RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING ||
                RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING_PAUSED) {

            if (SettingTool.getPlayMode(this) == SettingTool.PlayMode.SPEAKER) {// 强制使用扬声器
                speakerMode();
            } else if (SettingTool.getPlayMode(this) == SettingTool.PlayMode.RECEIVER) {// 强制使用听筒
                receiverMode();
            }
        }
    }

    public void speakerMode() {
        // 08.12 默认为扬声器
//        RecordTool.e(TAG, "shouldChangePlayMode:" + shouldChangePlayMode());
//        if (!shouldChangePlayMode()) {
//            return;
//        }
//        RecordTool.e(TAG, "正常模式");
////		audioManager.setMicrophoneMute(false);
//        audioManager.setSpeakerphoneOn(true);// 使用扬声器外放，即使已经插入耳机
////        setVolumeControlStream(AudioManager.STREAM_MUSIC);//控制声音的大小
//        audioManager.setMode(AudioManager.MODE_NORMAL);
    }

    public void receiverMode() {
        // 08.12 关闭听筒播放
//        RecordTool.e(TAG, "shouldChangePlayMode:" + shouldChangePlayMode());
//        if (!shouldChangePlayMode()) {
//            return;
//        }
//        RecordTool.e(TAG, "听筒模式");
////		audioManager.setMicrophoneMute(true);
////		audioManager.setRouting(AudioManager.MODE_NORMAL, AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
////        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
//        audioManager.setMode(AudioManager.MODE_IN_CALL);
//        audioManager.setSpeakerphoneOn(false);
    }

    public void stopPlay() {
        if (!RecordTool.canClick(1000)) {
            return;
        }
        RecordTool.e(TAG, "stopPlay:state:" + RecordApp.getInstance().getmState());
        shareBtn.setEnabled(false);
        playBtn.setEnabled(false);
        editBtn.setEnabled(false);
        PlayEngineImp.getInstance().setpEngineListener(null);
        PlayEngineImp.getInstance().stop();
        if (instance != null) {
            instance.notifyDataSetChanged(null);
        }

        Animation am_share = AnimationUtils.loadAnimation(PlayRecordActivity.this, R.anim.anim_out_bottom);
        shareBtn.startAnimation(am_share);

        Animation am_edit = AnimationUtils.loadAnimation(PlayRecordActivity.this, R.anim.anim_out_bottom);
        am_edit.setStartOffset(70);
        editBtn.startAnimation(am_edit);

        RecordTool.e(TAG, "stopPlay:state:" + RecordApp.getInstance().getmState());
        if (RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING) {
            playBtn.setImageResource(R.drawable.frame_pause_record);
        } else if (RecordApp.getInstance().getmState() == MediaRecorderState.PLAY_STOP) {
//            playBtn.setImageResource(R.drawable.frame_play_record);
        }

        RecordApp.getInstance().setmState(MediaRecorderState.IDLE_STATE);
        RecordTool.e(TAG, "stopPlay:state:" + RecordApp.getInstance().getmState());
//        AnimationDrawable am_record = (AnimationDrawable) playBtn.getDrawable();
//        am_record.start();


        shareBtn.setVisibility(View.INVISIBLE);
        editBtn.setVisibility(View.INVISIBLE);
        mHandler.removeMessages(1);
        mHandler.sendEmptyMessageDelayed(1, 490);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //isConfigurationChanged=true;
        restartActivity();
    }

    private void restartActivity() {
        //stopPlay();
        //Intent intent = new Intent(this,RecorderActivity.class);
        finish();
        //startActivity(intent);
    }


}
