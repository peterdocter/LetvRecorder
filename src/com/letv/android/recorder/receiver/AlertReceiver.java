package com.letv.android.recorder.receiver;

import com.letv.android.recorder.Constants;
import com.letv.android.recorder.tool.RecordTool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlertReceiver extends BroadcastReceiver {

	private int ALERT_NOTIFICATION_ID=66667;
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		if(Constants.ALERT_ACTION.equals(arg1.getAction())){
			
			if(arg1.hasExtra("showAlert")){
				boolean show = arg1.getBooleanExtra("showAlert", false);
				if(show){
					RecordTool.showNotificationAlert(arg0, ALERT_NOTIFICATION_ID);
				}else{
					RecordTool.hideNotificationAlert(arg0, ALERT_NOTIFICATION_ID);
				}
			}
			
		}
	}

}
