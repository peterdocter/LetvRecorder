package com.letv.android.recorder.tool;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by snile on 14/12/11.
 */
public class LockScreen {

    public static final String IS_SHOWING="is_showing_lock_screen_widget";

    public static final String ACTION_KEYGUARD_INSTALL_WIDGET = "com.leui.keyguard.action.INSTALL_WIDGET";
    public static final String ACTION_KEYGUARD_UNINSTALL_WIDGET = "com.leui.keyguard.action.UNINSTALL_WIDGET";
    public static final String EXTRA_KEYGUARD_APPWIDGET_COMPONENT = "com.leui.keyguard.extra.widget.COMPONENT";

    public static void showLockScreenWidght(Context mContext){
        String pkg = "com.letv.android.recorder";
        String clss = "com.letv.android.recorder.widget.RecorderAppWidget";
        ComponentName mRecordWidget = new ComponentName(pkg, clss);  //ComponentName还可以通过其它方法new出来，这个无所谓。
	Intent intent = new Intent(ACTION_KEYGUARD_INSTALL_WIDGET);
        intent.putExtra(EXTRA_KEYGUARD_APPWIDGET_COMPONENT, mRecordWidget);
        mContext.sendBroadcast(intent);
        saveShowScreenWidgetInfo(mContext,true);
    }

    public static void hideLockScreenWidget(Context mContext){
        String pkg = "com.letv.android.recorder";
        String clss = "com.letv.android.recorder.widget.RecorderAppWidget";
        ComponentName mRecordWidget = new ComponentName(pkg, clss);  //ComponentName还可以通过其它方法new出来，这个无所谓。
	Intent intent = new Intent(ACTION_KEYGUARD_UNINSTALL_WIDGET);
        intent.putExtra(EXTRA_KEYGUARD_APPWIDGET_COMPONENT, mRecordWidget);
        mContext.sendBroadcast(intent);
        saveShowScreenWidgetInfo(mContext,false);
    }

    private static void saveShowScreenWidgetInfo(Context context,boolean show){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(IS_SHOWING,show).commit();
    }

    public static boolean isShowing(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(IS_SHOWING,false);
    }
}
