package com.letv.android.recorder.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.letv.android.recorder.*;
import com.letv.android.recorder.provider.ProviderTool;
import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.tool.FileSyncContentProvider;
import com.letv.android.recorder.tool.FileTool;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.android.recorder.widget.EditRecordNameDialog;
import com.letv.android.recorder.widget.RecordingView;
import com.letv.leui.widget.LeBottomWidget;
import com.letv.leui.widget.LeCheckBox;

public class RecordedFragment extends Fragment implements OnClickListener {

	private View rootView = null;
	private ViewFlipper recordVF;
	private ListView recordList;
	private TextView recordTime;
	private TextView recordStartTime;
	private TextView recordName;
    private View updateName,recordViewMask;
	private RecordingView recordingView;

	private RecorderAdapter recordedAdapter;


	private List<Boolean> recordSelectFlag;
	
	private View selectView,cancelView,selectAllView,backView;


    private final int PAGE_NO_RECORD=0;
    private final int PAGE_SHOW_RECORD_LIST=1;
    private final int PAGE_SHOW_RECORDING=2;

    protected LeBottomWidget leBottomWidget;

//  other app record launch record UI
    private boolean callRecordUI = false;

    public boolean isCallRecordUI() {
        return callRecordUI;
    }

    public void setCallRecordUI(boolean callRecordUI) {
        this.callRecordUI = callRecordUI;
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        if(!isCallRecordUI()) {
            recordedAdapter = new RecorderAdapter(getActivity(), null,this);
        }
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume() {
        if(!isCallRecordUI()&&!((AbsRecorderActivity)getActivity()).isFistTime()) {
            refreshRecordList();
        }
		super.onResume();
	}

	public void onPrepareOptionsMenu() {

		MediaRecorderState state = RecordApp.getInstance().getmState();
		selectView = getActivity().getActionBar().getCustomView().findViewById(R.id.select_mode);
		cancelView = getActivity().getActionBar().getCustomView().findViewById(R.id.select_cancel);
        selectAllView = getActivity().getActionBar().getCustomView().findViewById(R.id.select_all);
        backView = getActivity().getActionBar().getCustomView().findViewById(R.id.back);

        if(recordedAdapter.isShowCallRecord()){
            backView.setVisibility(View.VISIBLE);
        }else{
            backView.setVisibility(View.GONE);
        }

		if (MediaRecorderState.RECORDING == state || MediaRecorderState.PAUSED == state || MediaRecorderState.STOPPED == state) {
			selectView.setVisibility(View.GONE);
			cancelView.setVisibility(View.GONE);
            selectAllView.setVisibility(View.GONE);
		} else {
			if (recordedAdapter.isActionMode()) {
				selectView.setVisibility(View.GONE);
				cancelView.setVisibility(View.VISIBLE);
                selectAllView.setVisibility(View.VISIBLE);
			} else {
				if (recordedAdapter.getCount()==0||(recordedAdapter.getCount()==1
                    && recordedAdapter.getItemViewType(0)==RecorderAdapter.ITEM_TYPE_CALL_SET)) {
					selectView.setVisibility(View.GONE);
					cancelView.setVisibility(View.GONE);
                    selectAllView.setVisibility(View.GONE);
					getActivity().findViewById(R.id.bottom_widget).setVisibility(View.GONE);
					getActivity().findViewById(R.id.record_control_layout).setVisibility(View.VISIBLE);
				} else {
					selectView.setVisibility(View.VISIBLE);
                    selectAllView.setVisibility(View.GONE);
					cancelView.setVisibility(View.GONE);
				}
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_recorded, container, false);
		recordVF = (ViewFlipper) rootView.findViewById(R.id.recordVF);
		recordList = (ListView) rootView.findViewById(R.id.record_list);
//        recordList.setOverScrollEnabled(false);
		recordTime = (TextView) rootView.findViewById(R.id.record_time);
		recordStartTime = (TextView) rootView.findViewById(R.id.record_start_time);
		recordName = (TextView) rootView.findViewById(R.id.record_title);
		recordingView = (RecordingView) rootView.findViewById(R.id.recording_view);
        updateName = rootView.findViewById(R.id.update_name);
        recordViewMask = rootView.findViewById(R.id.record_pause_mask);
        if(isCallRecordUI()){
            recordVF.setDisplayedChild(PAGE_SHOW_RECORDING);
        }
        if(((AbsRecorderActivity)getActivity()).isFistTime()){
            recordVF.setDisplayedChild(PAGE_SHOW_RECORDING);
            fistTimeRecordTimeUI();
        }
		return rootView;
	}

    public void setFirstTime(boolean firstTime){
        if(firstTime){
            recordVF.setDisplayedChild(PAGE_SHOW_RECORDING);
        }
    }

    public void fistTimeRecordTimeUI(){
        String newStr = RecordTool.recordTimeFormat(0);
        recordTime.setText(newStr);
        recordName.setText(RecordTool.getNewRecordName(getActivity()));
    }

	/**
	 * 
	 * @param recordTimeMillis
	 */
	public void updateRecordTimeUI(long recordTimeMillis, float db) {
		if(!isDetached()){
			if (recordTime != null) {
                String newStr = RecordTool.recordTimeFormat(recordTimeMillis);
                if(!recordTime.getText().toString().equals(newStr))
				    recordTime.setText(newStr);
			}

			if(recordStartTime!=null){
                String newStr = RecordTool.getStartTimeStr();
                if(!recordStartTime.getText().toString().equals(newStr)){
				    recordStartTime.setText(RecordTool.getStartTimeStr());
                }
			}

			if(recordName!=null){
                String newStr = RecordApp.getInstance().getRecordName();
                if(!recordName.getText().toString().equals(newStr)) {
                    recordName.setText(RecordApp.getInstance().getRecordName());
                }
			}


			if (recordingView != null) {
				recordingView.updateRecordUI(recordTimeMillis, db);
			}
		}
		
	}

	public void stopRecording() {
		if (recordingView != null) {
			recordingView.stopRecording();
		}
	}

	public void startRecording() {
		if (recordingView != null) {
			recordingView.startRecording();
		}
	}

    public void resumeRecording(){
        if(recordingView != null){
            recordingView.resumeRecording();
        }
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        if(!isCallRecordUI()) {
            recordList.setAdapter(recordedAdapter);
            recordedAdapter.setListView(recordList);
            if(!((AbsRecorderActivity)getActivity()).isFistTime()){
                refreshRecordList();
            }
            recordList.setOnItemClickListener(getRecordItemClickListener());
            recordList.setOnItemLongClickListener(longClickListener);
            selectView = getActivity().getActionBar().getCustomView().findViewById(R.id.select_mode);
            cancelView = getActivity().getActionBar().getCustomView().findViewById(R.id.select_cancel);
            selectAllView = getActivity().getActionBar().getCustomView().findViewById(R.id.select_all);
            backView = getActivity().getActionBar().getCustomView().findViewById(R.id.back);

            selectView.setOnClickListener(getSelectModeListener());
            cancelView.setOnClickListener(getCancelListener());
            selectAllView.setOnClickListener(getSelectAllListener());
            updateName.setOnClickListener(getUpdateRecordListener());

            recordVF.setInAnimation(getActivity(), android.R.anim.fade_in);
            recordVF.setOutAnimation(getActivity(), android.R.anim.fade_out);

            backView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });

            leBottomWidget = (LeBottomWidget)getActivity().findViewById(R.id.bottom_widget);
            intiBottomWidget();
        }

		super.onActivityCreated(savedInstanceState);
	}



