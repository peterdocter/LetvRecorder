package com.letv.android.recorder;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import com.letv.android.recorder.fragment.RecordedFragment;
import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.service.RecorderService;
import com.letv.android.recorder.tool.RecordTool;

import java.io.File;

public class SoundRecorder extends AbsRecorderActivity {

    private final static String TAG="SoundRecorder";
    public static final int RECORDER_FINISH=2;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//        topWidget.setCenterTitle(R.string.record_note);

        recordedFragment = new RecordedFragment();
        recordedFragment.setShowCallingRecordUI(true);
        getFragmentManager().beginTransaction().add(R.id.container, recordedFragment).commit();
//        mRecorder.startRecording(this);
	}


	@Override
	public void onClick(View arg0) {

        if(arg0.getId()==R.id.stopBtn){
            RecordTool.e(TAG, "onclickStop1");
            saveRecordAndReturn();
            RecordTool.e(TAG, "onclickStop2");
            return;
        }

        super.onClick(arg0);

	}

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        recordedFragment.setShowCallingRecordUI(false);
        super.onDestroy();
    }

    @Override
	public void onBackPressed() {
		saveRecordAndReturn();
	}

	private void saveRecordAndReturn() {
        RecordTool.e(TAG,"saveRecordAndReturn:"+mRecorderState);
		if (MediaRecorderState.RECORDING == mRecorderState || MediaRecorderState.PAUSED == mRecorderState) {
            mRecorder.stopRecording(this);
            recordName=RecordApp.getInstance().getRecordName();
            RecorderService.saveRecording(this, RecordApp.getInstance().getRecordName(),mHandler);
		} else {
			setResult(RESULT_CANCELED);
			finish();
		}
	}

    String recordName;
    boolean hasRecord = false;

	@Override
	public void onStateChanged(MediaRecorderState state) {
		mRecorderState = state;
		updateUI();

		if (MediaRecorderState.RECORDING == mRecorderState) {
            //recordName = RecordApp.getInstance().getRecordName();
            hasRecord = true;
		} else if (MediaRecorderState.IDLE_STATE == mRecorderState && hasRecord) {
			//RecorderService.saveRecording(this, recordName,mHandler);
			//Intent data = new Intent();
            //
			//RecordDb db = RecordDb.getInstance(this);
			//RecordEntry entry = db.query(recordName);
			//RecordDb.destroyInstance();
			//if (entry != null) {
             //   Uri uri = addToMediaDB(entry);
            //
             //   if(uri != null){
             //       data.setData(uri);
             //       setResult(RESULT_OK,data);
             //   }else{
             //       setResult(RESULT_CANCELED);
             //   }
            //
			//}else{
             //   setResult(RESULT_CANCELED);
            //}
            //
			//finish();
		}

	}


    private Uri addToMediaDB(RecordEntry entry) {
        File file = new File(entry.getFilePath());
        Resources res = getResources();
        ContentValues cv = new ContentValues();
        long current = System.currentTimeMillis();
        long modDate = file.lastModified();
//        Date date = new Date(current);
//        SimpleDateFormat formatter = new SimpleDateFormat(
//                res.getString(com.android.soundrecorder.R.string.audio_db_title_format));
//        String title = formatter.format(date);

        // Lets label the recorded audio file as NON-MUSIC so that the file
        // won't be displayed automatically, except for in the playlist.
        cv.put(MediaStore.Audio.Media.IS_MUSIC, "0");

        cv.put(MediaStore.Audio.Media.TITLE, entry.getRecordName());
        cv.put(MediaStore.Audio.Media.DATA, file.getAbsolutePath());
        cv.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
        cv.put(MediaStore.Audio.Media.DATE_MODIFIED, (int) (modDate / 1000));
        cv.put(MediaStore.Audio.Media.DURATION, entry.getRecordDuring());
        cv.put(MediaStore.Audio.Media.ARTIST,
                res.getString(R.string.audio_db_artist_name));
        cv.put(MediaStore.Audio.Media.ALBUM,
                res.getString(R.string.audio_db_album_name));
//        Log.d(TAG, "Inserting audio record: " + cv.toString());
        ContentResolver resolver = getContentResolver();
        Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//        Log.d(TAG, "ContentURI: " + base);
        Uri result = resolver.insert(base, cv);
        if (result == null) {
            return null;
        }
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
        return result;
    }

    Handler mHandler = new Handler(/* default looper */) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECORDER_FINISH:
                    Intent data = new Intent();
                    RecordDb db = RecordDb.getInstance(SoundRecorder.this);
                    RecordEntry entry = db.query(recordName);
                    RecordDb.destroyInstance();
                    if (entry != null) {
                       Uri uri = addToMediaDB(entry);
                       if(uri != null){
                           data.setData(uri);
                           setResult(RESULT_OK,data);
                       }else{
                           setResult(RESULT_CANCELED);
                       }
                    }else{
                       setResult(RESULT_CANCELED);
                    }
                    finish();
                    break;
            }
        }
    };

}
