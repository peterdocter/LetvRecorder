package com.letv.android.recorder.fragment;

import android.content.Context;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import com.letv.android.recorder.widget.ActionBarTool;
import com.letv.leui.widget.DividerFilter;
import android.widget.TextView;

import com.letv.android.recorder.R;
import com.letv.android.recorder.RecordApp;
import com.letv.android.recorder.RecordEntry;
import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.leui.widget.LeCheckBox;

import java.util.ArrayList;
import java.util.List;

public class RecorderAdapter extends BaseAdapter implements DividerFilter{

	private Context context;
	private List<RecordEntry> recordList;
	private List<Boolean> recordSelectFlag;
	private boolean actionMode = false;
    private boolean isShowCallRecord = false;
    
    private RecordEntry playRecordEntry;
    
    private ListView mListView;

    private boolean isScroll = false;

    private boolean isHasCall = false;



	public boolean isActionMode() {
		return actionMode;
	}

    public boolean isShowCallRecord() {
        return isShowCallRecord;
    }

    public void setShowCallRecord(boolean isShowCallRecord) {
        this.isShowCallRecord = isShowCallRecord;
    }

    public boolean isHasCallRecord() {
        return isHasCall;
    }

    public void setHasCall(boolean isHasCall) {
        this.isHasCall = isHasCall;
    }
    public boolean isSelected(int position){
    	if(recordSelectFlag==null||recordSelectFlag.size()<=position)
    		return false;
    	
    	return recordSelectFlag.get(position);
    }

    public void setActionMode(boolean actionMode) {
		this.actionMode = actionMode;
        recordSelectFlag = new ArrayList<Boolean>();

        for(int i=0;i<getCount();i++){
            recordSelectFlag.add(new Boolean(Boolean.FALSE));
        }
	}

    public void clearSelectFlag(){
        if(recordSelectFlag!=null) {

            for(int i=0;i<recordSelectFlag.size();i++){
                recordSelectFlag.set(i,false);
            }

            notifyDataSetChanged();
        }

        fragment.changeSelectStatus();
    }


    public void selectAllFlag(){
        if(recordSelectFlag!=null) {
            for(int i=0;i<recordSelectFlag.size();i++){
                recordSelectFlag.set(i,true);
            }

            notifyDataSetChanged();
        }

        fragment.changeSelectStatus();
    }

    public List<RecordEntry> getRecordList() {
        return recordList;
    }

    public void setRecordSelectFlag(List<Boolean> recordSelectFlag) {
        this.recordSelectFlag = recordSelectFlag;
    }

    public List<Boolean> getRecordSelectFlag() {
        return recordSelectFlag;
    }

    public void setRecordList(List<RecordEntry> recordList) {
        TransitionManager.beginDelayedTransition(mListView, ActionBarTool.autoTransition);
        this.recordList = recordList;
        recordSelectFlag = new ArrayList<Boolean>();

        for(int i=0;i<getCount();i++){
            recordSelectFlag.add(new Boolean(Boolean.FALSE));
        }
    }

    public RecordedFragment fragment;

    private static  RecorderAdapter instance;
    
    public static RecorderAdapter getInstance(){
    	return instance;
    }
    
    public RecorderAdapter(Context context, List<RecordEntry> recordList,RecordedFragment fragment) {
		super();
		this.context = context;
		this.recordList = recordList;
        this.fragment = fragment;
        instance = this;
	}


