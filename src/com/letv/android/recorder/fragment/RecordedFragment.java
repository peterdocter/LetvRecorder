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
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.app.ActionBar;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.letv.android.recorder.*;
import com.letv.android.recorder.R;
import com.letv.android.recorder.provider.ProviderTool;
import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.service.Recorder;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.tool.FileSyncContentProvider;
import com.letv.android.recorder.tool.FileTool;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.android.recorder.tool.StatusBarTool;
import com.letv.android.recorder.widget.ActionBarTool;
import com.letv.android.recorder.widget.EditRecordNameDialog;
import com.letv.android.recorder.widget.RecordingView;
import com.letv.android.recorder.service.RecorderService;
import com.letv.leui.widget.LeBottomWidget;
import com.letv.leui.widget.LeCheckBox;
import com.letv.leui.widget.LeTopSlideToastHelper;
import com.letv.leui.widget.LeBottomSheet;

public class RecordedFragment extends Fragment implements OnClickListener {

    static String TAG = "RecordedFragment";
    private View rootView = null;
    private ViewFlipper recordVF;
    private ListView recordList;
    private TextView recordTime;
    //	private TextView recordStartTime;
    private TextView recordName;
    private View updateName, recordViewMask;
    private RecordingView recordingView;

    private RecorderAdapter recordedAdapter;


    private List<Boolean> recordSelectFlag;

    private View cancelView, backView;
    private MenuItem /*selectMenu,*/selectAllMenu, selectNoneMenu;

    private final int PAGE_NO_RECORD = 0;
    private final int PAGE_SHOW_RECORD_LIST = 1;
    private final int PAGE_SHOW_RECORDING = 2;

    protected LeBottomWidget leBottomWidget;

