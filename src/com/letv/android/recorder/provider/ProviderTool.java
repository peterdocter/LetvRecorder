package com.letv.android.recorder.provider;

import java.io.File;
import java.util.ArrayList;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * 存放跟数据库有关的常量
 * @author jacp
 *
 */
public class ProviderTool {
	
	// 这个是每个Provider的标识，在Manifest中使用
	public static final String AUTHORITY = "com.letv.provider.record";
	
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.letv.record";

    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.letv.record";

    public static ArrayList<Uri> getShareUris(String[] filePath){
    	ArrayList<Uri> uris = new ArrayList<Uri>();
    	for(int i=0;i<filePath.length;i++){
    		uris.add(Uri.fromFile(new File(filePath[i])));
    	}
    	return uris;
    }
    
    public static Uri getShareUri(String filePath){
    	return Uri.fromFile(new File(filePath));
    }
    
    public static Uri getContentUri(int record_id){
    	
    	return Uri.parse("content://"+ AUTHORITY +"/records"+"/"+record_id);
    }
    
    /**
     * 跟record表相关的常量
     *
     */
	public static final class RecordColumns implements BaseColumns {
		// CONTENT_URI跟数据库的表关联，最后根据CONTENT_URI来查询对应的表
		public static final Uri CONTENT_URI = Uri.parse("content://"+ AUTHORITY +"/records");
		public static final String TABLE_NAME = RecordDb.RECORD_TABLE;
		public static final String DEFAULT_SORT_ORDER = RecordDb.RECORD_TIME+" desc";
		
		public static final String RECORD_ID=RecordDb._ID;
		public static final String RECORD_NAME = RecordDb.RECORD_NAME;
		public static final String RECORD_DURING=RecordDb.RECORD_DURING;
		public static final String RECORD_PATH = RecordDb.RECORD_PATH;
		public static final String RECORD_TIME = RecordDb.RECORD_TIME;
		public static final String RECORD_IS_CALL = RecordDb.RECORD_IS_CALL;
	}
	
}
