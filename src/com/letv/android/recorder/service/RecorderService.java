package com.letv.android.recorder.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.os.*;
import android.text.TextUtils;
import android.util.Log;

import com.letv.android.recorder.*;
import com.letv.android.recorder.aidl.IRecorder;
import com.letv.android.recorder.aidl.IRecorderCallBack;
import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.settings.AudioQulityPram;
import com.letv.android.recorder.tool.*;
import com.letv.android.recorder.widget.RecorderAppWidget;
import com.letv.leui.widget.LeTopSlideToastHelper;
import com.letv.leui.widget.ScreenRecordingView;


/**
 *             header  |   sample        | time pre sample
 *  ------------------------------------------------------
 *  AMR-WB:|    9bit   |     33bit       |    20ms
 *  ------------------------------------------------------
 *  AMR-NB:|    6bit   |     32bit       |    20ms
 *
 */

@SuppressLint("SimpleDateFormat")
public class RecorderService extends Service implements RecorderInterface {

    private static String LOG_TAG = RecorderService.class.getName();

	public static String SERVICE_ACTION = "com.letv.recorder.action.Recorder";

	public final static String RECORDER_SERVICE_BROADCAST_NAME = "com.letv.android.recorder.statechange";



	// private static String mFilepath;

	private static String mTempPath;// record temp file path
	private static String mSdCardRecodPath;
	private static String mFileFormat;//
	public static String recordName;
	private List<File> mTmpFiles = new ArrayList<File>();// temp file list
	private int mSegments = 1;//
	private static MediaRecorder mRecorder;
//	private int audioSource = MediaRecorder.AudioSource.MIC;

    private int PRE_RECORD_RINGER_MODE;

	private static boolean isRemoteRecord = false;

	public final static int ACTION_INVALID = 0;

	public final static int ACTION_START_RECORDING = 1;

	public final static int ACTION_STOP_RECORDING = 2;

	public final static int ACTION_PAUSE_RECORDING = 3;

	public final static int ACTION_SAVE_RECORDING = 4;

	public final static int ACTION_DELE_RECORDING = 5;

	public final static String ACTION_NAME = "action_type";

	private MediaRecorderState mRecorderState = MediaRecorderState.IDLE_STATE;

	private static long recordedDuring = 0;

//    private static long recordStartDuring = 0;

	public static long recordRealDuring = 0;

	public static long recordStartTime = 0;

	private final int MAX_TIME_LENGTH = 3600000;// 1 hour

	public float db = 0;

    private RemainingTimeCalculator mRemainingTimeCalculator;

//    public static final int BITRATE_AMR = 2 * 1024 * 8; // bits/sec

    public static final int BITRATE_3GPP = 20 * 1024 * 8; // bits/sec

    private final Handler mHandler = new Handler();

	private KeyguardManager keyguardManager;

	//system Aduio when recording

	private volatile ServiceHandler mServiceHandler;
	private volatile Looper mServiceLooper;
	private String mName="RecorderService";

	private AudioQulityPram audioQulityPram;
	private static Context whichContext;
    RemoteCallbackList<IRecorderCallBack> rc=new RemoteCallbackList<IRecorderCallBack>();

	public static int getDB() {
		int db = 0;// 分贝
		if (mRecorder != null) {
			int ratio = mRecorder.getMaxAmplitude();
			if (ratio > 1) {
                db = ratio;
			}
		}

		return db;
	}

	private Timer mTimer;
	private boolean showNotification = false;


    private Runnable alertStorage = new Runnable() {
        @Override
        public void run() {
			LeTopSlideToastHelper.getToastHelper(getApplicationContext(),LeTopSlideToastHelper.LENGTH_SHORT,
					getResources().getString(R.string.storage_full),null,
					null,null,
					null).show();
        }
    };