    private ActionMode mActionMode = null;
    private ActionBar actionBar;

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
        RecordTool.e(TAG, "onCreate: !isCallRecordUI = " + !isCallRecordUI());
        actionBar = getActivity().getActionBar();
        actionBar.setTitle(R.string.record_note);
        actionBar.setBottomLineDrawable(getResources().getDrawable(R.color.record_list_actionbar_color));
        actionBar.setBottomLineHight(1);
        if (!isCallRecordUI()) {
            recordedAdapter = new RecorderAdapter(getActivity(), null, this);
        }
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
//        RecordTool.e(TAG,"onResume:!isCallRecordUI()"+!isCallRecordUI()
//                +"\n!((AbsRecorderActivity)getActivity()).isFistTime():"+!((AbsRecorderActivity)getActivity()).isFistTime()
//                +"\n!recordedAdapter.isActionMode():"+!recordedAdapter.isActionMode());
        if (!isCallRecordUI() && !recordedAdapter.isActionMode()) {
            RecordTool.e(TAG, "onResumerefreshRecordList");
            refreshRecordList();
        }
        super.onResume();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RecordTool.e(TAG, "onCreateView");
        rootView = inflater.inflate(R.layout.fragment_recorded, container, false);
        recordVF = (ViewFlipper) rootView.findViewById(R.id.recordVF);
        recordList = (ListView) rootView.findViewById(R.id.record_list);
//        recordList.setOverScrollEnabled(false);
        recordTime = (TextView) rootView.findViewById(R.id.record_time);
//		recordStartTime = (TextView) rootView.findViewById(R.id.record_start_time);
        recordName = (TextView) rootView.findViewById(R.id.record_title);
        recordingView = (RecordingView) rootView.findViewById(R.id.recording_view);
        updateName = rootView.findViewById(R.id.update_name);
        recordViewMask = rootView.findViewById(R.id.record_pause_mask);
        if (isCallRecordUI()) {
            recordVF.setDisplayedChild(PAGE_SHOW_RECORDING);
        }
        if (((AbsRecorderActivity) getActivity()).isFistTime()) {
            recordVF.setDisplayedChild(PAGE_SHOW_RECORDING);
            fistTimeRecordTimeUI();
        }
        return rootView;
    }

    public void setFirstTime(boolean firstTime) {
        if (firstTime) {
            recordVF.setDisplayedChild(PAGE_SHOW_RECORDING);
        }
    }

    public void fistTimeRecordTimeUI() {
        String newStr = RecordTool.recordTimeFormat(0);
        recordTime.setText(newStr);
        recordName.setText(RecordTool.getNewRecordName(getActivity()));
    }

    /**
     * @param recordTimeMillis
     */
    public void updateRecordTimeUI(long recordTimeMillis, float db) {
        RecordTool.e(TAG, "updateRecordTimeUI:recordTimeMillis=" + recordTimeMillis + " db:" + db);
        if (!isDetached()) {
            if (recordTime != null) {
                String newStr = RecordTool.recordTimeFormat(recordTimeMillis);
                if (!recordTime.getText().toString().equals(newStr))
                    recordTime.setText(newStr);
            }
            if (recordName != null) {
                String newStr = RecordApp.getInstance().getRecordName();
                if (!recordName.getText().toString().equals(newStr)) {
                    recordName.setText(RecordApp.getInstance().getRecordName());
                }
            }
            if (recordingView != null) {
                RecordTool.e(TAG, "recordingView:" + recordingView);
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

    public void resumeRecording() {
        if (recordingView != null) {
            recordingView.resumeRecording();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        RecordTool.e(TAG, "onActivityCreated:" + isCallRecordUI());
        if (!isCallRecordUI()) {
            TypedArray actionbarSizeTypedArray = getActivity().obtainStyledAttributes(new int[]{
                    android.R.attr.actionBarSize
            });

            int h = (int) actionbarSizeTypedArray.getDimension(0, 0);
//			recordList.setPadding(0, 0, 0, 0);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recordList.getLayoutParams();
            params.topMargin = h;
            recordList.setLayoutParams(params);
            recordList.setAdapter(recordedAdapter);
            recordedAdapter.setListView(recordList);
            if (!((AbsRecorderActivity) getActivity()).isFistTime()) {
                RecordTool.e(TAG, "onActivityCreated:+refreshRecordList");
                refreshRecordList();
            }
            recordList.setOnItemClickListener(getRecordItemClickListener());
            recordList.setOnItemLongClickListener(longClickListener);
            updateName.setOnClickListener(getUpdateRecordListener());
            recordVF.setInAnimation(getActivity(), android.R.anim.fade_in);
            recordVF.setOutAnimation(getActivity(), android.R.anim.fade_out);

            leBottomWidget = (LeBottomWidget) getActivity().findViewById(R.id.bottom_widget);
            initBottomWidget();
        }

        super.onActivityCreated(savedInstanceState);
    }

    private void initBottomWidget() {
        if (leBottomWidget != null) {
            leBottomWidget.setModeAndTabCount(LeBottomWidget.MODE_ICON_ONLY, 3);
            leBottomWidget.addTab(0, com.android.internal.R.drawable.le_bottom_btn_icon_share_white, getResources().getString(R.string.share));
            leBottomWidget.addTab(1, com.android.internal.R.drawable.le_bottom_btn_icon_delete_white, getResources().getString(R.string.delete));
            leBottomWidget.addTab(2, com.android.internal.R.drawable.le_bottom_btn_icon_edit_white, getResources().getString(R.string.rename));
            leBottomWidget.setOnClickAndLongClickListener(new LeBottomWidget.OnClickAndLongClickListener() {
                @Override
                public void onClick(int pos, String tag) {
                    switch (pos) {
                        case 0:
                            ArrayList<Uri> uris = ProviderTool.getShareUris(getSelectedPaths());
                            boolean multiple = uris.size() > 1;
                            int shareSelect = uris.size();
                            if (shareSelect == 0) {
                                LeTopSlideToastHelper.getToastHelper(getActivity(), LeTopSlideToastHelper.LENGTH_SHORT,
                                        getResources().getString(R.string.no_selected_share), null,
                                        null, null,
                                        null).show();
                                return;
                            }
                            Intent share = new Intent(shareSelect <= 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE);
                            share.setType("audio/*");
                            share.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share));
                            share.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.record_files));

                            if (multiple) {
                                share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                            } else {
                                share.putExtra(Intent.EXTRA_STREAM, uris.get(0));
                            }

                            startActivity(share);
                            break;
                        case 1:
//                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            final int[] selecteds = getSelectedIndexs();

                            final LeBottomSheet mBottomSheet = new LeBottomSheet(getActivity());
                            View.OnClickListener delete_listener = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    for (int i = 0; i < selecteds.length; i++) {
                                        int type = recordedAdapter.getItemViewType(selecteds[i]);
                                        if (type == RecorderAdapter.ITEM_TYPE_CALL_SET) {
                                            RecordDb db = RecordDb.getInstance(getActivity());
                                            List<RecordEntry> callRecords = db.getCallRecords();
                                            if (callRecords != null && callRecords.size() > 0) {
                                                for (RecordEntry call : callRecords) {
                                                    String path = call.getFilePath();
                                                    File delFile = new File(path);
                                                    delFile.delete();
                                                    FileSyncContentProvider.removeImageFromLib(getActivity(),
                                                            path);
                                                }
                                            }
                                        } else {
                                            String path = recordedAdapter.getItem(selecteds[i]).getFilePath();
                                            File delFile = new File(path);
                                            delFile.delete();
                                            FileSyncContentProvider.removeImageFromLib(getActivity(),
                                                    path);
                                        }
                                    }
                                    refreshRecordList();
                                    updateSherlockUI();
                                    if (recordedAdapter.isActionMode()) {
                                        recordedAdapter.setActionMode(true);
                                    }
                                    if (mActionMode != null) {
                                        mActionMode.invalidate();
                                    }
                                    mBottomSheet.disappear();
                                }
                            };
                            View.OnClickListener cancel_listener = new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    mBottomSheet.disappear();
                                }
                            };
                            mBottomSheet.setStyle(
                                    LeBottomSheet.SWITCH_BUTTON_STYLE_DIY,
                                    delete_listener,
                                    cancel_listener,
                                    null,
                                    new String[]{String.format(getResources().getString(R.string.delete_record_dialog_title), selecteds.length), "取消"},
                                    null,
                                    null,
                                    null,
                                    getActivity().getResources().getColor(R.color.defalut_red),
                                    false
                            );
                            mBottomSheet.appear();
                            break;
                        case 2:

                            int type = recordedAdapter.getItemViewType(getSelectedIndexs()[0]);
                            if (type == RecorderAdapter.ITEM_TYPE_CALL_SET) {
                                LeTopSlideToastHelper.getToastHelper(getActivity(), LeTopSlideToastHelper.LENGTH_SHORT,
                                        getResources().getString(R.string.app_keyword_call_record), null,
                                        null, null,
                                        null).show();
                                return;
                            }
                            final EditRecordNameDialog mDialog = new EditRecordNameDialog(getActivity());
                            mDialog.setPositiveButton(new Dialog.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    RecordEntry mEntry = mDialog.getEntry();

                                    File file = new File(mEntry.getFilePath());
                                    String fileName = RecordTool.getRecordName(mEntry.getFilePath());
                                    if (fileName.equalsIgnoreCase(mDialog.getText())) {
                                        LeTopSlideToastHelper.getToastHelper(getActivity(), LeTopSlideToastHelper.LENGTH_SHORT,
                                                getResources().getString(R.string.no_change_recordname), null,
                                                null, null,
                                                null).show();
                                    } else if (RecordTool.canSave(getActivity(), mDialog.getText())) {
                                        RecordDb recordDb = RecordDb.getInstance(getActivity());
                                        String oldPath = mEntry.getFilePath();
                                        String newPath = mEntry.getFilePath().replace(fileName, mDialog.getText());
                                        file.renameTo(new File(newPath));
                                        recordDb.update(mEntry.getFilePath(), newPath);
                                        RecordDb.destroyInstance();
//                                        cancelEdit();
                                        dialog.dismiss();
                                        FileSyncContentProvider.renameFile(getActivity(), oldPath, newPath);
                                        refreshRecordList();
                                        changeSelectStatus();
                                        mActionMode.invalidate();
                                    }

                                }
                            });

                            mDialog.setNegativeButton(new Dialog.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
//                                    cancelEdit();
                                }
                            });

                            mDialog.show(recordedAdapter.getItem(getSelectedIndexs()[0]), true);

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
            RecordTool.e(TAG, "longClickListener");
            if (recordedAdapter.isActionMode())
                return false;
            initSelectItem();
