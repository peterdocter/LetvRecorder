package com.letv.android.recorder.aidl;

interface IRecorderCallBack{

    boolean storageFull();

    void updateMaxAmplitude(in long recordedTimeMs,in int maxAmplitude);
}