    private void intiBottomWidget() {
        if(leBottomWidget!=null){
            leBottomWidget.setModeAndTabCount(LeBottomWidget.MODE_ICON_TEXT,3);
            leBottomWidget.addTab(0,"share",R.drawable.ic_rec_share,R.drawable.ic_rec_play_share_pressed,
                    getResources().getString(R.string.share));
            leBottomWidget.addTab(1,"delete",R.drawable.ic_rec_delete,R.drawable.ic_rec_delete_disable,
                    getResources().getString(R.string.delete));
            leBottomWidget.addTab(2,"rename",R.drawable.ic_rec_rename,R.drawable.ic_rec_rename_disable,
                    getResources().getString(R.string.rename));
            leBottomWidget.setTitleTextColor(R.color.actionBarBackground);
            leBottomWidget.setOnClickAndLongClickListener(new LeBottomWidget.OnClickAndLongClickListener() {
                @Override
                public void onClick(int pos, String tag) {
                    switch (pos){
                        case 0:
                            ArrayList<Uri> uris = ProviderTool.getShareUris(getSelectedPaths());
                            boolean multiple = uris.size() > 1;
                            Intent share = new Intent(!multiple ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
                            share.setType("audio/*");
                            share.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share));
                            share.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.record_files));

                            if (multiple) {
                                share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                            } else {
                                share.putExtra(Intent.EXTRA_STREAM, uris.get(0));
                            }

                            startActivity(Intent.createChooser(share, getActivity().getTitle()));
                            break;
                        case 1:
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(getResources().getString(R.string.delete_record_dialog_title))
                                    .setPositiveButton(getResources().getString(R.string.delete),new Dialog.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            recordedAdapter.setActionMode(false);
                                            int[] selecteds = getSelectedIndexs();
                                            for (int i = 0; i < selecteds.length; i++) {

                                                int type = recordedAdapter.getItemViewType(selecteds[i]);

                                                if(type == RecorderAdapter.ITEM_TYPE_CALL_SET){
                                                    RecordDb db = RecordDb.getInstance(getActivity());
                                                    List<RecordEntry> callRecords =db.getCallRecords();
                                                    if(callRecords!=null && callRecords.size()>0) {
                                                        for (RecordEntry call : callRecords) {
                                                            String path = call.getFilePath();
                                                            File delFile = new File(path);
                                                            delFile.delete();
                                                            FileSyncContentProvider.removeImageFromLib(getActivity(),
                                                                    path);
                                                        }
                                                    }
                                                }else{
                                                    String path = recordedAdapter.getItem(selecteds[i]).getFilePath();
                                                    File delFile = new File(path);
                                                    delFile.delete();
                                                    FileSyncContentProvider.removeImageFromLib(getActivity(),
                                                            path);
                                                }
                                            }
                                            refreshRecordList();
                                            updateSherlockUI();

                                        }
                                    })
                                    .setNegativeButton(getResources().getString(R.string.cancel), new Dialog.OnClickListener(){
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            recordedAdapter.setActionMode(false);
                                            refreshRecordList();
                                            updateSherlockUI();
                                        }
                                    } )
                                    .create()
                                    .show();

