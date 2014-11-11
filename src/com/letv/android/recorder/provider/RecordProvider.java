package com.letv.android.recorder.provider;

import java.io.File;
import java.util.HashMap;

import com.letv.android.recorder.tool.RecordTool;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class RecordProvider extends ContentProvider {
	private static HashMap<String, String> recordMatchMap;
	private static final UriMatcher sUriMatcher;

	public static final int RECORDS = 1;
	public static final int RECORD_ID = 2;

	private RecordDb recordDb;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(ProviderTool.AUTHORITY, "records", RECORDS);
		sUriMatcher.addURI(ProviderTool.AUTHORITY, "records/#", RECORD_ID);

		recordMatchMap = new HashMap<String, String>();
		recordMatchMap.put(ProviderTool.RecordColumns._ID, ProviderTool.RecordColumns._ID);
		recordMatchMap.put(ProviderTool.RecordColumns.RECORD_NAME, ProviderTool.RecordColumns.RECORD_NAME);
		recordMatchMap.put(ProviderTool.RecordColumns.RECORD_DURING, ProviderTool.RecordColumns.RECORD_DURING);
		recordMatchMap.put(ProviderTool.RecordColumns.RECORD_IS_CALL, ProviderTool.RecordColumns.RECORD_IS_CALL);
		recordMatchMap.put(ProviderTool.RecordColumns.RECORD_PATH, ProviderTool.RecordColumns.RECORD_PATH);		
		recordMatchMap.put(ProviderTool.RecordColumns.RECORD_TIME, ProviderTool.RecordColumns.RECORD_TIME);
	}
	
	@Override
	public boolean onCreate() {
		recordDb = RecordDb.getInstance(getContext());
		return true;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		int count = 0 ;
		switch (sUriMatcher.match(arg0)) {
		case RECORDS:
			count = recordDb.getWritableDatabase().delete(ProviderTool.RecordColumns.TABLE_NAME, arg1, arg2);
			break;
		case RECORD_ID:
			 String noteId = arg0.getPathSegments().get(1);
			   count = recordDb.getWritableDatabase().delete(ProviderTool.RecordColumns.TABLE_NAME, ProviderTool.RecordColumns._ID + "=" + noteId
	                    + (!TextUtils.isEmpty(arg1) ? " AND (" + arg1 + ')' : ""), arg2);
			break;
		default:
			 throw new IllegalArgumentException("Unknown URI " + arg0);
		}
		
		getContext().getContentResolver().notifyChange(arg0, null);
		return count;
	}

	@Override
	public String getType(Uri arg0) {
		
		switch (sUriMatcher.match(arg0)) {
		case RECORDS:
			return ProviderTool.CONTENT_TYPE;
		case RECORD_ID:
			return ProviderTool.CONTENT_ITEM_TYPE;
		default:
			 throw new IllegalArgumentException("Unknown URI " + arg0);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		
		if(sUriMatcher.match(uri)!=RECORDS){
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        
        if (!values.containsKey(ProviderTool.RecordColumns.RECORD_PATH)) {
        	String path = values.getAsString(ProviderTool.RecordColumns.RECORD_PATH);
        	File file = new File(path);
        	if(file.isDirectory()||!file.exists())
        		throw new SQLException("Failed to insert row into " + uri);
        }

        if (!values.containsKey(ProviderTool.RecordColumns.RECORD_NAME)) {
        	String path = values.getAsString(ProviderTool.RecordColumns.RECORD_PATH);
            values.put(ProviderTool.RecordColumns.RECORD_NAME, RecordTool.getRecordName(path));
        }
        
        if(!values.containsKey(ProviderTool.RecordColumns.RECORD_TIME)){
        	String path = values.getAsString(ProviderTool.RecordColumns.RECORD_PATH);
        	File file = new File(path);
        	values.put(ProviderTool.RecordColumns.RECORD_TIME, file.lastModified());
        }

        SQLiteDatabase db = recordDb.getWritableDatabase();
        long rowId = db.insert(ProviderTool.RecordColumns.TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(ProviderTool.RecordColumns.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
		
	}

	

	@Override
	public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(ProviderTool.RecordColumns.TABLE_NAME);

		switch (sUriMatcher.match(arg0)) {
		case RECORDS:
			qb.setProjectionMap(recordMatchMap);
			break;
		case RECORD_ID:
			qb.setProjectionMap(recordMatchMap);
			qb.appendWhere(ProviderTool.RecordColumns._ID + "=" + arg0.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + arg0);
		}
		String orderBy;
		if (TextUtils.isEmpty(arg4)) {
			orderBy = ProviderTool.RecordColumns.DEFAULT_SORT_ORDER;
		} else {
			orderBy = arg4;
		}


		Cursor c = qb.query(recordDb.getReadableDatabase(), arg1, arg2, arg3, null, null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), arg0);
		return c;
	}
	

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		int count=0;
		switch (sUriMatcher.match(arg0)) {
		case RECORDS:
			count=recordDb.getWritableDatabase().update(ProviderTool.RecordColumns.TABLE_NAME, arg1, arg2, arg3);
			break;
		case RECORD_ID:
			count=recordDb.getWritableDatabase().update(ProviderTool.RecordColumns.TABLE_NAME, arg1, ProviderTool.RecordColumns._ID + "=" + arg0.getPathSegments().get(1)
                    + (!TextUtils.isEmpty(arg2) ? " AND (" + arg2 + ')' : ""), arg3);
			break;
		default:
			break;
		}
		
		getContext().getContentResolver().notifyChange(arg0, null);
		return count;
	}
	
}
