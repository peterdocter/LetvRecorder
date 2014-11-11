package com.letv.android.recorder.tool;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.FrameLayout;

/**
 * Created by snile on 14-9-1.
 */
public class StatusBarTool {

    public static void updateStausBar(Activity activity){

        Window win = activity.getWindow();
        WindowManager.LayoutParams params = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        params.flags |= bits;
        win.setAttributes(params);

        // Create a view of status bar
        int statusBarHeight = getInternalDimensionSize(activity.getApplicationContext().getResources(), "status_bar_height");

        FrameLayout.LayoutParams params2 = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, statusBarHeight);
        params2.gravity = Gravity.TOP;
        View statusBar = new View(activity);
        statusBar.setLayoutParams(params2);
        ((ViewGroup) win.getDecorView()).addView(statusBar);

    }

    public static int getInternalDimensionSize(Resources res, String key) {
        int result = 0;
        int resourceId = res.getIdentifier(key, "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }


    public static boolean updateStatusBarActionBackgroundAndTopMargin(Activity activity,Drawable statusBarActonBarBackground){

        int contentTopMargin=getActionBarHeight(activity)+getInternalDimensionSize(activity.getResources(),"status_bar_height");


        boolean result = false;
        if(activity instanceof Activity){
            Window win = activity.getWindow();
            View mDecorView = win.getDecorView();
            View actionBarOverlayLayout = mDecorView.findViewById(getInternalId(activity,"action_bar_overlay_layout"));
            ViewGroup content = (ViewGroup) actionBarOverlayLayout.findViewById(getInternalId(activity,"content"));
            ActionBar mActionBar = activity.getActionBar();

            if(actionBarOverlayLayout != null && content != null && mActionBar != null){
                if(content.getChildCount() > 0 && contentTopMargin > 0){
                    View child = content.getChildAt(0);
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                    lp.topMargin = contentTopMargin;
                    result = true;
                } else if (contentTopMargin > 0) {
                    return result;
                }

                actionBarOverlayLayout.setBackground(statusBarActonBarBackground);
                mActionBar.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                result = true;
            }
        }
        return result;
    }


    private static int getInternalId(Context context,String IdName){
        Resources resources = context.getResources();
        int statusBarIdentifier = resources.getIdentifier(IdName,
                "id", "android");
        return statusBarIdentifier;
    }

    /**
     * Retrieve the height of action bar that defined in theme
     * @param context
     * @return
     */
    public static int getActionBarHeight(Context context) {
        TypedArray actionbarSizeTypedArray = context.obtainStyledAttributes(new int[] {android.R.attr.actionBarSize});
        int actionbarHeight = actionbarSizeTypedArray.getDimensionPixelSize(0,0);
        actionbarSizeTypedArray.recycle();
        return actionbarHeight;
    }

}
