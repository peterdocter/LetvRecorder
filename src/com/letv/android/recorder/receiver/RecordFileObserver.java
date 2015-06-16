package com.letv.android.recorder.receiver;

import android.content.Context;
import android.os.FileObserver;
import android.os.Handler;
import com.letv.android.recorder.Constants;

import java.io.File;

/**
 * Created by zhangjiahao on 15-6-16.
 */
public class RecordFileObserver extends FileObserver {

    public interface OnRecordFileChangeListener {
        void onDeleted(String path);
    }

    private Context mContext;
    private OnRecordFileChangeListener mOnRecordFileChangeListener;
    private Handler mHandler;

    public RecordFileObserver(Context context, OnRecordFileChangeListener listener) {
        super(Constants.RECORD_PATH, FileObserver.ALL_EVENTS);
        this.mContext = context;
        this.mOnRecordFileChangeListener = listener;
        mHandler = new Handler(mContext.getMainLooper());
    }

    @Override
    public void onEvent(int event, String path) {
        final String relativePath = path;
        final int action = event & FileObserver.ALL_EVENTS;
        switch (action) {
            case FileObserver.ACCESS:
                break;

            case FileObserver.DELETE:
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mOnRecordFileChangeListener.onDeleted(Constants.RECORD_PATH + File.separator + relativePath);
                    }
                });
                break;

            case FileObserver.OPEN:
                break;

            case FileObserver.MODIFY:
                break;
        }
    }
}
