package com.letv.android.recorder.settings;

import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import com.letv.android.recorder.R;

/**
 * Created by snile on 14/11/4.
 */
public class RecorderSetting extends PreferenceActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.recorder_setting);
        setTitle(R.string.app_name);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
