package com.letv.android.recorder.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.letv.android.recorder.R;

/**
 * Created by snile on 14/11/4.
 */
public class RecorderSetting extends PreferenceActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.recorder_setting);

    }
}