                            break;
                        case 2:
                            final EditRecordNameDialog mDialog = new EditRecordNameDialog(getActivity());
                            mDialog.setPositiveButton(new Dialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    RecordEntry mEntry = mDialog.getEntry();

                                    File file = new File(mEntry.getFilePath());
                                    String fileName = RecordTool.getRecordName(mEntry.getFilePath());
                                    if (fileName.equalsIgnoreCase(mDialog.getText())) {

                                    } else if (RecordTool.canSave(getActivity(), mDialog.getText())) {
                                        RecordDb recordDb = RecordDb.getInstance(getActivity());
                                        String oldPath = mEntry.getFilePath();
                                        String newPath = mEntry.getFilePath().replace(fileName, mDialog.getText());
                                        file.renameTo(new File(newPath));
                                        recordDb.update(mEntry.getFilePath(), newPath);
                                        RecordDb.destroyInstance();
                                        cancelEdit();
                                        dialog.dismiss();
                                        FileSyncContentProvider.renameFile(getActivity(),oldPath,newPath);
                                    }

                                }
                            });

                            mDialog.setNegativeButton(new Dialog.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cancelEdit();
                                }
                            });

                            mDialog.show(recordedAdapter.getItem(getSelectedIndexs()[0]),true);

                            break;
                        default:
                            break;
                    }
                }

                @Override
                public boolean onLongClick(int pos, String tag) {
                    return false;
                }
            });
        }
    }
    
    
    private OnItemLongClickListener longClickListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			System.out.println("chang an");
			if(recordedAdapter.isActionMode())
				return false;
			initSelectItem();
			if (recordedAdapter.isActionMode()) {
				boolean flag = recordSelectFlag.get(position);
                recordSelectFlag.set(position, !flag);
                ((LeCheckBox)view.findViewById(R.id.item_select)).setChecked(!flag,true);
                changeSelectStatus();
				updateSherlockUI();
			}
			return true;
		}
	};


    private OnClickListener getUpdateRecordListener(){
        return  new OnClickListener(){
            @Override
            public void onClick(View v) {
                final EditRecordNameDialog mdialog = new EditRecordNameDialog(getActivity());
                mdialog.setPositiveButton(new Dialog.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (FileTool.acceptNewFileName(mdialog.getText() )) {
                            if(RecordTool.canSave(getActivity(), mdialog.getText())) {
                                RecordApp.getInstance().setRecordName(mdialog.getText());
                            }
                        }else{
                            Toast.makeText(getActivity(),R.string.create_file_fail,Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                mdialog.show(null,false);
            }
        };
    }

	private OnClickListener getCancelListener() {
		return new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				cancelEdit();
			}
		};
	}


    private void cancelEdit(){
        refreshRecordList();
        recordedAdapter.setActionMode(false);
        getActivity().invalidateOptionsMenu();
        onPrepareOptionsMenu();
        updateActionBarAndBottomLayout();
        updateSherlockUI();
    }

    private OnClickListener getSelectAllListener() {
        return  new OnClickListener() {
            @Override
                public void onClick(View v) {
                if(getSelectedCount()==recordedAdapter.getCount()){
                    //TODO
                    recordedAdapter.clearSelectFlag();
                }else{
                    recordedAdapter.selectAllFlag();
                }
            }
        };
    }

	private OnClickListener getSelectModeListener() {
		return new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				initSelectItem();
			}
		};
	}
	
	private void initSelectItem(){
		getActivity().findViewById(R.id.bottom_widget).setVisibility(View.VISIBLE);
		getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
		recordedAdapter.setActionMode(true);
        recordSelectFlag = recordedAdapter.getRecordSelectFlag();
		onPrepareOptionsMenu();
		updateSherlockUI();
        backView.setVisibility(View.GONE);
	}

    public void changeSelectStatus(){
        if(selectAllView!=null){
            if(getSelectedCount()!=recordedAdapter.getCount()){

                ((TextView)((ViewGroup)selectAllView).getChildAt(0)).setText(R.string.select_all);
            }else{
                ((TextView)((ViewGroup)selectAllView).getChildAt(0)).setText(R.string.select_none);
            }

            updateSherlockUI();
        }
    }

	private OnItemClickListener getRecordItemClickListener() {
		return new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				if (recordedAdapter.isActionMode()) {
					boolean flag = recordSelectFlag.get(position);
                    recordSelectFlag.set(position, !flag);
                    ((LeCheckBox)view.findViewById(R.id.item_select)).setChecked(!flag,true);
                    changeSelectStatus();
					updateSherlockUI();
				} else {

                    if(recordedAdapter.getItemViewType(position)== RecorderAdapter.ITEM_TYPE_CALL_SET){
                        recordedAdapter.setShowCallRecord(true);
                        recordedAdapter.setRecordList(RecordDb.getInstance(getActivity()).getCallRecords());
                        recordedAdapter.notifyDataSetChanged();
                        onPrepareOptionsMenu();
                        updateActionBarAndBottomLayout();
                        return;
                    }

                    if(!RecordTool.canClick(500)){
                        return;
                    }

					Intent intent = new Intent(getActivity(), PlayRecordActivity.class);
					intent.putExtra(PlayRecordActivity.RECORD_ENTRY, (RecordEntry) (parent.getItemAtPosition(position)));
					getActivity().startActivity(intent);
					getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
				}
			}
		};
	}

	protected void updateSherlockUI() {
		int selectCount = getSelectedCount();
        if(recordedAdapter.isActionMode()) {

            leBottomWidget.setEnable(0,selectCount > 0);
            leBottomWidget.setEnable(1,selectCount > 0);
            leBottomWidget.setEnable(2,selectCount == 1);

            getActivity().findViewById(R.id.bottom_widget).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
        }else{
            getActivity().findViewById(R.id.bottom_widget).setVisibility(View.GONE);
            if(recordedAdapter.isShowCallRecord()) {
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
            }else{
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.VISIBLE);
            }
        }
	}

    protected void updateActionBarAndBottomLayout(){
        if(recordedAdapter.isShowCallRecord()){
            getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
//            getActivity().getActionBar().getCustomView().findViewById(R.id.back).setVisibility(View.VISIBLE);
        }else{
//            getActivity().getActionBar().getCustomView().findViewById(R.id.back).setVisibility(View.GONE);
            if(recordedAdapter.isActionMode()) {
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
            }else{
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.VISIBLE);
            }
        }
    }
	
	protected void setEnabled(View view,boolean enable){
		if(view instanceof ViewGroup){
			view.setEnabled(enable);
			for(int i=0;i<((ViewGroup)view).getChildCount();i++){
				setEnabled(((ViewGroup)view).getChildAt(i), enable);
			}
		}else{
			view.setEnabled(enable);
		}
	}

	public void refreshRecordList() {

        if(isCallRecordUI()){
            return;
        }

		MediaRecorderState state = RecordApp.getInstance().getmState();

		if (MediaRecorderState.RECORDING == state || MediaRecorderState.PAUSED == state /*|| MediaRecorderState.STOPPED == state*/) {
            if(recordVF.getDisplayedChild()!=PAGE_SHOW_RECORDING)
			    recordVF.setDisplayedChild(PAGE_SHOW_RECORDING);

            if(MediaRecorderState.PAUSED == state) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(recordViewMask, "alpha", 0, 1);
                animator.setDuration(400);

                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        recordViewMask.setVisibility(View.VISIBLE);
                        recordViewMask.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animator.start();
            }else{
                final ObjectAnimator animator = ObjectAnimator.ofFloat(recordViewMask, "alpha", 1, 0);
                animator.setDuration(400);

                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        if(recordingView.getVisibility()==View.GONE){
                            animator.cancel();
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        recordViewMask.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animator.start();
            }

		} else {

            boolean isShowList = false;

			RecordDb db = RecordDb.getInstance(getActivity());

            if(recordedAdapter.isShowCallRecord()){
                if(db.getCallRecordCounts()>0){
                    recordedAdapter.setRecordList(db.getCallRecords());
                    isShowList = true;
                }else{
                    recordedAdapter.setShowCallRecord(false);
                    recordedAdapter.setRecordList(db.getNormalRecords());
                    if(db.getNormalRecordCounts()>0) {
                        isShowList = true;
                    }else{
                        isShowList = false;
                    }
                }
            }else{
                recordedAdapter.setRecordList(db.getNormalRecords());
                if(db.getNormalRecordCounts()+db.getCallRecordCounts()>0){
                    isShowList = true;
                }else{
                    isShowList = false;
                }
            }

			if (isShowList) {
				if (recordVF != null&&recordVF.getDisplayedChild()!=PAGE_SHOW_RECORD_LIST)
					recordVF.setDisplayedChild(PAGE_SHOW_RECORD_LIST);
			    recordedAdapter.notifyDataSetChanged();
			} else {
				if (recordVF != null&&recordVF.getDisplayedChild()!=PAGE_NO_RECORD)
					recordVF.setDisplayedChild(PAGE_NO_RECORD);
			}
            recordedAdapter.setHasCall(db.getCallRecordCounts() > 0);
		}
        updateActionBarAndBottomLayout();
		onPrepareOptionsMenu();


	}

    public boolean onBackPressed() {

        if(recordedAdapter.isShowCallRecord()){
            recordedAdapter.setActionMode(false);
            recordedAdapter.setShowCallRecord(false);
            recordedAdapter.setRecordList(RecordDb.getInstance(getActivity()).getNormalRecords());
            recordedAdapter.notifyDataSetChanged();
            updateSherlockUI();
            updateActionBarAndBottomLayout();
            onPrepareOptionsMenu();
            return  true;
        }

        return false;
    }

	@Override
	public void onClick(View v) {
//		switch (v.getId()) {
//		case R.id.send_btn:
//			ArrayList<Uri> uris = ProviderTool.getShareUris(getSelectedPaths());
//			boolean multiple = uris.size() > 1;
//			Intent share = new Intent(!multiple ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
//			share.setType("audio/*");
//			share.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share));
//			share.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.record_files));
//
//			if (multiple) {
//				share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
//			} else {
//				share.putExtra(Intent.EXTRA_STREAM, uris.get(0));
//			}
//
//			startActivity(Intent.createChooser(share, getActivity().getTitle()));
//			break;
//		case R.id.dele_btn:
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//            builder.setTitle("你确定要删除该录音吗")
//                    .setPositiveButton("删除",new Dialog.OnClickListener(){
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            recordedAdapter.setActionMode(false);
//                            int[] selecteds = getSelectedIndexs();
//                            for (int i = 0; i < selecteds.length; i++) {
//                                String path = recordedAdapter.getItem(selecteds[i]).getFilePath();
//                                File delFile = new File(path);
//                                delFile.delete();
//                            }
//                            refreshRecordList();
//                            updateSherlockUI();
//
//                        }
//                    })
//                    .setNegativeButton("取消", new Dialog.OnClickListener(){
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            recordedAdapter.setActionMode(false);
//                            refreshRecordList();
//                            updateSherlockUI();
//                        }
//                    } )
//                    .create()
//                    .show();
//
//			break;
//		case R.id.rename_btn:
//            final EditRecordNameDialog mDialog = new EditRecordNameDialog(getActivity());
//            mDialog.setPositiveButton(new Dialog.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//
//                    RecordEntry mEntry = mDialog.getEntry();
//
//                    File file = new File(mEntry.getFilePath());
//                    String fileName = RecordTool.getRecordName(mEntry.getFilePath());
//                    if (fileName.equalsIgnoreCase(mDialog.getText())) {
//
//                    } else if (RecordTool.canSave(getActivity(), mDialog.getText())) {
//                        RecordDb recordDb = RecordDb.getInstance(getActivity());
//                        String newPath = mEntry.getFilePath().replace(fileName, mDialog.getText());
//                        file.renameTo(new File(newPath));
//                        recordDb.update(mEntry.getFilePath(), newPath);
//                        RecordDb.destroyInstance();
//                        cancelEdit();
//                        dialog.dismiss();
//                    }
//
//                }
//            });
//
//            mDialog.setNegativeButton(new Dialog.OnClickListener() {
//
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    cancelEdit();
//                }
//            });
//
//            mDialog.show(recordedAdapter.getItem(getSelectedIndexs()[0]),true);
//			break;
//		default:
//			break;
//		}
	}

	public String[] getSelectedPaths() {
		String[] path = new String[getSelectedCount()];
        RecordTool.loge(this.getClass().getSimpleName(),"count:"+getSelectedCount());
		int cursor = 0;
        int[] selecteds = getSelectedIndexs();
		for (int i = 0; i < selecteds.length; i++) {
            path[cursor++] = recordedAdapter.getItem(selecteds[i]).getFilePath();
		}

		return path;
	}

	public int[] getSelectedIndexs() {
		int[] flagIndexs = new int[getSelectedCount()];
		int cursor = 0;
		for (int i = 0; i < recordSelectFlag.size(); i++) {
			if (recordSelectFlag.get(i)) {
				flagIndexs[cursor++] = i;
			}
		}

		return flagIndexs;
	}

	public int getSelectedCount() {
        if(recordSelectFlag==null){
            return 0;
        }

		int count = 0;
		for (boolean flag : recordSelectFlag) {
			if (flag)
				count++;
		}
		return count;
	}

}
