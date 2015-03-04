package com.letv.android.recorder;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.letv.leui.widget.LeTopSlideToastHelper;

import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.service.RecorderService;
import com.letv.android.recorder.tool.RecordTool;

public class SaveRecordActivity extends Activity implements OnClickListener {
	
	
	public static final String RECORD_ENTRY = "record_entry";
	private RecordEntry mEntry;
	
	private TextView titleView;
	private EditText recordNameET;
	private Button delBtn,saveBtn;
	private boolean isEdit = false;
	
	
	
	public static final String ACTION_EDIT="edit_mode";
	public static final String ACTION_EDIT_PATH="edit_file_path";
	public static final String DATA_CHANGED="changed";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activty_save);
		
		isEdit = getIntent().getBooleanExtra(ACTION_EDIT, false);
		mEntry = (RecordEntry) getIntent().getSerializableExtra(RECORD_ENTRY);
		
		recordNameET = (EditText) findViewById(R.id.record_et);
		delBtn = (Button) findViewById(R.id.del_btn);
		saveBtn = (Button) findViewById(R.id.save_btn);
		titleView = (TextView) findViewById(R.id.titleView);
		delBtn.setOnClickListener(this);
		saveBtn.setOnClickListener(this);
	}
	
	@Override
	protected void onStart() {
		titleView.setText(isEdit?R.string.edit_recorder_name:R.string.input_new_recorder_name);
		if(isEdit){
			recordNameET.setText(mEntry.getRecordName());
		}else{
			recordNameET.setText(RecordApp.getInstance().getRecordName());
		}
		super.onStart();
	}
	
	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.del_btn:
			if(!isEdit){
				RecorderService.deleteRecording(this);
				finish();
			}else{
				finish();
			}
			break;
		case R.id.save_btn:
			if(!isEdit&&RecordTool.canSave(this, recordNameET.getText().toString())){
				RecorderService.saveRecording(this, recordNameET.getText().toString());
				finish();
			}else if(isEdit){
				File file = new File(mEntry.getFilePath());
				String fileName = RecordTool.getRecordName(mEntry.getFilePath());
				if(fileName.equalsIgnoreCase(recordNameET.getText().toString())){
					LeTopSlideToastHelper.getToastHelper(this, LeTopSlideToastHelper.LENGTH_SHORT,
							getResources().getString(R.string.no_change_recordname),null,
							null,null,
							null).show();
				}else if(RecordTool.canSave(this, recordNameET.getText().toString())){
					String newPath = mEntry.getFilePath().replace(fileName, recordNameET.getText().toString());
					file.renameTo(new File(newPath));
					RecordDb recordDb = RecordDb.getInstance(this);
					recordDb.update(mEntry.getFilePath(), newPath);
					RecordDb.destroyInstance();
					setChangedResult();
					finish();
				}
			}
			break;
		default:
			break;
		}
	}
	
	
	private void setChangedResult(){
		Intent data = new Intent();
		data.putExtra(DATA_CHANGED, true);
		setResult(RESULT_OK, data);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK&&!isEdit){
			return true;
		}
		
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
	}
	

}
