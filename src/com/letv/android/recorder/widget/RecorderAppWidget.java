package com.letv.android.recorder.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.letv.android.recorder.Constants;
import com.letv.android.recorder.R;
import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.service.Recorder;
import com.letv.android.recorder.service.RecorderService;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.leui.widget.*;
import com.letv.leui.widget.ScreenRecordingView;

/**
 * Created by snile on 9/19/14.
 */
public class RecorderAppWidget extends AppWidgetProvider{

    public String LOG_CAT = RecorderAppWidget.class.getSimpleName()+"widget";

    public static final String ACTION_FLAG="com.letv.android.recorder.AppWidget.ACTION_FLAG";
    public static final String ACTION_PAUSE="com.letv.android.recorder.AppWidget.ACTION_PAUSE";
    public static final String ACTION_START="com.letv.android.recorder.AppWidget.ACTION_START";
    public static final String ACTION_DONE="com.letv.android.recorder.AppWidget.ACTION_DONE";
    public static final String ACTION_UPDATE="com.letv.android.recorder.AppWidget.ACTION_UPDATE";

    public static String APP_WIDGET_ENABLED="app_widget_enabled";
    public int[] appWidgetIds;
    AppWidgetManager appWidgetManager;

    public RecorderAppWidget() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();

//        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
//        String stateStr =  sp.getString(RecordTool.getRecordState(context), Recorder.MediaRecorderState.getStateString(Recorder.MediaRecorderState.IDLE_STATE));
//        Recorder.MediaRecorderState mState = Recorder.MediaRecorderState.getState(stateStr);
        if (!RecordTool.isServiceRunning(context)) {
            RecorderService.stopRecording(context);
            return;
        }
        Recorder.MediaRecorderState mState = RecordTool.getRecordState(context);
        RecordTool.e(LOG_CAT, "onReceive: mState:"+mState+"  action"+action);
        if(action.equals(ACTION_DONE)){
            RecorderService.stopRecording(context);
            RecorderService.saveRecording(context, RecordApp.getInstance().getRecordName());
        }else if(action.equals(ACTION_FLAG)){
            if(mState == Recorder.MediaRecorderState.RECORDING) {
                RecordApp.getInstance().addFlag(RecorderService.recordRealDuring);
            }
        }else if(action.equals(ACTION_PAUSE)){
            if(!RecordTool.canClick(1500)){
                return;
            }
            RecorderService.pauseRecoring(context);
        }else if(action.equals(ACTION_START)){
            Recorder.getInstance();
            RecordTool.isRecordInBack=true;
            if(RecordApp.getInstance().getmState()== Recorder.MediaRecorderState.PAUSED||
                    RecordApp.getInstance().getmState()== Recorder.MediaRecorderState.RECORDING){
                RecordTool.showNotificationWhenBack(context, RecordApp.getInstance().getmState());
//            RecordTool.showNotificationLedWhenBack(this);
            }
            //Recorder.getInstance().checkRecorderState();
            RecordApp.isFromWidget=true;
            RecorderService.startRecording(context);
        }else if(action.equals(ACTION_UPDATE)){
            updateUI(context,mState,intent);
        }

    }

    private static RemoteViews remoteViews;
    static int  i=0;

    private void updateUI(Context context,Recorder.MediaRecorderState mState,Intent intent){
        long preTime = System.currentTimeMillis();
        i++;
        if(remoteViews==null) {
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.recorder_app_widget);
        }
        RecordTool.e(LOG_CAT,"updateUI state :"+Recorder.MediaRecorderState.getStateString(mState));
        updateRemoteViews(context, remoteViews,mState,intent);
        AppWidgetManager appwidget_manager = AppWidgetManager.getInstance(context);
        ComponentName component_name = new ComponentName(context, RecorderAppWidget.class);
        appwidget_manager.updateAppWidget(component_name, remoteViews);
        long postTime = System.currentTimeMillis();
        RecordTool.e(LOG_CAT+"->updateUI time "+i,(postTime-preTime)/1000f+"");
    }
    public void updateUI(Context context){
        long preTime = System.currentTimeMillis();
        i++;
        if(remoteViews==null) {
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.recorder_app_widget);
        }

        updateRemoteViews(context, remoteViews,RecordTool.getRecordState(context),null);
        AppWidgetManager appwidget_manager = AppWidgetManager.getInstance(context);
        ComponentName component_name = new ComponentName(context, RecorderAppWidget.class);
        appwidget_manager.updateAppWidget(component_name, remoteViews);
        long postTime = System.currentTimeMillis();
        RecordTool.e(LOG_CAT+"->updateUI time "+i,(postTime-preTime)/1000f+"");
    }

    private void updateRemoteViews(Context context,RemoteViews remoteViews,Recorder.MediaRecorderState mState,
                                   Intent intent){

        RecordTool.e(LOG_CAT,"updateRemoteViews:mState"+mState.toString());

        if(mState== Recorder.MediaRecorderState.IDLE_STATE||mState== Recorder.MediaRecorderState.PAUSED){

            Intent startIntent = new Intent(ACTION_START);
            PendingIntent startPendingIntent = PendingIntent.getBroadcast(context,0,startIntent,0);

            Intent flagIntent = new Intent(ACTION_FLAG);
            PendingIntent flagPendingIntent = PendingIntent.getBroadcast(context,0,flagIntent,0);

            Intent doneIntent = new Intent(ACTION_DONE);
            PendingIntent donePendingIntent = PendingIntent.getBroadcast(context,0,doneIntent,0);

            remoteViews.setOnClickPendingIntent(R.id.remote_record_flag, flagPendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.remote_record_done, donePendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.remote_record_action, startPendingIntent);
            remoteViews.setImageViewResource(R.id.remote_record_action,R.drawable.start_selector);
            remoteViews.setTextViewText(R.id.remote_record_name, RecordTool.getNewRecordName(context));
            remoteViews.setTextViewText(R.id.remote_record_time_during, RecordTool.recordTimeFormat(RecordTool.getRecordTime(context)));
            remoteViews.setTextViewText(R.id.remote_record_state,mState== Recorder.MediaRecorderState.IDLE_STATE?context.getResources().getText(R.string.ready_record):context.getResources().getText(R.string.record_paused));

        }else if(mState == Recorder.MediaRecorderState.RECORDING){

            Intent flagIntent = new Intent(ACTION_FLAG);
            PendingIntent flagPendingIntent = PendingIntent.getBroadcast(context,0,flagIntent,0);

            Intent doneIntent = new Intent(ACTION_DONE);
            PendingIntent donePendingIntent = PendingIntent.getBroadcast(context,0,doneIntent,0);

            Intent pauseIntent = new Intent(ACTION_PAUSE);
            PendingIntent pausePendingIntent = PendingIntent.getBroadcast(context,0,pauseIntent,0);

            remoteViews.setOnClickPendingIntent(R.id.remote_record_action, pausePendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.remote_record_flag, flagPendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.remote_record_done, donePendingIntent);

            remoteViews.setImageViewResource(R.id.remote_record_action, R.drawable.pause_selector);
            remoteViews.setTextViewText(R.id.remote_record_name, RecordTool.getRecordName(context));
            remoteViews.setTextViewText(R.id.remote_record_state,context.getResources().getText(R.string.recording));

        }else if(mState == Recorder.MediaRecorderState.STOPPED){
            remoteViews.setTextViewText(R.id.remote_record_state,context.getResources().getText(R.string.saving_record));
        }
        if(intent!=null&&intent.getExtras()!=null) {
            Bundle extras = intent.getExtras();
            if(extras.containsKey(ScreenRecordingView.RECORD_TIME_KEY)&&
                    extras.containsKey(ScreenRecordingView.RECORD_DB_KEY)) {
                Bundle bundle = new Bundle();
                RecordTool.e(LOG_CAT+"->updateRemoteViews","long:"+extras.getLong(ScreenRecordingView.RECORD_TIME_KEY)+"float:"+extras.getInt(ScreenRecordingView.RECORD_DB_KEY));
                bundle.putLong(ScreenRecordingView.RECORD_TIME_KEY, extras.getLong(ScreenRecordingView.RECORD_TIME_KEY));
                bundle.putFloat(ScreenRecordingView.RECORD_DB_KEY, extras.getInt(ScreenRecordingView.RECORD_DB_KEY));
//                remoteViews.setBundle(R.id.remote_wave, "updateRecordUI", bundle);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        RecordTool.e(LOG_CAT,"onUpdate:State"+RecordApp.getInstance().getmState());
        if(appWidgetIds==null || appWidgetIds.length<=0){
            return;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
//        String stateStr =  sp.getString(Recorder.MediaRecorderState.KEY, Recorder.MediaRecorderState.getStateString(Recorder.MediaRecorderState.IDLE_STATE));

        Recorder.MediaRecorderState mState = RecordApp.getInstance().getmState();

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.recorder_app_widget);

        updateRemoteViews(context,remoteViews,mState,null);

        appWidgetManager.updateAppWidget(appWidgetIds,remoteViews);

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(APP_WIDGET_ENABLED,true).commit();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(APP_WIDGET_ENABLED,false).commit();
    }

    public static boolean hasAppWidget(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(APP_WIDGET_ENABLED,false);
    }


    private static RecorderAppWidget sInstance;
    public static synchronized RecorderAppWidget getInstance() {
        if (sInstance == null) {
            sInstance = new RecorderAppWidget();
        }
        return sInstance;
    }
    public boolean hasInstances(Context context) {
        appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this.getClass()));
        RecordTool.e(LOG_CAT,"Recorderhaswidget:"+appWidgetIds.length);
        return (appWidgetIds.length > 0);
    }
}
