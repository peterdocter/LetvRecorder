package com.letv.android.recorder.tool;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.letv.android.recorder.Constants;
import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.RecordEntry;
import com.letv.android.recorder.RecorderActivity;
import com.letv.android.recorder.R;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.leui.util.LeDateTimeUtils;
import com.letv.android.recorder.AbsRecorderActivity;

public class RecordTool {

    public static final boolean DEBUG = false;

    public static void logi(String tag,String log){
        if(DEBUG)
            Log.i(tag,log);
    }

    public static void loge(String tag,String log){
        if(DEBUG)
            Log.e(tag, log);
    }
    public static void e(String tag,String log){
        if(DEBUG)
            Log.e(tag,log);
    }
    /* start save record data*/

    public final static String RECORDER_SERVICE_STATE = "recorder_state";

    public final static String RECORDER_CALL_RECORDING="is_call_recording";

    public final static String RECORDER_NAME="recorder_name";

    public final static String RECORDER_START_TIME="recorder_start_time";

    public final static String RECORDER_RECORDED_TIME="recorder_recorded_time";

    public final static String RECORDER_IS_FIRST_LAUNCH="is_first_launch";

    public static final String RECORD_SILENCE_RingerMode="record_silence_mode";

    public static boolean isRecordInBack=false;



    public static SharedPreferences getSharedPreferences(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
    public static int getRingMode(Context context){
        return getSharedPreferences(context)
                .getInt(RECORD_SILENCE_RingerMode,2);
    }

    public static void saveRingMode(Context context,int ringMode){
        getSharedPreferences(context).edit()
                .putInt(RECORD_SILENCE_RingerMode, ringMode)
                .commit();
    }
    public static boolean isFirstLaunch(Context context){
        boolean isFirst = getSharedPreferences(context).getBoolean(RECORDER_IS_FIRST_LAUNCH,true);
        getSharedPreferences(context).edit().putBoolean(RECORDER_IS_FIRST_LAUNCH,false).commit();
        return isFirst;
    }

    public static void saveRecordState(Context context ,MediaRecorderState mediaRecorderState){
        getSharedPreferences(context).edit()
                .putString(RECORDER_SERVICE_STATE,MediaRecorderState.getStateString(mediaRecorderState))
                .commit();
    }

    public static MediaRecorderState getRecordState(Context context){
        String stateStr = getSharedPreferences(context).getString(RECORDER_SERVICE_STATE,"");
        return MediaRecorderState.getState(stateStr);
    }

    public static void saveRecordedTime(Context context,long recordedTime){
        getSharedPreferences(context).edit()
                .putLong(RECORDER_RECORDED_TIME,recordedTime)
                .commit();
    }

    public static long getRecordedTime(Context context){
        return getSharedPreferences(context)
                .getLong(RECORDER_RECORDED_TIME,0);
    }

    public static void saveStartRecordTime(Context context,long startTime){
        getSharedPreferences(context).edit()
                .putLong(RECORDER_START_TIME,startTime)
                .commit();
    }

    public static long getStartRecordTime(Context context){
        return getSharedPreferences(context)
                .getLong(RECORDER_START_TIME,0);
    }

    public static long getRecordTime(Context context){
        SharedPreferences sp = getSharedPreferences(context);
        long recordedTime = sp.getLong(RECORDER_RECORDED_TIME,0);
        long startTime = sp.getLong(RECORDER_START_TIME,0);
        RecordTool.e("RecordTool--time",""+startTime);
        if(startTime!=0) {
            SharedPreferences.Editor editor= sp.edit();
            editor.putLong(RECORDER_START_TIME,startTime);
            editor.commit();
            return recordedTime + System.currentTimeMillis() - startTime;
        }else{
            return recordedTime;
        }
    }

    public static void saveRecordName(Context context,String recordName){
        getSharedPreferences(context).edit()
                .putString(RECORDER_NAME,recordName)
                .commit();
    }

    public static String getRecordName(Context context){
        return getSharedPreferences(context).getString(RECORDER_NAME,"");
    }

    public static void saveRecordType(Context context,boolean isCall){
        getSharedPreferences(context).edit()
                .putBoolean(RECORDER_CALL_RECORDING,isCall)
                .commit();
    }

    public static boolean getRecordType(Context context){
        return getSharedPreferences(context).getBoolean(RECORDER_CALL_RECORDING,false);
    }

    /* end save record data*/
	
	public static long preClickMillis=0;
	
	public static boolean canClick(int timeInterval){
		long curClickMillis=System.currentTimeMillis();

        timeInterval = Math.max(timeInterval,500);

		boolean canClick = curClickMillis - preClickMillis>timeInterval;
		if(canClick)
			preClickMillis = curClickMillis;
		return canClick;
		
	}
	
	public static void hideNotificationWhenBack(Context context){
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(Constants.NOTIFICATION_BACK_ID);
	}


    public static String getNewRecordName(Context context){


//        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

//        int newIndex = sp.getInt(Constants.NEXT_RECORD_INDEX,1);
//
//        sp.edit().putInt(Constants.NEXT_RECORD_INDEX,newIndex+1).commit();

        int newIndex=1;

        File recordDir = new File(Constants.RECORD_PATH);


        File [] listFile = recordDir.listFiles();
        if(listFile==null||listFile.length==0){
            newIndex = 1;
        }else{
            for(File tmp:listFile){
                String fileName = tmp.getName();
                if(!TextUtils.isEmpty(fileName)&&(fileName.endsWith(Constants.RECORD_FORMAT[0])||fileName.endsWith(Constants.RECORD_FORMAT[1]))&&
                        fileName.length()>(4+3)){
                    String indexStr = fileName.substring(3,fileName.length()-4);
                    try {
                        int index = Integer.parseInt(indexStr);

                        if(index>=newIndex){
                            newIndex = index+1;
                        }

                    }catch (NumberFormatException e){
                        continue;
                    }
                }

            }
        }

        String recordName = context.getResources().getString(R.string.new_recorder_xliff);
        recordName = String.format(recordName,newIndex);
        return recordName;
    }

	
	public static void showNotificationWhenBack(Context context,MediaRecorderState mRecorderState){
        RecordTool.e("showNotificationWhenBack","showNotificationWhenBack 1 :"+isRecordInBack);
        RecordTool.e("showNotificationWhenBack","showNotificationWhenBack 2 :"+isRecordInBack);
        if(isRecordInBack){
            Intent intent = new Intent(context, RecorderActivity.class);

            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

            String title = context.getResources().getString(R.string.app_name);
            String conStr;

            RecordTool.e("showNotificationWhenBack","showNotificationWhenBack 2 :mRecorderState"+mRecorderState+"RecordApp:"+RecordApp.getInstance().getmState());
            if(mRecorderState==MediaRecorderState.RECORDING){
                conStr = context.getResources().getString(R.string.recording);
            }else if(mRecorderState==MediaRecorderState.PAUSED){
                conStr = context.getResources().getString(R.string.record_pase);
            }else{
                hideNotificationWhenBack(context);
                return;
            }

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification(R.drawable.ic_rec_status, conStr, System.currentTimeMillis());
            notification.setLatestEventInfo(context, title, conStr, contentIntent);
            notification.flags = Notification.FLAG_ONGOING_EVENT;
            manager.notify(Constants.NOTIFICATION_BACK_ID, notification);
        }

	}
    private static boolean isLedOn=false;
    private static NotificationManager notifiManager;
    private static Notification ledNotifi;
    private static Handler ledHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };
    private static Runnable runableLed = new Runnable() {

        public void run() {
            if (isLedOn) {
                notifiManager.cancel(Constants.NOTIFICATION_BACK_LED_ID);
                isLedOn=false;
                ledHandler.postDelayed(this, 1000);
            }else{
                notifiManager.notify(Constants.NOTIFICATION_BACK_LED_ID, ledNotifi);
                isLedOn=true;
                ledHandler.postDelayed(this, 1000);
            }
        }
    };
    public static void showNotificationLedWhenBack(Context context){
        notifiManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        ledNotifi = new Notification();
        ledNotifi.defaults = Notification.DEFAULT_ALL;
        ledNotifi.defaults &= ~Notification.DEFAULT_LIGHTS;
        ledNotifi.defaults &= ~Notification.DEFAULT_SOUND;
        ledNotifi.defaults &= ~Notification.DEFAULT_VIBRATE;
        ledNotifi.ledARGB = Color.GREEN;
        ledNotifi.ledOffMS = 0;
        ledNotifi.ledOnMS = 1000;
        ledNotifi.flags |= Notification.FLAG_SHOW_LIGHTS;
        ledNotifi.priority = Notification.PRIORITY_MAX;

        ledHandler.post(runableLed);
    }
    public static void hintNotificationLedWhenBack(Context context){
        if(notifiManager!=null){
            notifiManager.cancel(Constants.NOTIFICATION_BACK_LED_ID);
            ledHandler.removeCallbacks(runableLed);
        }
    }

