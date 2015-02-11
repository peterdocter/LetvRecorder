package com.letv.android.recorder.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import com.letv.android.recorder.R;

/**
 * Created by snile on 14/11/4.
 */
public class RecorderSetting extends PreferenceActivity{


    private ActionBar setActionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.recorder_setting);
        setTitle(R.string.app_name);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_record_sys_set,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.app_detail){
            Intent in = new Intent().setAction("android.settings.APPLICATION_DETAILS_SETTINGS")
                    .setData(Uri.fromParts("package", "com.letv.android.recorder", null))
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            startActivity(in);
        }

        return super.onOptionsItemSelected(item);
    }

}
