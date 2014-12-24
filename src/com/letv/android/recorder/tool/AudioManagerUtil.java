package com.letv.android.recorder.tool;

import android.content.Context;
import android.media.AudioManager;
import com.letv.android.recorder.RecordApp;

/**
 * Created by snile on 14/12/23.
 */
public class AudioManagerUtil {

    public static int initPrePlayingAudioFocus(AudioManager.OnAudioFocusChangeListener afChangeListener){
        AudioManager am = (AudioManager) RecordApp.getInstance().getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(afChangeListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        return result;
    }

    public static void destroyAudioFocus(AudioManager.OnAudioFocusChangeListener afChangeListener){
        AudioManager am = (AudioManager) RecordApp.getInstance().getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(afChangeListener);
    }

}
