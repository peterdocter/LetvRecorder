package com.letv.android.recorder;


import java.io.Serializable;
import java.util.ArrayList;

public class RecordEntry implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int _id;
	private String filePath;
	private long recordTime;
	private long recordDuring;
	private String recordName;
	private boolean isCall = false;

    private ArrayList<Long> flags;

    public ArrayList<Long> getFlags() {
        return flags;
    }

    public void setFlags(ArrayList<Long> flags) {
        this.flags = flags;
    }

    public void addFlags(long flag){
        if(flags!=null){
            flags.add(flag);
        }
    }

    public int get_id() {
		return _id;
	}

	public void set_id(int _id) {
		this._id = _id;
	}

	public boolean isCall() {
		return isCall;
	}

	public void setCall(boolean isCall) {
		this.isCall = isCall;
	}

	public String getRecordName() {
		return recordName;
	}

	public void setRecordName(String recordName) {
		this.recordName = recordName;
	}

	public long getRecordTime() {
		return recordTime;
	}

	public void setRecordTime(long recordTime) {
		this.recordTime = recordTime;
	}

	public long getRecordDuring() {
		return recordDuring;
	}

	public void setRecordDuring(long recordDuring) {
		this.recordDuring = recordDuring;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	@Override
	public String toString() {
		return "RecordEntry [_id=" + _id + ", filePath=" + filePath + ", recordTime=" + recordTime + ", recordDuring=" + recordDuring + ", recordName=" + recordName + ", isCall=" + isCall + "]";
	}
	
	

}