    public void setListView(ListView mListView) {
        this.mListView = mListView;

        if(this.mListView!=null){
            this.mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    isScroll = scrollState != SCROLL_STATE_IDLE;
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                }
            });
        }
    }


    @Override
	public int getCount() {
        int count = recordList ==null?0:recordList.size();

        if(!isShowCallRecord()&&isHasCallRecord()){
            count++;
        }
        return count;
	}

	@Override
	public RecordEntry getItem(int arg0) {

        if(!isShowCallRecord()){
            if(isHasCallRecord()&&arg0==0){
                RecordEntry entry = new RecordEntry();
                entry.setRecordName(
                        context.getResources().getString(R.string.voice_recording));
                return entry;
            }else if(isHasCallRecord()){
                return recordList.get(arg0-1);
            }else{
                return recordList.get(arg0);
            }
        }

        return recordList.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}

	
	public void notifyDataSetChanged(RecordEntry mRecordEntry) {
		playRecordEntry = mRecordEntry;
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position , View arg1, ViewGroup arg2) {
		ViewHolder holder = null;
		if(arg1 ==null){
			holder = new ViewHolder();
			arg1 = LayoutInflater.from(context).inflate(R.layout.record_item, null);
			holder.recordName = (TextView) arg1.findViewById(R.id.record_name);
			holder.recordLength = (TextView) arg1.findViewById(R.id.record_length);
			holder.recordTime = (TextView) arg1.findViewById(R.id.record_time);
			holder.box= (LeCheckBox) arg1.findViewById(R.id.item_select);
            holder.arrow = arg1.findViewById(R.id.arrow);
			arg1.setTag(holder);
		}else{
			holder = (ViewHolder) arg1.getTag();
		}

		RecordEntry entry = getItem(position);

		holder.recordName.setText(entry.getRecordName());
		holder.box.setVisibility(actionMode ? View.VISIBLE : View.GONE);
        if(getItemViewType(position)!=ITEM_TYPE_CALL_SET) {
            holder.recordLength.setText(RecordTool.recordTimeFormat(entry.getRecordDuring()));
            holder.recordTime.setText(RecordTool.recordDateFormat(entry.getRecordTime()));
            holder.recordTime.setVisibility(View.VISIBLE);
            holder.arrow.setVisibility(View.GONE);
        }else{
            holder.recordTime.setVisibility(View.GONE);
            int callCount = RecordDb.getInstance(context).getCallRecordCounts();

            String str = context.getResources().getString(R.string.call_record_count_xliff);


            holder.recordLength.setText(String.format(str,callCount));
            holder.arrow.setVisibility(View.VISIBLE);
        }


		if(actionMode){
			holder.box.setChecked(recordSelectFlag.get(position),!isScroll);
		}else{
            if(holder.box.isChecked()){
                holder.box.setChecked(false,false);
            }
        }
		MediaRecorderState state = RecordApp.getInstance().getmState();
    	if (MediaRecorderState.PLAYING == state || MediaRecorderState.PLAYING_PAUSED == state ||
                MediaRecorderState.PLAY_STOP == state) {
    		if(playRecordEntry!=null){
    			if(TextUtils.equals(getItem(position).getFilePath(), playRecordEntry.getFilePath())){
    				holder.recordName.setTextColor(getColor(R.color.title__color_play));
            		holder.recordTime.setTextColor(getColor(R.color.sumary_color_play));
            		holder.recordLength.setTextColor(getColor(R.color.sumary_color_play));

                    if(MediaRecorderState.PLAYING == state){
                        String playStr = context.getResources().getString(R.string.play_record_playing);
                        holder.recordLength.setText(playStr);
                    }else if(MediaRecorderState.PLAYING_PAUSED == state){
                        String pauseStr = context.getResources().getString(R.string.play_record_pause);
                        holder.recordLength.setText(pauseStr);
                    }

    			}else{
    				holder.recordName.setTextColor(getColor(R.color.title_color_a30));
            		holder.recordTime.setTextColor(getColor(R.color.sumary_color_a30));
            		holder.recordLength.setTextColor(getColor(R.color.sumary_color_a30));
    			}
    		}
    	}else if(actionMode){
    		if(isSelected(position)){
                arg1.setActivated(true);
    			holder.recordName.setTextColor(getColor(R.color.title_color_selected));
        		holder.recordTime.setTextColor(getColor(R.color.summary_color_selected));
        		holder.recordLength.setTextColor(getColor(R.color.summary_color_selected));
    		}else{
                arg1.setActivated(false);
    			holder.recordName.setTextColor(getColor(R.color.title_color_a30));
        		holder.recordTime.setTextColor(getColor(R.color.sumary_color_a30));
        		holder.recordLength.setTextColor(getColor(R.color.sumary_color_a30));
    		}
    	}else{
            arg1.setActivated(false);
    		holder.recordName.setTextColor(getColor(R.color.title_color));
    		holder.recordTime.setTextColor(getColor(R.color.summary_color));
    		holder.recordLength.setTextColor(getColor(R.color.summary_color));
    	}
		
		return arg1;
	}
	
	private int getColor(int res){
		return context.getResources().getColor(res); 
	}
	
    
    public boolean topDividerEnabled() {
        return false;
    }

    
    public boolean bottomDividerEnabled() {
        return false;
    }

    
    public boolean dividerEnabled(int position) {
        return true;
    }

    
    public int leftDividerMargin(int position) {
        return context.getResources().getDimensionPixelOffset(R.dimen.record_item_divider_padding_left);
    }

    
    public int rightDividerMargin(int position) {
        return leftDividerMargin(position);
    }

    static class  ViewHolder{
		TextView recordName,recordLength,recordTime;
		LeCheckBox box;
        View arrow;
	}


    @Override
    public int getViewTypeCount() {
        if(recordList == null){
            return super.getViewTypeCount();
        }

        if(isShowCallRecord)
            return 1;

        boolean hasCall = false;
        boolean hasNormal = false;


        for(int i=0;i<recordList.size();i++){
           if(recordList.get(i).isCall()){
               hasCall = true;
           }else{
               hasNormal = true;
           }
        }

        int count = 0;

        count+=(hasCall?1:0);
        count+=(hasNormal?1:0);
        return count==0?1:count;
    }

    @Override
    public int getItemViewType(int position) {
        if(recordList == null){
            return super.getItemViewType(position);
        }

        if(isShowCallRecord){
            return ITEM_TYPE_CALL_RECORD;
        }else{
            if(position==0&&isHasCallRecord()){
                return ITEM_TYPE_CALL_SET;
            }else{
                return ITEM_TYPE_NORMAL_RECORD;
            }
        }
    }


    public static final int ITEM_TYPE_CALL_RECORD=1001;
    public static final int ITEM_TYPE_NORMAL_RECORD=1002;
    public static final int ITEM_TYPE_CALL_SET=1003;


    @Override
    public boolean forceDrawDivider(int position){
        return false;
    }
}
