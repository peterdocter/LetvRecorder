package com.letv.android.recorder.service;

/**
 * Created by dupengtao on 15-4-16.
 */
public class NotificationConfig {

    public int smallIconRes;
    public int largeIconRes;
    public String contentTitle;
    public String ticker;


    public NotificationConfig(String contentTitle,int smallIconRes, int largeIconRes) {
        this.smallIconRes = smallIconRes;
        this.largeIconRes = largeIconRes;
        this.contentTitle = contentTitle;
    }
}
