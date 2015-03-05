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
		OnClickListener, SensorEventListener,PlayEngineListener {

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

	private  Handler mHandler;
	private  boolean restart=false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mEntry = (RecordEntry) getIntent().getSerializableExtra(RECORD_ENTRY);
		setContentView(R.layout.activity_play);

		curTime = (TextView) findViewById(R.id.current_time);
		totalTime = (TextView) findViewById(R.id.total_time);
		mSeekBar = (RecorderSeekBar) findViewById(R.id.play_seekbar);
		shareBtn = (ImageView) findViewById(R.id.shareBtn);
		playBtn = (ImageView) findViewById(R.id.playBtn);
		editBtn = (ImageView) findViewById(R.id.editBtn);

		curTime.setText(RecordTool.timeFormat(0, "mm:ss"));
		totalTime.setText(RecordTool.timeFormat(mEntry.getRecordDuring(), "mm:ss"));
		mSeekBar.setMax((int) mEntry.getRecordDuring());

		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		sensorManager = (SensorManager) RecordApp.getInstance().getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		mSeekBar.setMax((int) mEntry.getRecordDuring());
		mSeekBar.setFlags(mEntry.getFlags());
		PlayEngineImp.getInstance().setpEngineListener(this);
		PlayService.startPlay(this, mEntry.getFilePath());

		Animation an_edit=AnimationUtils.loadAnimation(PlayRecordActivity.this, R.anim.anim_in_bottom);
		an_edit.setStartOffset(70);
		shareBtn.startAnimation(AnimationUtils.loadAnimation(PlayRecordActivity.this,R.anim.anim_in_bottom));
		editBtn.startAnimation(an_edit);

		findViewById(R.id.empty_part).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				stopPlay();

			}
		});
		mHandler=new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(msg.what==1){
					PlayRecordActivity.this.finish();
				}
				super.handleMessage(msg);
			}
		};
	}

	@Override
	protected void onStop() {
		RecordTool.e("reboot->","--------------------->Play record onStop");
		if (RecordApp.getInstance().getmState()
				== MediaRecorderState.PLAYING) {
			PlayService.pausePlay(this);
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		RecordTool.e("reboot->","--------------------->Play record onDestroy");
		unregisterHeadsetPlugReceiver();
//		unregisterSensorListener();
//		PlayEngineImp.getInstance().setpEngineListener(null);
//		PlayEngineImp.getInstance().stop();


		super.onDestroy();
	}

	@Override
	protected void onResume() {
		RecordTool.e("reboot->","--------------------->Play record onResume");
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
				share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(Intent.createChooser(share, getTitle()));
				break;
			case R.id.playBtn:
				RecordTool.e("reboot->","--------------------->Play record before onClick:"+RecordApp.getInstance().getmState());
				if (RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING) {
					PlayService.pausePlay(this);
				} else if (RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING_PAUSED) {
					PlayService.startPlay(this, mEntry.getFilePath());
				} else {
					PlayService.startPlay(this, mEntry.getFilePath());
				}
				RecordTool.e("reboot->","--------------------->Play record after onClick:"+RecordApp.getInstance().getmState());
				break;
			case R.id.editBtn:
			case R.id.record_title:

				final EditRecordNameDialog mDialog = new EditRecordNameDialog(this);
				mDialog.setPositiveButton(new Dialog.OnClickListener(){
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
							if(file.renameTo(new File(newPath))){
								AbsRecorderActivity.recordedFragment.refreshRecordList();
								FileSyncContentProvider.renameFile(PlayRecordActivity.this,oldPath,newPath);
								mEntry.setFilePath(newPath);
							}
						}
						dialog.dismiss();
					}
				});
				mDialog.setNegativeButton(new Dialog.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {

					}
				});

				mDialog.show(mEntry,true);
				break;
			default:
				break;
		}

	}

	@Override
	public void onBackPressed() {
		RecordTool.e("reboot->","--------------------->Play record onBackPressed");
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
		RecordTool.e("reboot->","--------------------->Play record finish");
		super.finish();
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
	}

	private boolean shouldChangePlayMode(){
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		boolean bluetooth = BluetoothProfile.STATE_CONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET);
		boolean headset = audioManager.isWiredHeadsetOn();
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

	/**
	 * 耳机插拔广播
	 *
	 * @author snile
	 *
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
			public void onTrackStart(int miTime, int totalTime) {

				if(!restart){
					playBtn.setImageResource(R.drawable.frame_record_pause);
					AnimationDrawable am_record=(AnimationDrawable)playBtn.getDrawable();
					am_record.start();
					restart=true;
				}else{
					playBtn.setImageResource(R.drawable.frame_play_pause);
					AnimationDrawable am_record=(AnimationDrawable)playBtn.getDrawable();
					am_record.start();
				}
				mSeekBar.setMax(totalTime);
				curTime.setText(RecordTool.timeFormat(miTime, "mm:ss"));
				PlayRecordActivity.this.totalTime.setText(RecordTool.timeFormat(totalTime, "mm:ss"));
				registerHeadsetPlugReceiver();
				setPlayMode();
//				RecorderAdapter instance = RecorderAdapter.getInstance();
				if(instance!=null){
					instance.notifyDataSetChanged(mEntry);
				}

			}

			@Override
			public void onTrackProgressChange(int miTime) {
				mSeekBar.setProgress(miTime);
				curTime.setText(RecordTool.timeFormat(miTime, "mm:ss"));
			}

			@Override
			public void onTrackPause() {
				speakerMode();
				unregisterHeadsetPlugReceiver();
				playBtn.setImageResource(R.drawable.frame_pause_play);
				AnimationDrawable am_record=(AnimationDrawable)playBtn.getDrawable();
				am_record.start();
				if(instance!=null){
					instance.notifyDataSetChanged(mEntry);
				}
			}

			@Override
			public void onTrackChange() {

			}

			@Override
			public void onTrackStop() {
				speakerMode();
				unregisterHeadsetPlugReceiver();
				playBtn.setImageResource(R.drawable.frame_pause_play);
				AnimationDrawable am_record=(AnimationDrawable)playBtn.getDrawable();
				am_record.start();
				mSeekBar.setProgress(mSeekBar.getMax());
				if(instance!=null){
					instance.notifyDataSetChanged(mEntry);
				}
			}

			@Override
			public void onTrackError() {

			}
//		};
//	}



	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		RecordTool.e("paly mode","传感起变化");
		if (RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING ||
				RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING_PAUSED) {

			float range = event.values[0];
			if (range == sensor.getMaximumRange()) {// 强制使用扬声器
				System.out.println("playing record mode 正常模式");
				audioManager.setMicrophoneMute(false);
				audioManager.setSpeakerphoneOn(true);// 使用扬声器外放，即使已经插入耳机
				// setVolumeControlStream(AudioManager.STREAM_MUSIC);//控制声音的大小
				audioManager.setMode(AudioManager.MODE_NORMAL);
			} else {// 强制使用听筒
				System.out.println("playing record mode 听筒模式");
				audioManager.setSpeakerphoneOn(false);
				// setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

				audioManager.setMode(AudioManager.MODE_IN_CALL);
			}
		}
	}


	public void setPlayMode() {

		if (RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING ||
				RecordApp.getInstance().getmState() == MediaRecorderState.PLAYING_PAUSED) {

			if (SettingTool.getPlayMode(this)== SettingTool.PlayMode.SPEAKER) {// 强制使用扬声器
				speakerMode();
			} else if(SettingTool.getPlayMode(this) == SettingTool.PlayMode.RECEIVER){// 强制使用听筒
				receiverMode();
			}
		}
	}

	public void speakerMode(){
		RecordTool.e("play mode","shouldChangePlayMode:"+shouldChangePlayMode());
		if(!shouldChangePlayMode()){
			return;
		}
		RecordTool.e("play mode", "正常模式");
//		audioManager.setMicrophoneMute(false);
		audioManager.setSpeakerphoneOn(true);// 使用扬声器外放，即使已经插入耳机
		setVolumeControlStream(AudioManager.STREAM_MUSIC);//控制声音的大小
		audioManager.setMode(AudioManager.MODE_NORMAL);
	}

	public void receiverMode(){
		RecordTool.e("play mode","shouldChangePlayMode:"+shouldChangePlayMode());
		if(!shouldChangePlayMode()) {
			return;
		}
		RecordTool.e("play mode","听筒模式");
//		audioManager.setMicrophoneMute(true);
//		audioManager.setRouting(AudioManager.MODE_NORMAL, AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
		audioManager.setMode(AudioManager.MODE_IN_CALL);
		audioManager.setSpeakerphoneOn(false);
	}
	public void stopPlay(){
		RecordTool.e("reboot->","--------------------->1 Play record before stopPlay:"+RecordApp.getInstance().getmState());
		shareBtn.setEnabled(false);
		playBtn.setEnabled(false);
		editBtn.setEnabled(false);
		PlayEngineImp.getInstance().stop();
		PlayEngineImp.getInstance().setpEngineListener(null);
		if(instance!=null){
			instance.notifyDataSetChanged(null);
		}

		Animation am_share = AnimationUtils.loadAnimation(PlayRecordActivity.this, R.anim.anim_out_bottom);
		shareBtn.startAnimation(am_share);

		Animation am_edit = AnimationUtils.loadAnimation(PlayRecordActivity.this, R.anim.anim_out_bottom);
		am_edit.setStartOffset(70);
		editBtn.startAnimation(am_edit);

		RecordTool.e("reboot->","--------------------->4 in  stopPlay after play engine:"+RecordApp.getInstance().getmState());
		if(RecordApp.getInstance().getmState()==MediaRecorderState.PLAYING){
			playBtn.setImageResource(R.drawable.frame_pause_record);
		}else if(RecordApp.getInstance().getmState()==MediaRecorderState.PLAY_STOP){
			playBtn.setImageResource(R.drawable.frame_play_record);
		}

		RecordApp.getInstance().setmState(MediaRecorderState.IDLE_STATE);
		RecordTool.e("reboot->","--------------------->5 in  stopPlay after play engine:"+RecordApp.getInstance().getmState());
		AnimationDrawable am_record=(AnimationDrawable)playBtn.getDrawable();
		am_record.start();


		shareBtn.setVisibility(View.INVISIBLE);
		editBtn.setVisibility(View.INVISIBLE);
		mHandler.removeMessages(1);
		mHandler.sendEmptyMessageDelayed(1,590);
	}
}
