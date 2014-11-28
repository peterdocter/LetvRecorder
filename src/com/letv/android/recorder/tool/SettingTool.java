package com.letv.android.recorder.tool;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by snile on 14/11/27.
 */
public class SettingTool {

    public static final String PLAY_MODE="play_mode";
    public static final String SCREEN_WIDGET="screen_widget";
    public static final String RECORD_SILENCE="record_silence";
    public static final String SCENE_MODE="scene_mode";

    public static final String SCENE_VOICE_MODE="1";
    public static final String SCENE_MUSIC_MODE="2";

    public enum PlayMode{
        SPEAKER,RECEIVER
    }

    public enum SceneMode{
        VOICES,MUSIC
    }

    public static PlayMode getPlayMode(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean receiver = sp.getBoolean(PLAY_MODE,false);

        return !receiver?PlayMode.SPEAKER:PlayMode.RECEIVER;
    }

    public static boolean isShowScreenWidget(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isShow = sp.getBoolean(SCREEN_WIDGET,false);
        return  isShow;
    }

    public static boolean isSilence(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean silence = sp.getBoolean(RECORD_SILENCE,false);
        return silence;
    }

    public static SceneMode getSceneMode(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String sceneValue = sp.getString(SCENE_MODE, String.valueOf(1));
        if(sceneValue.equals(SCENE_VOICE_MODE)){
            return SceneMode.VOICES;
        }else if(sceneValue.equals(SCENE_MUSIC_MODE)){
            return SceneMode.MUSIC;
        }else{
            return SceneMode.VOICES;
        }
    }
}
