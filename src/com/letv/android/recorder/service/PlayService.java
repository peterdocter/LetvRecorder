package com.letv.android.recorder.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

public class PlayService extends Service  {

	public static final String ACTION_PLAY = "play";
	public static final String ACTION_NEXT = "next";
	public static final String ACTION_PREV = "prev";
	public static final String ACTION_STOP = "stop";
	public static final String ACTION_PAUSE = "pause";
	public static final String ACTION_SEEK = "seek";

	public static final String PARAM_PATH = "param_path";


	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			String action = intent.getAction();
			if (action != null) {
				if (action.equals(ACTION_PLAY)) {
					String path = intent.getStringExtra(PARAM_PATH);
					PlayEngineImp.getInstance().play(path);
				} else if (action.equals(ACTION_NEXT)) {
					PlayEngineImp.getInstance().next();
				} else if (action.equals(ACTION_PREV)) {
					PlayEngineImp.getInstance().prev();
				} else if (action.equals(ACTION_STOP)) {
					stopSelfResult(startId);
				} else if (action.equals(ACTION_PAUSE)) {
					PlayEngineImp.getInstance().pause();
				}
			}
		}
		return super.onStartCommand(intent, flags, startId);
	}

    private static String path;

    public static void play(Context context){
        if(!TextUtils.isEmpty(path)){
            startPlay(context,path);
        }
    }
	
	public static void startPlay(Context context,String path){
        PlayService.path = path;
		Intent intent = new Intent(context, PlayService.class);
		intent.setAction(ACTION_PLAY);
		intent.putExtra(PARAM_PATH, path);
		context.startService(intent);
	}
	
	public static void pausePlay(Context context){
		Intent intent = new Intent(context, PlayService.class);
		intent.setAction(ACTION_PAUSE);
		context.startService(intent);
	}
	
	public static void stopPlay(Context context){
		Intent intent = new Intent(context, PlayService.class);
		intent.setAction(ACTION_STOP);
		context.startService(intent);
        PlayService.path = null;
	}
	

}
