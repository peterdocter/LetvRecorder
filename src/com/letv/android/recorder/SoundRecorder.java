package com.letv.android.recorder;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import com.letv.android.recorder.fragment.RecordedFragment;
import com.letv.android.recorder.provider.ProviderTool;
import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.service.RecorderService;
import com.letv.android.recorder.tool.RecordTool;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SoundRecorder extends AbsRecorderActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        topWidget.setCenterTitle(R.string.record_note);

        recordedFragment = new RecordedFragment();
        recordedFragment.setCallRecordUI(true);
        getFragmentManager().beginTransaction().add(R.id.container, recordedFragment).commit();
        mRecorder.startRecording(this);
	}


	@Override
	public void onClick(View arg0) {

        if(arg0.getId()==R.id.stopBtn){
            saveRecordAndReturn();
            return;
        }

        super.onClick(arg0);

	}

	@Override
	public void onBackPressed() {
		saveRecordAndReturn();
	}

	private void saveRecordAndReturn() {
		if (MediaRecorderState.RECORDING == mRecorderState || MediaRecorderState.PAUSED == mRecorderState) {
            mRecorder.stopRecording(this);
            RecorderService.saveRecording(this, RecordApp.getInstance().getRecordName());
		} else {
			setResult(RESULT_CANCELED);
			finish();
		}
	}

    String recordName;

	@Override
	public void onStateChanged(MediaRecorderState state) {
		mRecorderState = state;
		updateUI();

		if (MediaRecorderState.RECORDING == mRecorderState) {
            recordName = RecordTool.getRecordName(this);
		} else if (MediaRecorderState.IDLE_STATE == mRecorderState) {
			RecorderService.saveRecording(this, recordName);
			Intent data = new Intent();

			RecordDb db = RecordDb.getInstance(this);
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


}
