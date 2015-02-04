package com.letv.android.recorder.settings;

import java.util.HashMap;

import android.content.Context;
/**
 * Created by ci on 15-1-26.
 */

public  class AudioQulityPram {

    public int EncodeType=0;
    public int AudioChannel=0;
    public int EncodeBitrate=0;
    public int SampleRate=0;
    public int OutputFormat=0;
    public AudioQulityPram(int [] arr){
        EncodeType=arr[0];
        AudioChannel=arr[1];
        EncodeBitrate=arr[2];
        SampleRate=arr[3];
        OutputFormat=arr[4];
    }

}