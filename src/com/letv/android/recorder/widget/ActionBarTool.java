package com.letv.android.recorder.widget;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import com.letv.android.recorder.R;

/**
 * Created by snile on 15/1/5.
 */
public class ActionBarTool {

    static public void changeActionBar(Activity activity,boolean showUpAsHome){
        Window win = activity.getWindow();
        View mDecorView = win.getDecorView();
        ViewGroup sceneRoot = (ViewGroup)mDecorView.findViewById(getInternalId(activity,"action_bar"));
        TransitionManager.beginDelayedTransition(sceneRoot,autoTransition);
        int options = activity.getActionBar().getDisplayOptions();
        int optionsMask = ActionBar.DISPLAY_HOME_AS_UP;
//        activity.getActionBar().setDisplayOptions(options ^ optionsMask, optionsMask);
        ActionBar tActionBar=activity.getActionBar();
        tActionBar.setDisplayOptions(options ^ optionsMask, optionsMask);
        if(showUpAsHome){
            tActionBar.setTitle(R.string.voice_recording);
            return;
        }
        tActionBar.setTitle(R.string.record_note);
    }

    public static int getInternalId(Context context,String IdName){
        Resources resources = context.getResources();
        int id = resources.getIdentifier(IdName,
                "id", "android");
        return id;
    }

    public static AutoTransition autoTransition = new AutoTransition(){

        @Override
        public TransitionSet setDuration(long duration) {
            return super.setDuration(200);
        }

        @Override
        public TransitionSet setOrdering(int ordering) {
            return super.setOrdering(ORDERING_TOGETHER);
        }
    };

}