//            TransitionManager.beginDelayedTransition(parent,ActionBarTool.autoTransition);//引起长按listView后无法多选
            getActivity().startActionMode(mCallback);
            RecordApp.getInstance().setActionMode(recordedAdapter.isActionMode());
            if (recordedAdapter.isActionMode()) {
                boolean flag = recordSelectFlag.get(position);
                recordSelectFlag.set(position, !flag);
                ((LeCheckBox) view.findViewById(R.id.item_select)).setChecked(!flag, true);
                changeSelectStatus();
                updateSherlockUI();
                mActionMode.invalidate();
            }
            return true;
        }
    };

    private ActionMode.Callback mCallback = new ActionMode.Callback() {

        @Override
        public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
            RecordTool.e(TAG, "onPrepareActionMode");
            String itemXliff = getActivity().getResources().getString(R.string.select_item_xliff);
            mActionMode.setTitle(getSelectedCount() == 0 ? getActivity().getResources().getString(R.string.please_select_reoord) : String.format(itemXliff, getSelectedCount()));

            if (recordedAdapter.isShowCallRecord()) {
//	          backView.setVisibility(View.VISIBLE);
            } else {
//	          backView.setVisibility(View.GONE);
            }
            MediaRecorderState state = RecordApp.getInstance().getmState();
            if (MediaRecorderState.RECORDING == state || MediaRecorderState.PAUSED == state || MediaRecorderState.STOPPED == state) {
                actionBar.hide();
            } else {
                actionBar.show();
                if (recordedAdapter.isActionMode()) {
                    if (getSelectedCount() != recordedAdapter.getCount()) {
                        selectAllMenu.setVisible(true);
                        selectNoneMenu.setVisible(false);
                    } else {
                        selectAllMenu.setVisible(false);
                        selectNoneMenu.setVisible(true);
                    }
                } else {
                    if (recordedAdapter.getCount() == 0 || (recordedAdapter.getCount() == 1
                            && recordedAdapter.getItemViewType(0) == RecorderAdapter.ITEM_TYPE_CALL_SET)) {
                        getActivity().findViewById(R.id.bottom_widget).setVisibility(View.GONE);
                        getActivity().findViewById(R.id.record_control_layout).setVisibility(View.VISIBLE);
                    }
                }
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            cancelEdit();
            RecordApp.getInstance().setActionMode(false);
            mActionMode = null;
        }

        @Override
        public boolean onCreateActionMode(ActionMode arg0, Menu menu) {
            mActionMode = arg0;
            MenuInflater menuInflater = getActivity().getMenuInflater();
            menuInflater.inflate(R.menu.recorder_menu, menu);
            selectAllMenu = menu.findItem(R.id.select_all);
            selectNoneMenu = menu.findItem(R.id.select_none);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode arg0, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.select_all:
                    recordedAdapter.selectAllFlag();
                    break;
                case R.id.select_none:
                    recordedAdapter.clearSelectFlag();
                    break;
                default:
                    break;
            }
            arg0.invalidate();
            return true;
        }
    };

    private OnClickListener getUpdateRecordListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditRecordNameDialog mdialog = new EditRecordNameDialog(getActivity());
                mdialog.setPositiveButton(new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (FileTool.acceptNewFileName(mdialog.getText())) {
                            if (RecordTool.canSave(getActivity(), mdialog.getText())) {
                                RecordApp.getInstance().setRecordName(mdialog.getText());
                            }
                        } else {
                            LeTopSlideToastHelper.getToastHelper(getActivity(), LeTopSlideToastHelper.LENGTH_SHORT,
                                    getResources().getString(R.string.create_file_fail), null,
                                    null, null,
                                    null).show();
                        }
                    }
                });
                mdialog.setNegativeButton(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                mdialog.show(null, false);
            }
        };
    }

    private void cancelEdit() {
        refreshRecordList();
        recordedAdapter.setActionMode(false);
        getActivity().invalidateOptionsMenu();
        updateActionBarAndBottomLayout();
        updateSherlockUI();
    }

    private OnClickListener getSelectAllListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getSelectedCount() == recordedAdapter.getCount()) {
                    //TODO
                    recordedAdapter.clearSelectFlag();
                } else {
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

    private void initSelectItem() {
        RecordTool.e(TAG, "initSelectItem");
        getActivity().findViewById(R.id.bottom_widget).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
        recordedAdapter.setActionMode(true);
        recordSelectFlag = recordedAdapter.getRecordSelectFlag();
        getActivity().invalidateOptionsMenu();
        updateSherlockUI();
    }

    public void changeSelectStatus() {
        RecordTool.e(TAG, "changeSelectStatus");
        getActivity().invalidateOptionsMenu();
        updateSherlockUI();
    }

    private OnItemClickListener getRecordItemClickListener() {
        return new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (recordedAdapter.isActionMode()) {
                    boolean flag = recordSelectFlag.get(position);
                    recordSelectFlag.set(position, !flag);
                    ((LeCheckBox) view.findViewById(R.id.item_select)).setChecked(!flag, true);
                    recordedAdapter.notifyDataSetChanged();
                    changeSelectStatus();
//					updateSherlockUI();
                    mActionMode.invalidate();
                } else {

                    if (recordedAdapter.getItemViewType(position) == RecorderAdapter.ITEM_TYPE_CALL_SET) {
                        recordedAdapter.setShowCallRecord(true);
                        recordedAdapter.setRecordList(RecordDb.getInstance(getActivity()).getCallRecords());
                        RecordTool.e("rename", "rename 1");
                        recordedAdapter.notifyDataSetChanged();
                        ActionBarTool.changeActionBar(getActivity(), true);
                        getActivity().invalidateOptionsMenu();
                        updateActionBarAndBottomLayout();
                        return;
                    }

                    if (!RecordTool.canClick(500)) {
                        return;
                    }

                    RecordTool.e("rename", "rename 2");
                    Intent intent = new Intent(getActivity(), PlayRecordActivity.class);
                    intent.putExtra(PlayRecordActivity.RECORD_ENTRY, (RecordEntry) (parent.getItemAtPosition(position)));
                    getActivity().startActivity(intent);
                    getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:

                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void updateSherlockUI() {
        RecordTool.e(TAG, "updateSherlockUI");
        int selectCount = getSelectedCount();
        if (recordedAdapter.isActionMode()) {

            leBottomWidget.setEnable(0, selectCount > 0);
            leBottomWidget.setEnable(1, selectCount > 0);
            leBottomWidget.setEnable(2, selectCount == 1);

            getActivity().findViewById(R.id.bottom_widget).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
        } else {
            getActivity().findViewById(R.id.bottom_widget).setVisibility(View.GONE);
            if (recordedAdapter.isShowCallRecord()) {
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
            } else {
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.VISIBLE);
            }
        }
    }

    protected void updateActionBarAndBottomLayout() {
        RecordTool.e(TAG, "updateActionBarAndBottomLayout");
        if (recordedAdapter.isShowCallRecord()) {
            getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
        } else {
            if (recordedAdapter.isActionMode()) {
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
            } else {
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.VISIBLE);
            }
        }
    }

    protected void setEnabled(View view, boolean enable) {
        if (view instanceof ViewGroup) {
            view.setEnabled(enable);
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setEnabled(((ViewGroup) view).getChildAt(i), enable);
            }
        } else {
            view.setEnabled(enable);
        }
    }

    public void refreshRecordList() {
        RecordTool.e(TAG, "refreshRecordList:isCallRecordUI():" + isCallRecordUI());
        if (isCallRecordUI()) {
            return;
        }

        MediaRecorderState state = RecordTool.getRecordState(getActivity());

        if (MediaRecorderState.RECORDING == state || MediaRecorderState.PAUSED == state /*|| MediaRecorderState.STOPPED == state*/) {
            if (recordVF.getDisplayedChild() != PAGE_SHOW_RECORDING)
                recordVF.setDisplayedChild(PAGE_SHOW_RECORDING);

            if (MediaRecorderState.PAUSED == state) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(recordViewMask, "alpha", 0, 1);
                animator.setDuration(400);

                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        recordViewMask.setVisibility(View.VISIBLE);
                        StatusBarTool.updateStausBar(getActivity(), true);
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
            } else {
                final ObjectAnimator animator = ObjectAnimator.ofFloat(recordViewMask, "alpha", 1, 0);
                animator.setDuration(400);
                StatusBarTool.updateStausBar(getActivity(), false);
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (recordingView.getVisibility() == View.GONE) {
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
            StatusBarTool.updateStausBar(getActivity(), false);
            boolean isShowList = false;
            RecordDb db = RecordDb.getInstance(getActivity());
            if (recordedAdapter.isShowCallRecord()) {
                if (db.getCallRecordCounts() > 0) {
                    recordedAdapter.setRecordList(db.getCallRecords());
                    isShowList = true;
                } else {
                    recordedAdapter.setShowCallRecord(false);
                    recordedAdapter.setRecordList(db.getNormalRecords());
                    if (db.getNormalRecordCounts() > 0) {
                        isShowList = true;
                    } else {
                        isShowList = false;
                    }
                }
            } else {
                recordedAdapter.setRecordList(db.getNormalRecords());
                if (db.getNormalRecordCounts() + db.getCallRecordCounts() > 0) {
                    isShowList = true;
                } else {
                    isShowList = false;
                }
            }
            if (isShowList) {
                if (recordVF != null && recordVF.getDisplayedChild() != PAGE_SHOW_RECORD_LIST)
                    recordVF.setDisplayedChild(PAGE_SHOW_RECORD_LIST);
                recordedAdapter.notifyDataSetChanged();
            } else {
                if (recordVF != null && recordVF.getDisplayedChild() != PAGE_NO_RECORD) {
                    recordVF.setDisplayedChild(PAGE_NO_RECORD);
                    if (mActionMode != null) {
                        mActionMode.finish();
                    }
                }

            }
            recordedAdapter.setHasCall(db.getCallRecordCounts() > 0);
        }
        updateActionBarAndBottomLayout();
    }

    public boolean onBackPressed() {
        if (recordedAdapter.isActionMode()) {
            cancelEdit();
            return true;
        }

        if (recordedAdapter.isShowCallRecord()) {
            recordedAdapter.setActionMode(false);
            recordedAdapter.setShowCallRecord(false);
            recordedAdapter.setRecordList(RecordDb.getInstance(getActivity()).getNormalRecords());
            recordedAdapter.notifyDataSetChanged();
            ActionBarTool.changeActionBar(getActivity(), false);
            updateSherlockUI();
            updateActionBarAndBottomLayout();
            getActivity().invalidateOptionsMenu();
            return true;
        }

        return false;
    }

    @Override
    public void onClick(View v) {

    }

    public String[] getSelectedPaths() {
        ArrayList<String> path = new ArrayList<String>();
        RecordTool.loge(this.getClass().getSimpleName(), "count:" + getSelectedCount());
        int cursor = 0;
        int[] selecteds = getSelectedIndexs();
        for (int i = 0; i < selecteds.length; i++) {
            int type = recordedAdapter.getItemViewType(selecteds[i]);
            if (type == RecorderAdapter.ITEM_TYPE_CALL_SET) {
                RecordDb db = RecordDb.getInstance(getActivity());
                List<RecordEntry> callRecords = db.getCallRecords();
                if (callRecords != null && callRecords.size() > 0) {
                    for (RecordEntry call : callRecords) {
                        path.add(call.getFilePath());
                    }
                }
            } else {
                path.add(recordedAdapter.getItem(selecteds[i]).getFilePath());
            }
        }
        String[] pathArr = new String[path.size()];
        return path.toArray(pathArr);
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
        recordSelectFlag = recordedAdapter.getRecordSelectFlag();
        if (recordSelectFlag == null) {
            return 0;
        }

        int count = 0;
        for (boolean flag : recordSelectFlag) {
            if (flag)
                count++;
        }
        return count;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (RecordApp.isFromWidget) {
            Recorder.getInstance().checkRecorderState();
            RecordApp.isFromWidget = false;
        }
    }
}