	int i=0;
	private void timerStart() {
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {

                recordRealDuring = recordedDuring + (System.currentTimeMillis() - recordStartTime);

				if (recordRealDuring >= MAX_TIME_LENGTH && !showNotification) {
					showNotification = true;
					sendAlertBroadcast();
				}

                boolean isFull = mRemainingTimeCalculator.storageFull();

                if(isFull){
                    stopRecording();
                    saveRecording(RecordApp.getInstance().getRecordName());
                    if(!storageFullCallBack()){
                        mHandler.postDelayed(alertStorage,500);
                    }
                    return;
                }
				i++;
//				boolean locked = keyguardManager.isKeyguardLocked();

//                if(LockScreen.isShowing(RecorderService.this)&& 0==i%50 &&locked){

				if(0==i%50){
					int count=rc.beginBroadcast();
					for (int i=0;i<count;i++){
						try {
							rc.getBroadcastItem(i).updateMaxAmplitude(recordRealDuring,getDB());
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
					rc.finishBroadcast();
                    Intent intent = new Intent(RecorderAppWidget.ACTION_UPDATE);
                    intent.putExtra(ScreenRecordingView.RECORD_TIME_KEY,recordRealDuring);
                    intent.putExtra(ScreenRecordingView.RECORD_DB_KEY,getDB());
                    intent.putExtra(ScreenRecordingView.RECORD_NAME,RecordTool.getRecordName(getApplicationContext()));
                    sendBroadcast(intent);
                }

			}
		}, 20, 20);
	}

