package com.letv.android.recorder.widget;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import com.letv.android.recorder.R;
import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.RecordEntry;
import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.tool.RecordTool;

import java.io.File;


public class EditRecordNameDialog {

    private AlertDialog mEditDialog;
    AlertDialog.Builder builder;
    private EditText mEditText;
    private boolean isEdit = false;
    private RecordEntry mEntry;

    public EditRecordNameDialog(final Context context){

        mEditText = new EditText(context);

        builder = new AlertDialog.Builder(context);

        builder.setTitle(R.string.edit_recorder_name)
                .setView(mEditText);
        builder.setPositiveButtonPattern(AlertDialog.ButtonPattern.EXPECTATION)// 期望，蓝色
                .setNeutralButtonPattern(AlertDialog.ButtonPattern.NORMAL);//　警告，红色
        mEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});

    }


    public RecordEntry getEntry() {
        return mEntry;
    }

    public void setEntry(RecordEntry mEntry) {
        this.mEntry = mEntry;
    }

    public void setPositiveButton(Dialog.OnClickListener listener){
        builder.setPositiveButton(R.string.save,listener);
    }

    public void setNegativeButton(Dialog.OnClickListener listener){
        builder.setNeutralButton(R.string.cancel,listener);
    }

    public String getText(){
        return mEditText.getText().toString();
    }





    public void show(RecordEntry mEntry,boolean isEdit){
        this.mEntry = mEntry;
        this.isEdit = isEdit;

        mEditDialog = builder.create();
        Window window = mEditDialog.getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        if(isEdit){
            mEditText.setText(mEntry.getRecordName());
        }else{
            mEditText.setText(RecordApp.getInstance().getRecordName());
        }
        mEditText.setSelection(mEditText.getText().length());
        if(mEditDialog!=null) {
            mEditDialog.show();
        }
    }
}