	public static void showNotificationAlert(Context context, int id){
		Intent intent = new Intent(context, RecorderActivity.class);
		
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);

		String title = context.getResources().getString(R.string.app_name);
		String conStr = context.getResources().getString(R.string.record_over_max_time);
		
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.ic_rec_status, title, System.currentTimeMillis());
		notification.setLatestEventInfo(context, title, conStr, contentIntent);
		manager.notify(id, notification);
		
	}
	
	
	public static void hideNotificationAlert(Context context, int id){
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(id);
	}
	

	public static String getRecordName(String recordPath){
		if(TextUtils.isEmpty(recordPath)){
			throw new IllegalArgumentException("record path is null");
		}else{
			int index = recordPath.lastIndexOf(File.separator);
			String name = recordPath.substring(index+1);
			int formatIndex=name.lastIndexOf(".");
			if(formatIndex!=0){
				name = name.substring(0, formatIndex);
			}
			return name;
		}
	}
	
	public static boolean canSave(Context context, String recordNewName) {
		if (TextUtils.isEmpty(recordNewName)) {
			Toast.makeText(context, R.string.record_name_cannot_null, Toast.LENGTH_LONG).show();
			return false;
		}

		File recordDir = new File(Constants.RECORD_PATH);
		File[] fileList = recordDir.listFiles();
		if (fileList != null && fileList.length > 0) {
			for (File tmp : fileList) {
				if (tmp.isFile()) {
					if (tmp.getName().equalsIgnoreCase(recordNewName+Constants.RECORD_FORMAT[0])||tmp.getName().equalsIgnoreCase(recordNewName+Constants.RECORD_FORMAT[1])) {
						Toast.makeText(context, R.string.record_existed, Toast.LENGTH_LONG).show();
						return false;
					}
				}
			}
		}

		return true;
	}

	public static boolean haveRecord() {
		boolean haveRecord = false;

		if (isMounted()) {
			File recordDir = new File(Constants.RECORD_PATH);
			File[] fileList = recordDir.listFiles();
			if (fileList != null && fileList.length > 1) {
				haveRecord = true;
			}
		}

		return haveRecord;

	}

	public static boolean isMounted() {
		return Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED);
	}

	public static List<RecordEntry> getRecordFileList() {
		List<RecordEntry> fileList = new ArrayList<RecordEntry>();
		if (isMounted()) {
			File recordDir = new File(Constants.RECORD_PATH);
			File[] files = recordDir.listFiles();
			if (files != null && files.length > 1) {
				for (File tmp : files) {
					if (tmp.isDirectory())
						continue;
					RecordEntry entry = new RecordEntry();
					entry.setFilePath(tmp.getAbsolutePath());
					entry.setRecordName(tmp.getName());
					entry.setRecordTime(tmp.lastModified());
					fileList.add(entry);
				}
			}
		}
		return fileList;
	}

	public static String timeFormat(long time, String format) {
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		Date date = new Date(time);
		return dateFormat.format(date);
	}
	
	public static String recordTimeFormat(long duration){
        long sec = (duration / 1000) % 60;
        long min = (duration / 1000 / 60) % 60;
        long hour = (duration / 1000 / 60 / 60);
        StringBuilder sb = new StringBuilder();
        sb.append(hour<10?("0")+hour:hour);
		sb.append(":");
		sb.append(min<10?("0"+min):min);
		sb.append(":");
		sb.append(sec<10?("0"+sec):sec);
        return sb.toString();
	}


	public static String  recordDateFormat(long timestamp){

        return  LeDateTimeUtils.formatTimeStampString(RecordApp.getInstance(),timestamp,LeDateTimeUtils.FORMAT_TYPE_LIST);

//		long curTimestamp = System.currentTimeMillis();
//
//		int deltaDay = (int)((curTimestamp - timestamp)/(1000*60*60*24));
//
//		Date date = new Date(timestamp);
//		Date curDate = new Date(System.currentTimeMillis());
//		SimpleDateFormat dateFormat = null;
//
//		if(deltaDay==0){
//			dateFormat = new SimpleDateFormat("HH:mm");
//			return dateFormat.format(date);
//		}else if(deltaDay<7){
//			String str=null;
//			switch (deltaDay) {
//			case 1:
//				str = RecordApp.getInstance().getResources().getString(R.string.yesterday);
//				break;
//			case 2:
//				str = RecordApp.getInstance().getResources().getString(R.string.before_yesterday);
//				break;
//			case 3:
//			case 4:
//			case 5:
//			case 6:
//				str = deltaDay+RecordApp.getInstance().getResources().getString(R.string.few_days);
//				break;
//			default:
//				break;
//			}
//			return str;
//		}else if(deltaDay<14){
//			return RecordApp.getInstance().getResources().getString(R.string.week_ago);
//		}else if(date.getYear()!=curDate.getYear()){
//			dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//			return dateFormat.format(date);
//		}else{
//			dateFormat = new SimpleDateFormat("MM-dd");
//			return dateFormat.format(date);
//		}
		
	}
	
	public static String ruleTime(long ruleTimemills){
		if(ruleTimemills<0){
			return "";
		}
		long sec = (ruleTimemills / 1000) % 60;
        long min = (ruleTimemills / 1000 / 60) % 60;
        StringBuilder sb = new StringBuilder();
		sb.append(min<10?("0"+min):min);
		sb.append(":");
		sb.append(sec<10?("0"+sec):sec);
        return sb.toString();
	}
	
	public static String getStartTimeStr(){
		Date date = new Date(RecordApp.getInstance().getStartTimeMills());
		SimpleDateFormat format = new SimpleDateFormat(RecordApp.getInstance().getResources().getString(R.string.start_time_format));
		return format.format(date);
	}

    public static String convertArrayFlagsToString(ArrayList<Long> flags){
        if(flags==null||flags.size()==0){
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for(int i=0;i<flags.size();i++){
            sb.append(flags.get(i));
            if(i < flags.size()-1){
                sb.append(",");
            }
        }

        return sb.toString();
    }

    public static ArrayList<Long> convertFlagsStringToArrayList(String flagsString){

        if(TextUtils.isEmpty(flagsString)){
            return null;
        }

        String [] flags = flagsString.split(",");
        ArrayList<Long> flagList = new ArrayList<Long>();
        for(int i = 0;i<flags.length;i++){
            try {
                flagList.add(Long.parseLong(flags[i]));
            }catch (Exception e){
//                e.printStackTrace();
                continue;
            }
        }

        return flagList;

    }

}