	private void timerStop() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
	}

	@Override
	public void onCreate() {
		mSdCardRecodPath = Constants.RECORD_PATH;
		mTempPath = mSdCardRecodPath + File.separator + ".temp";
		mFileFormat = getResources().getString(R.string.record_file_name_format);
        mRemainingTimeCalculator = new RemainingTimeCalculator();

		keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
		super.onCreate();
	}

	public static boolean isRecording() {
		return mRecorder != null;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return iRecorder;
	}

	protected void onHandleIntent(Intent intent) {
		RecordTool.e(LOG_TAG,"onHandleIntent");
		if (intent == null){
			RecordTool.e("IntentService","onHandleIntent");
			return;
		}
		Bundle bundle = intent.getExtras();
		if (bundle != null && bundle.containsKey(ACTION_NAME)) {
			int action = bundle.getInt(ACTION_NAME);
			if (action == ACTION_PAUSE_RECORDING) {
				pauseRecording();
			} else if (action == ACTION_START_RECORDING) {
				startRecording();
				RecordTool.e(LOG_TAG,"onStartCommand startRecording");
			} else if (action == ACTION_STOP_RECORDING) {
				stopRecording();
			} else if (action == ACTION_DELE_RECORDING) {
				deleRecording();
			} else if (action == ACTION_SAVE_RECORDING) {
				saveRecording(recordName);
			}
		}


	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

//		RecordTool.e(LOG_TAG,"onStartCommand onStartCommand 1");
//		if (intent == null)
//			return super.onStartCommand(intent, flags, startId);
//		Bundle bundle = intent.getExtras();
//		if (bundle != null && bundle.containsKey(ACTION_NAME)) {
//			int action = bundle.getInt(ACTION_NAME);
//			if (action == ACTION_PAUSE_RECORDING) {
//				pauseRecording();
//			} else if (action == ACTION_START_RECORDING) {
//				startRecording();
//				RecordTool.e(LOG_TAG,"onStartCommand startRecording");
//			} else if (action == ACTION_STOP_RECORDING) {
//				stopRecording();
//			} else if (action == ACTION_DELE_RECORDING) {
//				deleRecording();
//			} else if (action == ACTION_SAVE_RECORDING) {
//				saveRecording(recordName);
//			}
//			return START_STICKY;
//		}

		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent;
		mServiceHandler.handleMessage(msg);

		return super.onStartCommand(intent, flags, startId);
	}

	private IRecorder.Stub iRecorder = new IRecorder.Stub() {

		@Override
		public boolean stopRecorder() throws RemoteException {
			stopRecording();
			saveRecording(recordName);
			isRemoteRecord = false;
			return true;
		}


		@Override
		public boolean rename(String newName) throws RemoteException {
			recordName = newName;
			isRemoteRecord = true;
			return true;
		}

		/**
		 * 
		 * @param recordName
		 * @return if success return 0 else
		 * @throws RemoteException
		 */
		@Override
		public int startRecorder(String recordName) throws RemoteException {
            if(RecordApp.getInstance().getmState()!=MediaRecorderState.IDLE_STATE) {
                stopRecording();
                saveRecording(RecordApp.getInstance().getRecordName());
                RecordTool.hideNotificationWhenBack(getApplicationContext());
            }
			isRemoteRecord = true;
			RecorderService.recordName = recordName;
			return startRecording();
		}

        @Override
        public void register(IRecorderCallBack callback){
            rc.register(callback);
        }


        @Override
        public void unregister(IRecorderCallBack callback){
            rc.unregister(callback);
        }


	};


    /*
     * Make sure we're not recording music playing in the background, ask
     * the MediaPlaybackService to pause playback.
     */
    private void stopAudioPlayback() {
        // Shamelessly copied from MediaPlaybackService.java, which
        // should be public, but isn't.
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        sendBroadcast(i);
        AudioManager am = (AudioManager) RecordApp.getInstance().getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(null,AudioManager.STREAM_DTMF,AudioManager.AUDIOFOCUS_LOSS);

    }

	@Override
	public int startRecording() {
		RecordTool.e("RecordSer","->startRecording");
		if(whichContext instanceof  SoundRecorder){
			audioQulityPram=SettingTool.getAudioQulity(whichContext);
		}else {
			audioQulityPram=SettingTool.getAudioQulity(this);
		}
		if (!RecordTool.isMounted()) {
			RecordTool.e(this.getClass().getName(), "sdcard is unmounted");

			return Constants.SDCARD_UNMOUNT;
		}
        boolean isFull = mRemainingTimeCalculator.storageFull();
        if(isFull){
            if(!storageFullCallBack()){
                mHandler.postDelayed(alertStorage,500);
            }
            return Constants.SDCARD_FULL;
        }

		String rName = null;
		if (!isRemoteRecord) {
			if (mSegments == 1) {
                rName = RecordTool.getNewRecordName(getApplicationContext());

                RecordApp.getInstance().setRecordName(rName);
				recordName = rName;
				RecordApp.getInstance().setStartTimeMills(System.currentTimeMillis());
                RecordApp.getInstance().clearFlag();
			} else {
				rName = RecordApp.getInstance().getRecordName();
			}
		} else {
			if(mSegments == 1&&TextUtils.isEmpty(recordName)){
				SimpleDateFormat dateFormat = new SimpleDateFormat(mFileFormat);
				Date date = new Date(System.currentTimeMillis());
				rName = dateFormat.format(date);
				recordName = rName;
				RecordApp.getInstance().setRecordName(rName);
                RecordApp.getInstance().clearFlag();
			}else{
				rName = recordName;
			}
		}
		rName = rName + "_" + mSegments + Constants.RECORD_FORMAT[audioQulityPram.OutputFormat/3];

		mRecorderState = MediaRecorderState.RECORDING;
		File file = getFile(mTempPath, rName);
		mTmpFiles.add(file);

		mSegments++;
		if (file.exists()) {
			if (file.delete())
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
		} else {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		mRecorder = new MediaRecorder();
		mRecorder.reset();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		if (mTmpFiles.size() <= 0) {
			return Constants.CREATE_FILE_FAIL;
		}
		mRecorder.setOutputFormat(audioQulityPram.OutputFormat);
		mRecorder.setOutputFile(file.getAbsolutePath());
		mRecorder.setAudioSamplingRate(audioQulityPram.SampleRate);
		mRemainingTimeCalculator.reset();

		mRecorder.setAudioEncodingBitRate(audioQulityPram.EncodeBitrate);
		mRecorder.setAudioChannels(audioQulityPram.AudioChannel);
		mRecorder.setAudioEncoder(audioQulityPram.EncodeType);

		RecordTool.e(LOG_TAG,audioQulityPram.AudioChannel+"-"+audioQulityPram.EncodeBitrate+"-"+audioQulityPram.EncodeType+"-"+audioQulityPram.OutputFormat+"-"+audioQulityPram.SampleRate);
		mRecorder.setOnErrorListener(new OnErrorListener() {
			@Override
			public void onError(MediaRecorder mr, int what, int extra) {
				mRecorder.reset();
                timerStop();
                mRecorder.release();
                mRecorder=null;
                mRecorderState = MediaRecorderState.IDLE_STATE;
                sendStateBroadcast();
				LeTopSlideToastHelper.getToastHelper(getApplicationContext(), LeTopSlideToastHelper.LENGTH_SHORT,
						getResources().getString(R.string.record_error), null,
						null, null,
						null).show();
			}
		});
		try {
            stopAudioPlayback();
			mRecorder.prepare();
			mRecorder.start();
			recordStartTime = System.currentTimeMillis();
			timerStart();
		    sendStateBroadcast();
			AudioManagerUtil.initPrePlayingAudioFocus(null);
		} catch (Exception e) {
			e.printStackTrace();
			LeTopSlideToastHelper.getToastHelper(getApplicationContext(),LeTopSlideToastHelper.LENGTH_SHORT,
					getResources().getString(R.string.record_exception),null,
					null,null,
					null).show();
			mRecorder.release();
			mRecorder = null;
            timerStop();
            mRecorderState = MediaRecorderState.IDLE_STATE;
            sendStateBroadcast();
		}
        RecordTool.loge(LOG_TAG,"startRecording");
		return  Constants.START_RECORD_SUCCESS;
	}


	@Override
	public boolean pauseRecording() {

		RecordTool.loge("RecordSer","->pauseRecording");
		mRecorderState = MediaRecorderState.PAUSED;
		recordedDuring += System.currentTimeMillis() - recordStartTime;
		timerStop();
		if (mRecorder != null) {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}
		sendStateBroadcast();
//        if(!isRemoteRecord){
//            RecordTool.showNotificationWhenBack(getApplicationContext());
//        }
		return true;
	}

	@Override
	public boolean stopRecording() {
		if (MediaRecorderState.RECORDING == mRecorderState) {
			recordRealDuring = recordedDuring + System.currentTimeMillis() - recordStartTime;
		}
		timerStop();
		mRecorderState = MediaRecorderState.IDLE_STATE;
		if (mRecorder != null) {
			mRecorder.stop();
			mRecorder.release();
			mRecorder = null;
		}
		sendStateBroadcast();
		showNotification = false;
		sendAlertBroadcast();
        RecordTool.loge(LOG_TAG,"stopRecording");
		AudioManagerUtil.destroyAudioFocus(null);
		return true;
	}

	@Override
	public boolean deleRecording() {
		if (mTmpFiles == null || mTmpFiles.size() <= 0) {
			return false;
		}

		for (File tmp : mTmpFiles) {
			tmp.delete();
		}
		mTmpFiles.clear();
        clearRecorderData();
		sendStateBroadcast();
        RecordTool.loge(LOG_TAG,"deleRecording");
		return true;
	}



	@Override
	public boolean saveRecording(String recordName) {
		if (mTmpFiles == null || mTmpFiles.size() <= 0) {
			return false;
		}

//        resumeRecorderData();

        RecordTool.loge(LOG_TAG,"saveRecording");

		String tempPath = null;
		if (TextUtils.isEmpty(recordName)) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(mFileFormat);
			Date date = new Date(System.currentTimeMillis());
			tempPath = dateFormat.format(date) + Constants.RECORD_FORMAT[audioQulityPram.OutputFormat/3];
		} else {
            tempPath = recordName;
		}
		
		File finalFile = getFile(isRemoteRecord?Constants.CALL_RECORD_PATH:Constants.RECORD_PATH, tempPath);
		if (!finalFile.exists()) {
			try {
				finalFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
                return false;
			}
		}
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(finalFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < mTmpFiles.size(); i++) {
			File tmpFile = mTmpFiles.get(i);
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(tmpFile);
				byte[] tmpBytes = new byte[fis.available()];
				int length = tmpBytes.length;
				if (i == 0) {
					while (fis.read(tmpBytes) != -1) {
						fileOutputStream.write(tmpBytes, 0, length);
					}
				} else {
					while (fis.read(tmpBytes) != -1) {
						fileOutputStream.write(tmpBytes, 6, length - 6);
					}
				}
				fileOutputStream.flush();
				fis.close();
				if (i == mTmpFiles.size() - 1) {
					RecordEntry entry = new RecordEntry();
					entry.setFilePath(finalFile.getAbsolutePath());
					String fileName = finalFile.getName();
					if(!TextUtils.isEmpty(fileName)){
						if(fileName.endsWith(Constants.RECORD_FORMAT[audioQulityPram.OutputFormat/3])){
							entry.setRecordName(fileName.replace(Constants.RECORD_FORMAT[audioQulityPram.OutputFormat/3], ""));
						}
					}else{
						entry.setRecordName(finalFile.getName());
					}
					entry.setRecordTime(finalFile.lastModified());
					entry.setRecordDuring(RecordDb.getFileDuring(finalFile));
					RecordTool.e(LOG_TAG,"during"+RecordDb.getFileDuring(finalFile));
					entry.setCall(isRemoteRecord);
                    entry.setFlags(RecordApp.getInstance().getFlags());
					RecordDb recordDb = RecordDb.getInstance(getApplicationContext());
					recordDb.insert(entry);
					RecordDb.destroyInstance();
                    FileSyncContentProvider.scanFile(this,finalFile);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				fis = null;
			}
		}
		try {
			if (fileOutputStream != null)
				fileOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			fileOutputStream = null;
		}
		for (File f : mTmpFiles)
			f.delete();
		mTmpFiles.clear();
		clearRecorderData();
		sendStateBroadcast();
		return true;
	}

	private void sendStateBroadcast() {
		Intent intent = new Intent(RECORDER_SERVICE_BROADCAST_NAME);
		intent.putExtra(RecordTool.RECORDER_SERVICE_STATE, MediaRecorderState.getStateString(mRecorderState));
        intent.putExtra(RecordTool.RECORDER_CALL_RECORDING,isRemoteRecord);
		sendBroadcast(intent);
		if(MediaRecorderState.RECORDING == mRecorderState||
				MediaRecorderState.PAUSED == mRecorderState){
			if(SettingTool.isShowScreenWidget(this)&&!LockScreen.isShowing(this)){
				RecordTool.e(LOG_TAG+"sendStateBroadcast","state:"+MediaRecorderState.getStateString(mRecorderState)+":showLock");
				LockScreen.showLockScreenWidght(this);
			}
		}else{
			LockScreen.hideLockScreenWidget(this);
		}

		RecordTool.showNotificationWhenBack(this,mRecorderState);
        if(isRemoteRecord) {
            RecordApp.getInstance().setmState(mRecorderState);
        }
        saveRecorderData();

        if(RecorderAppWidget.hasAppWidget(getApplicationContext())) {
            Intent appWidget = new Intent(RecorderAppWidget.ACTION_UPDATE);
            sendBroadcast(appWidget);
        }
	}

	@Override
	public void onDestroy() {
		LockScreen.hideLockScreenWidget(this);
		timerStop();
		super.onDestroy();
	}

	/**
     * record everything change
     */
    private void saveRecorderData(){

        if(mRecorderState == MediaRecorderState.PAUSED||
               mRecorderState==MediaRecorderState.STOPPED){
            recordedDuring = RecordTool.getRecordedTime(getApplicationContext());
            recordedDuring +=System.currentTimeMillis() - recordStartTime;
            recordStartTime = 0;
        }

        RecordTool.saveRecordState(getApplicationContext(),mRecorderState);
        RecordTool.saveRecordedTime(getApplicationContext(),recordedDuring);
        RecordTool.saveStartRecordTime(getApplicationContext(),recordStartTime);
        RecordTool.saveRecordName(getApplicationContext(),RecordApp.getInstance().getRecordName());
        RecordTool.saveRecordType(getApplicationContext(),isRemoteRecord);
    }

    /**
     * resume data if service crash
     */
    private void resumeRecorderData(){
        recordedDuring = RecordTool.getRecordedTime(getApplicationContext());
        recordStartTime = RecordTool.getStartRecordTime(getApplicationContext());
        recordRealDuring = RecordTool.getRecordTime(getApplicationContext());
        mRecorderState = RecordTool.getRecordState(getApplicationContext());
        isRemoteRecord = RecordTool.getRecordType(getApplicationContext());
        RecordApp.getInstance().setRecordName(RecordTool.getRecordName(getApplicationContext()));
    }

    /**
     * it should clear data when record complete
     */
    private void clearRecorderData(){
        mSegments = 1;
        recordStartTime = 0;
        recordRealDuring = 0;
        recordedDuring = 0;
        mRecorderState = MediaRecorderState.IDLE_STATE;
        RecordApp.getInstance().setRecordName("");
        isRemoteRecord = false;
        saveRecorderData();
    }


	protected void sendAlertBroadcast() {
		Intent intent = new Intent(Constants.ALERT_ACTION);
		intent.putExtra("showAlert", showNotification);
		sendBroadcast(intent);
	}

	private File getFile(String dir, String fileName) {

        if(isRemoteRecord){
            int index = 1;
            String tempName = fileName;
            while(true) {

                File temp = new File(dir, tempName + Constants.RECORD_FORMAT[audioQulityPram.OutputFormat/3]);

                if(temp.exists()){
                    String countStr = getResources().getString(R.string.call_record_filename_xliff);
                    countStr = String.format(countStr,index++);
                    tempName = fileName+countStr;

                }else{
                    fileName = tempName;
                    break;
                }
            }
        }

        fileName = fileName + Constants.RECORD_FORMAT[audioQulityPram.OutputFormat/3];


		File dirFile = new File(dir);
		Log.i("file dir", dir + "      " + fileName);
		if (dirFile.exists() && dirFile.isFile()) {
			dirFile.delete();
			dirFile.mkdirs();
		} else if (!dirFile.exists()) {
			dirFile.mkdirs();
		}

		return new File(dir, fileName);
	}


    // 储存卡满回调。
    protected boolean storageFullCallBack() {
        int i=rc.beginBroadcast();
        boolean isCallback = false;
        while (i > 0) {
            i--;
            try {
                rc.getBroadcastItem(i).storageFull();
                isCallback = true;
            } catch (RemoteException e) {
                e.printStackTrace();
                isCallback =false;
            }
        }
        rc.finishBroadcast();

        return isCallback;
    }

	public static void startRecording(Context context) {
		Intent intent = new Intent(context, RecorderService.class);
		whichContext=context;
		intent.putExtra(ACTION_NAME, ACTION_START_RECORDING);
		context.startService(intent);
		isRemoteRecord = false;
	}

	public static void stopRecording(Context context) {
		Intent intent = new Intent(context, RecorderService.class);
		intent.putExtra(ACTION_NAME, ACTION_STOP_RECORDING);
		context.startService(intent);
		isRemoteRecord = false;
	}

	public static void pauseRecoring(Context context) {
		Intent target = new Intent(context, RecorderService.class);
		target.putExtra(ACTION_NAME, RecorderService.ACTION_PAUSE_RECORDING);
		context.startService(target);
		isRemoteRecord = false;
	}

	public static void saveRecording(Context context, String recordName) {
		Intent target = new Intent(context, RecorderService.class);
		target.putExtra(ACTION_NAME, RecorderService.ACTION_SAVE_RECORDING);
		context.startService(target);
		RecorderService.recordName = recordName;
		isRemoteRecord = false;
	}

	public static void deleteRecording(Context context) {
		Intent target = new Intent(context, RecorderService.class);
		target.putExtra(ACTION_NAME, RecorderService.ACTION_DELE_RECORDING);
		context.startService(target);
		isRemoteRecord = false;
	}
	
	private boolean checkPermission() {
        AppOpsManager appOps = (AppOpsManager) ActivityThread.currentApplication().
            getSystemService(Context.APP_OPS_SERVICE);
        int callingUid = Binder.getCallingUid();
        String callingPackage= ActivityThread.currentPackageName();
        if (appOps.noteOp(AppOpsManager.OP_RECORD_AUDIO, callingUid, callingPackage) ==
            AppOpsManager.MODE_ALLOWED)
            return true;
        else
            return false;
    }

	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			onHandleIntent((Intent)msg.obj);
		}
	}
}
