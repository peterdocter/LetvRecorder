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
import android.app.*;
import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.os.*;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;

import com.letv.android.recorder.*;
import com.letv.android.recorder.aidl.IRecorder;
import com.letv.android.recorder.aidl.IRecorderCallBack;
import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.settings.AudioQulityPram;
import com.letv.android.recorder.tool.*;
import com.letv.android.recorder.tool.feature.FeatureUtil;
import com.letv.android.recorder.widget.RecorderAppWidget;
import com.letv.leui.widget.LeTopSlideToastHelper;
import com.letv.leui.widget.ScreenRecordingView;


import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;


/**
 * header  |   sample        | time pre sample
 * ------------------------------------------------------
 * AMR-WB:|    9bit   |     33bit       |    20ms
 * ------------------------------------------------------
 * AMR-NB:|    6bit   |     32bit       |    20ms
 */

@SuppressLint("SimpleDateFormat")
public class RecorderService extends Service implements RecorderInterface {

    private static String LOG_TAG = RecorderService.class.getSimpleName();

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
    private String mName = "RecorderService";

    private RecorderAppWidget mAppWidgetProvider = RecorderAppWidget.getInstance();
    private AudioQulityPram audioQulityPram;
    private static Context whichContext;

    //widget refresh field

    private Timer mTimer;
    private int mTimeCount = 0;
    private TimerTask mTimeCountTimerTask;
    RemoteViews mRemoteViews;

    AppWidgetManager mAppWidgetManager;

    ComponentName mComponentName;
    PowerManager pm;

    RemoteCallbackList<IRecorderCallBack> rc = new RemoteCallbackList<IRecorderCallBack>();


    private NotificationManager notificationManager;
    private Notification.Builder notification;
    private NotificationConfig notificationConfig;
    private PendingIntent contentIntent;


    private static final Object lock = new Object();
    private Messenger recorderActivityCallback;

    public static int getDB() {
        int db = 0;// 分贝
        int ratio = 0;
        try {
            //ratio = mRecorder.getMaxAmplitude();
            ratio = recorderGetMaxAmplitude();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ratio > 1) {
            db = ratio;
        }
        return db;
    }

    private static int recorderGetMaxAmplitude() {
        synchronized (lock) {
            if (mRecorder != null) {
                return mRecorder.getMaxAmplitude();
            }
            return 0;
        }
    }

    private boolean showNotification = false;


    private Runnable alertStorage = new Runnable() {
        @Override
        public void run() {
            LeTopSlideToastHelper.getToastHelper(getApplicationContext(), LeTopSlideToastHelper.LENGTH_SHORT,
                    getResources().getString(R.string.storage_full), null,
                    null, null,
                    null).show();
        }
    };

    //widget refresh method
    private void newObjectForWidget() {
        RecordTool.e(LOG_TAG, "newObjectForWidget");
        mTimeCount = 0;
        mAppWidgetManager = AppWidgetManager.getInstance(RecorderService.this);
        mComponentName = new ComponentName(this, RecorderAppWidget.class);
        mRemoteViews = new RemoteViews(Constants.PACKAGE_NAME, R.layout.recorder_app_widget);
        pm = (PowerManager) RecorderService.this.getSystemService(Context.POWER_SERVICE);
    }

    private void timerStart() {
        RecordTool.e(LOG_TAG, "timerStart");
        newObjectForWidget();
        mTimer = new Timer();
        mTimeCountTimerTask = new TimeCountTimerTask();
        mTimer.schedule(mTimeCountTimerTask, 20, 20);
    }

