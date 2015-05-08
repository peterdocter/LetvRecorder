package com.letv.android.recorder.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaMetadataRetriever;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.letv.android.recorder.Constants;
import com.letv.android.recorder.RecordEntry;
import com.letv.android.recorder.tool.RecordTool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecordDb extends SQLiteOpenHelper {

	private static String DB_NAME = "record.db";

	public static String RECORD_TABLE = "record";

	public static String _ID = "_id";
	public static String RECORD_NAME = "recordName";
	public static String RECORD_PATH = "recordPath";
	public static String RECORD_TIME = "recordTime";
	public static String RECORD_DURING = "recordDuring";
	public static String RECORD_IS_CALL = "recordIsCall";
    public static String RECORD_FLAGS = "recordFlags";

	private static RecordDb mInstance;

	public RecordDb(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	public RecordDb(Context context) {
		this(context, DB_NAME, null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table " + RECORD_TABLE + "(" + _ID + " integer  primary key autoincrement," + RECORD_PATH + " text," + RECORD_NAME + " text," + RECORD_TIME + " integer," + RECORD_DURING
				+ " integer," +RECORD_FLAGS+" text,"+ RECORD_IS_CALL + " integer default 0" + ")";
		db.execSQL(sql);
	}

	public synchronized void insert(RecordEntry entry) {
		if (entry != null) {

			Cursor c = getWritableDatabase().query(RECORD_TABLE, null, RECORD_PATH + "=?" ,new String[]{entry.getFilePath()}, null, null, null);
			ContentValues values = new ContentValues();
			values.put(RECORD_PATH, entry.getFilePath());
			values.put(RECORD_DURING, entry.getRecordDuring());
			values.put(RECORD_NAME, entry.getRecordName());
			values.put(RECORD_TIME, entry.getRecordTime());
			values.put(RECORD_IS_CALL, entry.isCall() ? 1 : 0);
            values.put(RECORD_FLAGS,RecordTool.convertArrayFlagsToString(entry.getFlags()));

			if (c == null || c.getCount() <= 0) {
				long row = getWritableDatabase().insert(RECORD_TABLE, null, values);
			} else {
				long row = getReadableDatabase().update(RECORD_TABLE, values, RECORD_PATH + "=?" ,new String[]{entry.getFilePath()});
			}
			
			if(c!=null){
				c.close();
			}
			
		}
	}

	public synchronized void delete(String recordPath) {
		if (!TextUtils.isEmpty(recordPath)) {
			getWritableDatabase().delete(RECORD_TABLE, RECORD_PATH + "=?", new String[] { recordPath });
		}
	}
	
	public synchronized RecordEntry query(String recordName){
		if(!TextUtils.isEmpty(recordName)){
			Cursor cursor = getReadableDatabase().query(RECORD_TABLE, null, RECORD_NAME +"='"+recordName+"'", null, null, null, null);
			if(cursor!=null&&cursor.moveToFirst()){
				RecordEntry entry = new RecordEntry();
				entry.setCall(1 == cursor.getInt(cursor.getColumnIndex(RECORD_IS_CALL)));
				entry.setFilePath(cursor.getString(cursor.getColumnIndex(RECORD_PATH)));
				entry.setRecordDuring(cursor.getInt(cursor.getColumnIndex(RECORD_DURING)));
				entry.setRecordName(cursor.getString(cursor.getColumnIndex(RECORD_NAME)));
				entry.setRecordTime(cursor.getLong(cursor.getColumnIndex(RECORD_TIME)));
				entry.set_id(cursor.getInt(cursor.getColumnIndex(_ID)));
                entry.setFlags(RecordTool.convertFlagsStringToArrayList(cursor.getString(cursor.getColumnIndex(RECORD_FLAGS))));
				cursor.close();
				return entry;
			}
		}
		
		return null;
	}

	public synchronized void update(String oldpath, String path) {
        RecordTool.e(this.getClass().getSimpleName(),oldpath+"-----------"+path);
		if (!TextUtils.isEmpty(path)) {
			ContentValues values = new ContentValues();
			values.put(RECORD_PATH, path);
			values.put(RECORD_NAME, RecordTool.getRecordName(path));
			getWritableDatabase().update(RECORD_TABLE, values, RECORD_PATH + "=?", new String[] { oldpath });
		}
	}

	public synchronized List<RecordEntry> getNormalRecords() {
		return getRecords(false);
	}

    public synchronized List<RecordEntry> getCallRecords() {
        return getRecords(true);
    }


    private synchronized List<RecordEntry> getRecords(boolean isCall){
        List<RecordEntry> entries = new ArrayList<RecordEntry>();
        Cursor cursor = getReadableDatabase().query(RECORD_TABLE, null, RECORD_IS_CALL+"="+(isCall?"1":"0"), null, null, null, RECORD_TIME + " DESC");
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                RecordEntry entry = new RecordEntry();
                entry.setCall(1 == cursor.getInt(cursor.getColumnIndex(RECORD_IS_CALL)));
                entry.setFilePath(cursor.getString(cursor.getColumnIndex(RECORD_PATH)));
                entry.setRecordDuring(cursor.getInt(cursor.getColumnIndex(RECORD_DURING)));
                entry.setRecordName(cursor.getString(cursor.getColumnIndex(RECORD_NAME)));
                entry.setRecordTime(cursor.getLong(cursor.getColumnIndex(RECORD_TIME)));
                entry.set_id(cursor.getInt(cursor.getColumnIndex(_ID)));
                entry.setFlags(RecordTool.convertFlagsStringToArrayList(cursor.getString(cursor.getColumnIndex(RECORD_FLAGS))));
                entries.add(entry);
            }
        }
        if (cursor != null) {
            cursor.close();
        }

        return entries;
    }


    public int getNormalRecordCounts(){
        return getCount(false);
    }

    public int getCallRecordCounts(){
        return getCount(true);
    }

    private synchronized int getCount(boolean isCall){
        Cursor cursor = getReadableDatabase().query(RECORD_TABLE, null, RECORD_IS_CALL+"="+(isCall?"1":"0"), null, null, null, RECORD_TIME + " DESC");
        int count = cursor==null?0:cursor.getCount();
        if(cursor!=null){
            cursor.close();
        }
        return count;
    }


	/**
	 * sync db ,user may be delete the record file by hand
	 */
	public synchronized void syncDBFromSdCard(Context context) {
		Cursor cursor = getReadableDatabase().query(RECORD_TABLE, null, null, null, null, null, null);
        ArrayList<String> savedPath = new ArrayList<String>();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				String path = cursor.getString(cursor.getColumnIndex(RECORD_PATH));
				if (!TextUtils.isEmpty(path)) {
					File file = new File(path);
					if (!file.exists()) {
						getWritableDatabase().delete(RECORD_TABLE, RECORD_PATH + "=?", new String[] { path });
					}else{
                        savedPath.add(path);
                    }
				}
			}
			cursor.close();
		}

        File recordDir = new File(Constants.RECORD_PATH);
        File callDir = new File(Constants.CALL_RECORD_PATH);

        syncFileToDb(context,recordDir,false,savedPath);
        syncFileToDb(context,callDir,true,savedPath);

	}


    private void syncFileToDb(Context context,File recordDir,boolean isCall,ArrayList<String> savedPath){
        if(recordDir!=null) {
            File[] files = recordDir.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    File temp = files[i];
                    if (temp.isFile() && temp.length() > 6 && (temp.getName().endsWith(Constants.RECORD_FORMAT[0])||temp.getName().endsWith(Constants.RECORD_FORMAT[1]))
                            && !savedPath.contains(temp.getAbsolutePath())) {
                        FileInputStream fis = null;
                        try {
                            byte[] header = new byte[11];
                            fis = new FileInputStream(temp);
                            fis.read(header);

                            if (    (header[2] == 0x41 && header[3] == 0x4D && header[4] == 0x52)  ||
                                    (header[8] == 0x33 && header[9] == 0x67 && header[10] == 0x70)) {//AMR  3GP

                                String recordName = temp.getName();
                                recordName = recordName.replace(Constants.RECORD_FORMAT[0], "");
                                recordName = recordName.replace(Constants.RECORD_FORMAT[1], "");
//                                long during = (temp.length() - 6) / 32 * 20;
                                long during =getFileDuring(temp);
                                RecordEntry mEntry = new RecordEntry();
//                                mEntry.setRecordName(recordName);
                                mEntry.setRecordName(getNameByPhoneNum(context, recordName));
                                mEntry.setFilePath(temp.getAbsolutePath());
                                mEntry.setCall(isCall);
                                mEntry.setRecordTime(temp.lastModified());
                                mEntry.setRecordDuring(during);

                                insert(mEntry);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (fis != null) {
                                try {
                                    fis.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static long getFileDuring(File file){
        int audioDuration;

        FileInputStream fis = null;
        String strDuration=null;

         MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(file.getPath());
        strDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

        try {
            audioDuration=Integer.parseInt(strDuration);
            return audioDuration;
        }catch (NumberFormatException num){
            num.printStackTrace();
        }finally {
            retriever.release();
        }
        return  0;
    }


	public synchronized static RecordDb getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new RecordDb(context);
		}
        mInstance.syncDBFromSdCard(context);
        return mInstance;
	}

	public synchronized static void destroyInstance() {
		if (mInstance != null) {
			mInstance.close();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

    private String  getNameByPhoneNum(Context context,String fileName){
        Cursor tCuror =null;
        try{
            String phoneNum = fileName.split("_")[6].replace(" ","");
            String[]  projection=new String[]{	ContactsContract.PhoneLookup.DISPLAY_NAME};
            String    selection =new String(ContactsContract.CommonDataKinds.Phone.NUMBER+" =  ?");
            String[]  selectionArgs=new String[]{phoneNum};
            String name = null;
            tCuror= context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null);

            for( int i = 0; i < tCuror.getCount(); i++ )
            {
                tCuror.moveToPosition(i);
                // 取得联系人名字
                int nameFieldColumnIndex = tCuror.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                name = tCuror.getString(nameFieldColumnIndex);
                RecordTool.e("Contacts", "" + name + " .... " + nameFieldColumnIndex); // 这里提示 force close
            }
            return null!=name?name:phoneNum;
        }catch(ArrayIndexOutOfBoundsException e){
            return fileName;
        }finally {
            if(null!=tCuror){
                tCuror.close();
            }
        }


    }
}
