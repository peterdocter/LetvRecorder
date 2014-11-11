package com.letv.android.recorder.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

    public String LOG_CAT = RecorderAppWidget.class.getSimpleName();

    public static final String ACTION_FLAG="com.letv.android.recorder.AppWidget.ACTION_FLAG";
    public static final String ACTION_PAUSE="com.letv.android.recorder.AppWidget.ACTION_PAUSE";
    public static final String ACTION_START="com.letv.android.recorder.AppWidget.ACTION_START";
    public static final String ACTION_DONE="com.letv.android.recorder.AppWidget.ACTION_DONE";
    public static final String ACTION_UPDATE="com.letv.android.recorder.AppWidget.ACTION_UPDATE";

    public static String APP_WIDGET_ENABLED="app_widget_enabled";

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
        Recorder.MediaRecorderState mState = RecordTool.getRecordState(context);
        if(action.equals(ACTION_DONE)){
            RecorderService.stopRecording(context);
            RecorderService.saveRecording(context, RecordApp.getInstance().getRecordName());
        }else if(action.equals(ACTION_FLAG)){
            if(mState == Recorder.MediaRecorderState.RECORDING) {
                RecordApp.getInstance().addFlag(RecorderService.recordRealDuring);
            }
        }else if(action.equals(ACTION_PAUSE)){
            RecorderService.pauseRecoring(context);
        }else if(action.equals(ACTION_START)){
            RecorderService.startRecording(context);
        }else if(action.equals(ACTION_UPDATE)){
            updateUI(context,mState,intent);
        }

    }

    private void updateUI(Context context,Recorder.MediaRecorderState mState,Intent intent){
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.recorder_app_widget);

        updateRemoteViews(context,remoteViews,mState,intent);

        AppWidgetManager appwidget_manager = AppWidgetManager.getInstance(context);
        ComponentName component_name = new ComponentName(context, RecorderAppWidget.class);
        appwidget_manager.updateAppWidget(component_name, remoteViews);

    }


    private void updateRemoteViews(Context context,RemoteViews remoteViews,Recorder.MediaRecorderState mState,
                                   Intent intent){




        if(mState== Recorder.MediaRecorderState.IDLE_STATE||mState== Recorder.MediaRecorderState.PAUSED){

            Intent startIntent = new Intent(ACTION_START);
            PendingIntent startPendingIntent = PendingIntent.getBroadcast(context,0,startIntent,0);


            remoteViews.setOnClickPendingIntent(R.id.remote_record_action, startPendingIntent);
            remoteViews.setImageViewResource(R.id.remote_record_action,R.drawable.start_selector);
            remoteViews.setTextViewText(R.id.remote_record_name, RecordTool.getNewRecordName(context));
            remoteViews.setTextViewText(R.id.remote_record_state,"准备就绪");

            remoteViews.setViewVisibility(R.id.remote_record_flag, View.GONE);
            remoteViews.setViewVisibility(R.id.remote_record_done,View.GONE);
            remoteViews.setViewVisibility(R.id.remote_wave,View.GONE);


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
            remoteViews.setTextViewText(R.id.remote_record_state,"正在录音...");

            remoteViews.setViewVisibility(R.id.remote_record_flag, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.remote_record_done,View.VISIBLE);
            remoteViews.setViewVisibility(R.id.remote_wave,View.VISIBLE);
        }else if(mState == Recorder.MediaRecorderState.STOPPED){
            remoteViews.setTextViewText(R.id.remote_record_state,"正在保存...");
        }


        remoteViews.setTextViewText(R.id.remote_record_time_during,
                RecordTool.recordTimeFormat(RecordTool.getRecordTime(context)));

        if(intent!=null&&intent.getExtras()!=null) {

            Bundle extras = intent.getExtras();
            if(extras.containsKey(ScreenRecordingView.RECORD_TIME_KEY)&&
                    extras.containsKey(ScreenRecordingView.RECORD_DB_KEY)) {

                Bundle bundle = new Bundle();
                bundle.putLong(ScreenRecordingView.RECORD_TIME_KEY, extras.getLong(ScreenRecordingView.RECORD_TIME_KEY));
                bundle.putFloat(ScreenRecordingView.RECORD_DB_KEY, extras.getFloat(ScreenRecordingView.RECORD_DB_KEY));
                remoteViews.setBundle(R.id.remote_wave, "updateRecordUI", bundle);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        if(appWidgetIds==null || appWidgetIds.length<=0){
            return;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String stateStr =  sp.getString(Recorder.MediaRecorderState.KEY, Recorder.MediaRecorderState.getStateString(Recorder.MediaRecorderState.IDLE_STATE));

        Recorder.MediaRecorderState mState = Recorder.MediaRecorderState.getState(stateStr);

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
}