    private void timerStop() {
        RecordTool.e(LOG_TAG, "timerStop" + "mTimer:" + mTimer + " mTimeCountTimerTask:" + mTimeCountTimerTask);
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimeCountTimerTask != null) {
            mTimeCountTimerTask.cancel();
            mTimer = null;
        }
        mTimeCount = 0;
    }

    @Override
    public void onCreate() {
        RecordTool.e(LOG_TAG, "onCreate");
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
        Intent intent = new Intent(this, RecorderActivity.class);
        contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        String title = getResources().getString(R.string.app_name);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notification = new Notification.Builder(this);
        notificationConfig = new NotificationConfig(title, R.drawable.ic_rec_status_44, R.drawable.recording_notification_bar);
        updateNotificationFinish();
    }

    public static boolean isRecording() {
        return mRecorder != null;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return iRecorder;
    }

    protected void onHandleIntent(Intent intent) {
        RecordTool.e(LOG_TAG, "onHandleIntent");
        if (intent == null) {
            RecordTool.e("IntentService", "onHandleIntent");
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(ACTION_NAME)) {
            int action = bundle.getInt(ACTION_NAME);
            if (action == ACTION_PAUSE_RECORDING) {
                pauseRecording();
            } else if (action == ACTION_START_RECORDING) {
                startRecording();
                RecordTool.e(LOG_TAG, "onStartCommand startRecording");
            } else if (action == ACTION_STOP_RECORDING) {
                stopRecording();
            } else if (action == ACTION_DELE_RECORDING) {
                deleRecording();
            } else if (action == ACTION_SAVE_RECORDING) {
                recorderActivityCallback = intent.getParcelableExtra("messenger");
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
        mServiceHandler.sendMessage(msg);
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
            RecordTool.e(LOG_TAG, "int startRecorder");
            if (RecordApp.getInstance().getmState() != MediaRecorderState.IDLE_STATE) {
                stopRecording();
                saveRecording(RecordApp.getInstance().getRecordName());
                RecordTool.hideNotificationWhenBack(getApplicationContext());
            }
            isRemoteRecord = true;
            RecorderService.recordName = recordName;
            return startRecording();
        }

        @Override
        public void register(IRecorderCallBack callback) {
            rc.register(callback);
        }


        @Override
        public void unregister(IRecorderCallBack callback) {
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
        AudioManager am = (AudioManager) RecordApp.getInstance().getSystemService(Context.AUDIO_SERVICE);
        RecordTool.e(LOG_TAG, "parameter：" + am.getParameters("Recorder"));
        int result = am.requestAudioFocus(null, AudioManager.STREAM_DTMF, AudioManager.AUDIOFOCUS_LOSS);

    }

    @Override
    public int startRecording() {

        RecordTool.e(LOG_TAG, "int startRecording");

        if (whichContext instanceof SoundRecorder) {
            audioQulityPram = SettingTool.getAudioQulity(whichContext);
        } else {
            audioQulityPram = SettingTool.getAudioQulity(this);
        }
        if (!RecordTool.isMounted()) {
            RecordTool.e(this.getClass().getName(), "sdcard is unmounted");

            return Constants.SDCARD_UNMOUNT;
        }
        boolean isFull = mRemainingTimeCalculator.storageFull();
        if (isFull) {
            if (!storageFullCallBack()) {
                mHandler.postDelayed(alertStorage, 500);
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
            if (mSegments == 1 && TextUtils.isEmpty(recordName)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(mFileFormat);
                Date date = new Date(System.currentTimeMillis());
                rName = dateFormat.format(date);
                recordName = rName;
                RecordApp.getInstance().setRecordName(rName);
                RecordApp.getInstance().clearFlag();
            } else {
                rName = recordName;
            }
        }
        rName = rName + "_" + mSegments + Constants.RECORD_FORMAT[audioQulityPram.OutputFormat / 3];

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

        if (mTmpFiles.size() <= 0) {
            return Constants.CREATE_FILE_FAIL;
        }

        recorderInit(file);

        //mRecorder = new MediaRecorder();
        ////mRecorder.reset();
        //mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //mRecorder.setOutputFormat(audioQulityPram.OutputFormat);
        //mRecorder.setOutputFile(file.getAbsolutePath());
        //mRecorder.setAudioSamplingRate(audioQulityPram.SampleRate);
        //mRecorder.setAudioEncodingBitRate(audioQulityPram.EncodeBitrate);
        //mRecorder.setAudioChannels(audioQulityPram.AudioChannel);
        //mRecorder.setAudioEncoder(audioQulityPram.EncodeType);
        mRemainingTimeCalculator.reset();

        RecordTool.e(LOG_TAG, audioQulityPram.AudioChannel + "-" + audioQulityPram.EncodeBitrate + "-" + audioQulityPram.EncodeType + "-" + audioQulityPram.OutputFormat + "-" + audioQulityPram.SampleRate);
        mRecorder.setOnErrorListener(new OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                //mRecorder.reset();
                timerStop();
                //mRecorder.release();
                //mRecorder=null;
                recorderReleaseAndSetNull();
                mRecorderState = MediaRecorderState.IDLE_STATE;
                sendStateBroadcast();
                Log.e("eee", "what---" + what + "---extra" + extra);
                LeTopSlideToastHelper.getToastHelper(getApplicationContext(), LeTopSlideToastHelper.LENGTH_SHORT,
                        getResources().getString(R.string.record_error), null,
                        null, null,
                        null).show();
            }
        });
        try {
            stopAudioPlayback();
            RecordTool.e("startRecording", "startRecording:prepare {");
            RecordTool.e("startRecording", "startRecording:prepare }");
            //mRecorder.prepare();
            //mRecorder.start();
            recorderPrepareAndStart();
            RecordTool.e("startRecording", "startRecording:start");
            recordStartTime = System.currentTimeMillis();
            RecordTool.e(LOG_TAG, "start:" + recordStartTime);
            timerStart();
            sendStateBroadcast();
            AudioManagerUtil.initPrePlayingAudioFocus(afChangeListener);

            AudioManager am = (AudioManager) RecordApp.getInstance().getSystemService(Context.AUDIO_SERVICE);
            RecordTool.e(LOG_TAG, "start:" + am.getProperty("Recorder"));
            RecordTool.e(LOG_TAG, "start:" + am.getParameters("Recorder"));
        } catch (Exception startExe) {
            startExe.printStackTrace();
            LeTopSlideToastHelper.getToastHelper(getApplicationContext(), LeTopSlideToastHelper.LENGTH_SHORT,
                    getResources().getString(R.string.record_exception), null,
                    null, null,
                    null).show();
            recorderReleaseAndSetNull();
            //mRecorder.release();
            //mRecorder = null;
            timerStop();
            mRecorderState = MediaRecorderState.IDLE_STATE;
            sendStateBroadcast();
        }
        return Constants.START_RECORD_SUCCESS;
    }

    private void recorderPrepareAndStart() throws IOException {
        synchronized (lock) {
            mRecorder.prepare();
            mRecorder.start();
        }
    }

    private void recorderInit(File file) {
        synchronized (lock) {
            mRecorder = new MediaRecorder();
            //mRecorder.reset();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(audioQulityPram.OutputFormat);
            mRecorder.setOutputFile(file.getAbsolutePath());
            mRecorder.setAudioSamplingRate(audioQulityPram.SampleRate);
            mRecorder.setAudioEncodingBitRate(audioQulityPram.EncodeBitrate);
            mRecorder.setAudioChannels(audioQulityPram.AudioChannel);
            mRecorder.setAudioEncoder(audioQulityPram.EncodeType);
        }
    }

    private void recorderReleaseAndSetNull() {
        synchronized (lock) {
            //mRecorder.reset();
            //mRecorder.release();
            //mRecorder = null;
            if (mRecorder == null)
                return;
            try {
                mRecorder.stop();
            } catch (RuntimeException exception) {
                exception.printStackTrace();
            }
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }


    @Override
    public boolean pauseRecording() {
        RecordTool.loge(LOG_TAG, "pauseRecording");
        mRecorderState = MediaRecorderState.PAUSED;
        recordedDuring += System.currentTimeMillis() - recordStartTime;
        timerStop();
        //if (mRecorder != null) {
        //    mRecorder.stop();
        //    mRecorder.release();
        //    mRecorder = null;
        //}
        recorderStopAndRelease();
        sendStateBroadcast();
//        if(!isRemoteRecord){
//            RecordTool.showNotificationWhenBack(getApplicationContext());
//        }
//        AudioManagerUtil.destroyAudioFocus(null);
        return true;
    }

    @Override
    public boolean stopRecording() {
        RecordTool.loge(LOG_TAG, "stopRecording {");
        if (MediaRecorderState.RECORDING == mRecorderState) {
            recordRealDuring = recordedDuring + System.currentTimeMillis() - recordStartTime;
        }
        timerStop();
        mRecorderState = MediaRecorderState.IDLE_STATE;
        //if (mRecorder != null) {
        //    mRecorder.stop();
        //    mRecorder.release();
        //    mRecorder = null;
        //}
        recorderStopAndRelease();
        sendStateBroadcast();
        showNotification = false;
        sendAlertBroadcast();
        RecordTool.e(LOG_TAG, "stopRecording }");
        AudioManagerUtil.destroyAudioFocus(afChangeListener);
        return true;
    }

    private void recorderStopAndRelease() {
        synchronized (lock) {
            //if (mRecorder != null) {
            //    mRecorder.stop();
            //    mRecorder.release();
            //    mRecorder = null;
            //}

            if (mRecorder == null)
                return;
            try {
                mRecorder.stop();
            } catch (RuntimeException exception) {
                exception.printStackTrace();
            }
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;

        }
    }

    @Override
    public boolean deleRecording() {
        RecordTool.loge(LOG_TAG, "deleRecording {");
        if (mTmpFiles == null || mTmpFiles.size() <= 0) {
            return false;
        }

        for (File tmp : mTmpFiles) {
            tmp.delete();
        }
        mTmpFiles.clear();
        clearRecorderData();
        sendStateBroadcast();
        RecordTool.e(LOG_TAG, "deleRecording }");
        return true;
    }


    @Override
    public boolean saveRecording(String recordName) {
        //metadata byte
        int offset = 6;
        if (FeatureUtil.hasSceneSetting(this) && audioQulityPram.OutputFormat == 4) {
            offset = 9;
        }

        RecordTool.e(LOG_TAG, "saveRecording");
        if (mTmpFiles == null || mTmpFiles.size() <= 0) {
            return false;
        }
//        resumeRecorderData();
        String tempPath = null;
        if (TextUtils.isEmpty(recordName)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(mFileFormat);
            Date date = new Date(System.currentTimeMillis());
            tempPath = dateFormat.format(date) + Constants.RECORD_FORMAT[audioQulityPram.OutputFormat / 3];
        } else {
            tempPath = recordName;
        }

        File finalFile = getFile(isRemoteRecord ? Constants.CALL_RECORD_PATH : Constants.RECORD_PATH, tempPath);
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
                        fileOutputStream.write(tmpBytes, offset, length - offset);
                    }
                }
                fileOutputStream.flush();
                fis.close();
                if (i == mTmpFiles.size() - 1) {
                    RecordEntry entry = new RecordEntry();
                    entry.setFilePath(finalFile.getAbsolutePath());
                    String fileName = finalFile.getName();
                    if (!TextUtils.isEmpty(fileName)) {
                        if (fileName.endsWith(Constants.RECORD_FORMAT[audioQulityPram.OutputFormat / 3])) {
                            entry.setRecordName(fileName.replace(Constants.RECORD_FORMAT[audioQulityPram.OutputFormat / 3], ""));
                        }
                    } else {
                        entry.setRecordName(finalFile.getName());
                    }
                    entry.setRecordTime(finalFile.lastModified());
                    entry.setRecordDuring(RecordDb.getFileDuring(finalFile));
                    RecordTool.e(LOG_TAG, "during" + RecordDb.getFileDuring(finalFile));
                    entry.setCall(isRemoteRecord);
                    entry.setFlags(RecordApp.getInstance().getFlags());
                    RecordDb recordDb = RecordDb.getInstance(getApplicationContext());
                    recordDb.insert(entry);
                    RecordDb.destroyInstance();
                    FileSyncContentProvider.scanFile(this, finalFile);
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

        if (recorderActivityCallback != null) {
            Message m = Message.obtain();
            m.what = SoundRecorder.RECORDER_FINISH;
            //m.obj = this;
            try {
                recorderActivityCallback.send(m);
            } catch (RemoteException e) {
                Log.e("Messenger", "Error passing service object back to activity.");
            }
        }

        return true;
    }

    private void sendStateBroadcast() {
        RecordTool.e(LOG_TAG, "sendStateBroadcast:state:" + RecordApp.getInstance().getmState());
        Intent intent = new Intent(RECORDER_SERVICE_BROADCAST_NAME);
        intent.putExtra(RecordTool.RECORDER_SERVICE_STATE, MediaRecorderState.getStateString(mRecorderState));
        intent.putExtra(RecordTool.RECORDER_CALL_RECORDING, isRemoteRecord);
        sendBroadcast(intent);
        if (MediaRecorderState.RECORDING == mRecorderState ||
                MediaRecorderState.PAUSED == mRecorderState) {
            if (SettingTool.isShowScreenWidget(this) && !LockScreen.isShowing(this)) {
                RecordTool.e(LOG_TAG + "sendStateBroadcast", "state:" + MediaRecorderState.getStateString(mRecorderState) + ":showLock");
                LockScreen.showLockScreenWidght(this);
            }
        } else {
            LockScreen.hideLockScreenWidget(this);
        }

        RecordTool.showNotificationWhenBack(this, mRecorderState);

        String conStr = "";

        if (mRecorderState == MediaRecorderState.RECORDING) {
            conStr = getResources().getString(R.string.recording);
            updateNotification(conStr);
        } else if (mRecorderState == MediaRecorderState.PAUSED) {
            conStr = getResources().getString(R.string.record_paused);
            updateNotification(conStr);
        } else {
            //conStr=getResources().getString(R.string.ready_record);
            //String ticker = getResources().getString(R.string.saving_record);
            //updateNotification(ticker,conStr);
            //stopForeground(false);
            updateNotificationFinish();
        }

        if (isRemoteRecord) {
            RecordApp.getInstance().setmState(mRecorderState);
        }
        saveRecorderData();

        if (RecorderAppWidget.hasAppWidget(getApplicationContext())) {
            RecordTool.e(LOG_TAG, "hasAppWidget");
            Intent appWidget = new Intent(RecorderAppWidget.ACTION_UPDATE);
            sendBroadcast(appWidget);
        }
    }

    @Override
    public void onDestroy() {
        RecordTool.e(LOG_TAG, "onDestroy");
        LockScreen.hideLockScreenWidget(this);
        timerStop();
        super.onDestroy();

        stopForeground(false);
        mServiceHandler.removeCallbacksAndMessages(null);
        mServiceLooper.quitSafely();
    }

    /**
     * record everything change
     */
    private void saveRecorderData() {
        RecordTool.e(LOG_TAG, "saveRecorderData:" + mRecorderState);
        if (mRecorderState == MediaRecorderState.PAUSED ||
                mRecorderState == MediaRecorderState.STOPPED) {
            recordedDuring = RecordTool.getRecordedTime(getApplicationContext());
            recordedDuring += System.currentTimeMillis() - recordStartTime;
            recordStartTime = 0;
        }

        RecordTool.saveRecordState(getApplicationContext(), mRecorderState);
        RecordTool.saveRecordedTime(getApplicationContext(), recordedDuring);
        RecordTool.saveStartRecordTime(getApplicationContext(), recordStartTime);
        RecordTool.saveRecordName(getApplicationContext(), RecordApp.getInstance().getRecordName());
        RecordTool.saveRecordType(getApplicationContext(), isRemoteRecord);
    }

    /**
     * resume data if service crash
     */
    private void resumeRecorderData() {
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
    private void clearRecorderData() {
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

        if (isRemoteRecord) {
            int index = 1;
            String tempName = fileName;
            while (true) {

                File temp = new File(dir, tempName + Constants.RECORD_FORMAT[audioQulityPram.OutputFormat / 3]);

                if (temp.exists()) {
                    String countStr = getResources().getString(R.string.call_record_filename_xliff);
                    countStr = String.format(countStr, index++);
                    tempName = fileName + countStr;

                } else {
                    fileName = tempName;
                    break;
                }
            }
        }

        fileName = fileName + Constants.RECORD_FORMAT[audioQulityPram.OutputFormat / 3];


        File dirFile = new File(dir);
        RecordTool.e("file dir", dir + "      " + fileName);
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
        int i = rc.beginBroadcast();
        boolean isCallback = false;
        while (i > 0) {
            i--;
            try {
                rc.getBroadcastItem(i).storageFull();
                isCallback = true;
            } catch (RemoteException e) {
                e.printStackTrace();
                isCallback = false;
            }
        }
        rc.finishBroadcast();

        return isCallback;
    }

    public static void startRecording(Context context) {
        RecordTool.e(LOG_TAG, "static:startRecording");
        Intent intent = new Intent(context, RecorderService.class);
        whichContext = context;
        intent.putExtra(ACTION_NAME, ACTION_START_RECORDING);
        context.startService(intent);
        isRemoteRecord = false;
    }

    public static void stopRecording(Context context) {
        RecordTool.e(LOG_TAG, "static:stopRecording");
        Intent intent = new Intent(context, RecorderService.class);
        intent.putExtra(ACTION_NAME, ACTION_STOP_RECORDING);
        context.startService(intent);
        isRemoteRecord = false;
    }

    public static void pauseRecoring(Context context) {
        RecordTool.e(LOG_TAG, "static:pauseRecoring");
        Intent target = new Intent(context, RecorderService.class);
        target.putExtra(ACTION_NAME, RecorderService.ACTION_PAUSE_RECORDING);
        context.startService(target);
        isRemoteRecord = false;
    }

    public static void saveRecording(Context context, String recordName) {
        RecordTool.e(LOG_TAG, "static:saveRecording");
        Intent target = new Intent(context, RecorderService.class);
        target.putExtra(ACTION_NAME, RecorderService.ACTION_SAVE_RECORDING);
        context.startService(target);
        RecorderService.recordName = recordName;
        isRemoteRecord = false;
    }

    public static void saveRecording(Context context, String recordName, Handler recorderHandler) {
        RecordTool.e(LOG_TAG, "static:saveRecording");
        Intent target = new Intent(context, RecorderService.class);
        target.putExtra(ACTION_NAME, RecorderService.ACTION_SAVE_RECORDING);
        target.putExtra("messenger", new Messenger(recorderHandler));
        context.startService(target);
        RecorderService.recordName = recordName;
        isRemoteRecord = false;
    }


    public static void deleteRecording(Context context) {
        RecordTool.e(LOG_TAG, "static:deleteRecording");
        Intent target = new Intent(context, RecorderService.class);
        target.putExtra(ACTION_NAME, RecorderService.ACTION_DELE_RECORDING);
        context.startService(target);
        isRemoteRecord = false;
    }

    private boolean checkPermission() {
        AppOpsManager appOps = (AppOpsManager) ActivityThread.currentApplication().
                getSystemService(Context.APP_OPS_SERVICE);
        int callingUid = Binder.getCallingUid();
        String callingPackage = ActivityThread.currentPackageName();
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
            onHandleIntent((Intent) msg.obj);
        }
    }

    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (!RecordTool.canClick(1000)) {
                return;
            }
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {//-2
                if (RecordApp.getInstance().getmState() == MediaRecorderState.RECORDING) {
                    RecorderService.pauseRecoring(getApplicationContext());
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {//1
                if (RecordApp.getInstance().getmState() == MediaRecorderState.STOPPED && RecordApp.getInstance().getmState() == MediaRecorderState.PAUSED) {
                    RecorderService.startRecording(getApplicationContext());
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {//-1
                if (RecordApp.getInstance().getmState() == MediaRecorderState.RECORDING) {
                    RecorderService.pauseRecoring(getApplicationContext());
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {//-3

            }
        }

    };

    class TimeCountTimerTask extends TimerTask {
        @Override
        public void run() {
            RecordTool.e(LOG_TAG, "TimerTask" + this.hashCode() + "run:" + mTimeCount);
            long temp_currentTimeMills = System.currentTimeMillis();
            recordRealDuring = recordedDuring + (temp_currentTimeMills - recordStartTime);
            if (recordRealDuring >= MAX_TIME_LENGTH && !showNotification) {
                showNotification = true;
                sendAlertBroadcast();
            }
            boolean isFull = mRemainingTimeCalculator.storageFull();
            if (isFull) {
                stopRecording();
                saveRecording(RecordApp.getInstance().getRecordName());
                if (!storageFullCallBack()) {
                    mHandler.postDelayed(alertStorage, 500);
                }
                return;
            }
            mTimeCount++;

            //最好作如下判读if(后台录音&&到达刷新时间&&当前widget是锁屏还是桌面小部件)
            if (0 == mTimeCount % 50 && pm.isInteractive() && mAppWidgetProvider.hasInstances(RecorderService.this)) {
                RecordTool.e("TimeCountTimerTask", "TimerTask" + this.hashCode() + "widgetupdate" + mTimeCount);
                if (null == mRemoteViews) {
                    mRemoteViews = new RemoteViews(Constants.PACKAGE_NAME, R.layout.recorder_app_widget);
                }
                if (null == mAppWidgetManager) {
                    mAppWidgetManager = AppWidgetManager.getInstance(RecorderService.this);
                }
                if (null == mComponentName) {
                    mComponentName = new ComponentName(RecorderService.this, RecorderAppWidget.class);
                }
                mRemoteViews.setTextViewText(R.id.remote_record_time_during, RecordTool.recordTimeFormat(RecordTool.getRecordTime(RecorderService.this)));
                mAppWidgetManager.updateAppWidget(mComponentName, mRemoteViews);
            }
        }

        @Override
        public boolean cancel() {
            RecordTool.e(LOG_TAG, "cancel");
            return super.cancel();
        }
    }

    private void createNotification() {
        updateNotification(null);
    }

    private void updateNotification(String ticker) {
        updateNotification(ticker, ticker);
    }

    private void updateNotification(String ticker, String contentText) {
        notification.setContentTitle(notificationConfig.contentTitle)
                .setSmallIcon(notificationConfig.smallIconRes)
                .setContentIntent(contentIntent)
                .setNotificationIcon(notificationConfig.largeIconRes)
                        //.setLargeIcon(BitmapFactory.decodeResource(getResources(), notificationConfig.largeIconRes))
                .setContentText(contentText)
                .setTicker(ticker);
        startForeground(Constants.NOTIFICATION_BACK_ID, notification.build());
    }

    private void updateNotificationFinish() {
        stopForeground(false);
        notificationManager.cancel(Constants.NOTIFICATION_BACK_ID);
    }

}
