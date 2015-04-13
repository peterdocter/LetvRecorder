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
import com.letv.android.recorder.tool.feature.FeatureUtil;

/**
 * Created by snile on 14/11/4.
 */
public class RecorderSetting extends PreferenceActivity{


    private ActionBar setActionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(FeatureUtil.hasSceneSetting(this)){
            addPreferencesFromResource(R.xml.recorder_setting_qcom);
        }else{
            addPreferencesFromResource(R.xml.recorder_setting_mtk);
        }
        setTitle(R.string.app_name);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        setActionBar=getActionBar();
        getMenuInflater().inflate(R.menu.menu_record_sys_set,menu);
        setActionBar.setBottomLineDrawable(getResources().getDrawable(R.color.actionBarActionColor));
        setActionBar.setBottomLineHight(1);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.app_detail:
                Intent in = new Intent().setAction("android.settings.APPLICATION_DETAILS_SETTINGS")
                        .setData(Uri.fromParts("package", "com.letv.android.recorder", null))
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivity(in);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
