package com.letv.android.recorder.fragment;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;
import com.letv.android.recorder.*;
import com.letv.android.recorder.provider.ProviderTool;
import com.letv.android.recorder.provider.RecordDb;
import com.letv.android.recorder.service.Recorder;
import com.letv.android.recorder.service.Recorder.MediaRecorderState;
import com.letv.android.recorder.service.RecorderService;
import com.letv.android.recorder.tool.FileSyncContentProvider;
import com.letv.android.recorder.tool.FileTool;
import com.letv.android.recorder.tool.RecordTool;
import com.letv.android.recorder.tool.StatusBarTool;
import com.letv.android.recorder.widget.ActionBarTool;
import com.letv.android.recorder.widget.EditRecordNameDialog;
import com.letv.android.recorder.widget.RecordingView;
import com.letv.leui.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordedFragment extends Fragment implements OnClickListener {

    static String TAG = "RecordedFragment";

    private final int PAGE_NO_RECORD = 0;
    private final int PAGE_SHOW_RECORD_LIST = 1;
    private final int PAGE_SHOW_RECORDING = 2;

    protected LeBottomWidget leBottomWidget;
    private ViewFlipper mViewFlipper;
    private LeListView mLeListView;
    private TextView tv_recordTime;
    private TextView tv_recordName;
    private View v_editName, v_pauseMask;
    private RecordingView mRecordingView;

    private RecorderAdapter mRecorderAdapter;

    private List<Boolean> mSelectedList;

    private MenuItem /*selectMenu,*/selectAllMenu, selectNoneMenu;

    private ActionMode mActionMode = null;
    private ActionBar actionBar;
    private int mActionBarHeight;

    private AsyncDeleteTask mAsyncDeleteTask;
    private Map<Integer, AsyncDeleteTask> mAsyncTasks;

    private TransitionInflater mInflater;
    // 是否正在做删除动画
    // 如果正在做删除动画，则action mode退出时就不应该启动 退出多选模式 的动画，否则应该做 退出多选模式 的动画
    private boolean mIsDeletingAnimRunning;

    /**
     * @param recordTimeMillis
     */
    public void updateRecordTimeUI(long recordTimeMillis, float db) {
        RecordTool.e(TAG, "updateRecordTimeUI:recordTimeMillis=" + recordTimeMillis + " db:" + db);
        if (!isDetached()) {
            if (tv_recordTime != null) {
                String newStr = RecordTool.recordTimeFormat(recordTimeMillis);
                if (!tv_recordTime.getText().toString().equals(newStr))
                    tv_recordTime.setText(newStr);
            }
            if (tv_recordName != null) {
                String newStr = RecordApp.getInstance().getRecordName();
                if (!tv_recordName.getText().toString().equals(newStr)) {
                    tv_recordName.setText(RecordApp.getInstance().getRecordName());
                }
            }
            if (mRecordingView != null) {
                RecordTool.e(TAG, "mRecordingView:" + mRecordingView);
                mRecordingView.updateRecordUI(recordTimeMillis, db);
            }
        }
    }

    public void stopRecording() {
        if (mRecordingView != null) {
            mRecordingView.stopRecording();
        }
    }

    public void startRecording() {
        if (mRecordingView != null) {
            mRecordingView.startRecording();
        }
    }

    //  whether the ListView is shown normal record or calling reocrd
    private boolean showCallingRecordUI = false;

    public boolean isShowCallingRecordUI() {
        return showCallingRecordUI;
    }

    public void setShowCallingRecordUI(boolean isCallingReocrdUI) {
        this.showCallingRecordUI = isCallingReocrdUI;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        actionBar = getActivity().getActionBar();
        actionBar.setTitle(R.string.record_note);
        actionBar.setBottomLineDrawable(getResources().getDrawable(R.color.record_list_actionbar_color));
        actionBar.setBottomLineHight(1);
        if (!isShowCallingRecordUI()) {
            mRecorderAdapter = new RecorderAdapter(getActivity(), null, this);
        }
        setHasOptionsMenu(true);
        mInflater = TransitionInflater.from(getActivity());
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recorded, container, false);
        mViewFlipper = (ViewFlipper) rootView.findViewById(R.id.recordVF);
        mLeListView = (LeListView) rootView.findViewById(R.id.record_list);
        mRecordingView = (RecordingView) rootView.findViewById(R.id.recording_view);
        tv_recordTime = (TextView) rootView.findViewById(R.id.record_time);
        tv_recordName = (TextView) rootView.findViewById(R.id.record_title);
        v_editName = rootView.findViewById(R.id.update_name);
        v_pauseMask = rootView.findViewById(R.id.record_pause_mask);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isShowCallingRecordUI()) {
            mViewFlipper.setDisplayedChild(PAGE_SHOW_RECORDING);
        }
        if (((AbsRecorderActivity) getActivity()).isFistTime()) {
            mViewFlipper.setDisplayedChild(PAGE_SHOW_RECORDING);
            fistTimeRecordTimeUI();
        }
        if (RecordApp.isFromWidget) {
            Recorder.getInstance().checkRecorderState();
            RecordApp.isFromWidget = false;
        }
    }

    public void fistTimeRecordTimeUI() {
        String newStr = RecordTool.recordTimeFormat(0);
        tv_recordTime.setText(newStr);
        tv_recordName.setText(RecordTool.getNewRecordName(getActivity()));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        if (!isShowCallingRecordUI()) {
            mActionBarHeight = StatusBarTool.getActionBarHeight(getActivity());
            initLeListView();

            v_editName.setOnClickListener(mEditNameOnClickListener);
            mViewFlipper.setInAnimation(getActivity(), android.R.anim.fade_in);
            mViewFlipper.setOutAnimation(getActivity(), android.R.anim.fade_out);

            initBottomWidget();
        }

        super.onActivityCreated(savedInstanceState);
    }

    private void initLeListView() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mLeListView.getLayoutParams();
        params.topMargin = mActionBarHeight;
        mLeListView.setLayoutParams(params);

        mLeListView.setAdapter(mRecorderAdapter);
        mRecorderAdapter.setListView(mLeListView);

        mLeListView.setSwipeListViewListener(mSwipeListViewListener);
        mLeListView.setOnItemClickListener(mItemClickListener);
        mLeListView.setOnItemLongClickListener(mLongClickListener);
    }

    private SwipeListViewListener mSwipeListViewListener = new BaseSwipeListViewListener(){

        private RecordEntry entry;
        private boolean isDismiss;

        @Override
        public int onChangeSwipeMode(int position) {
            if (mRecorderAdapter.isActionMode()) {
                return SwipeListViewHelper.SWIPE_MODE_NONE;
            }
            // the item that type is calling record.
            if (mRecorderAdapter.isHasCallRecord() && !mRecorderAdapter.isShowCallRecord() && position == 0) {
                return SwipeListViewHelper.SWIPE_MODE_NONE;
            }
            return super.onChangeSwipeMode(position);
        }

        @Override
        public void onDismiss(int[] reverseSortedPositions) {
            List<RecordEntry> recordList = mRecorderAdapter.getRecordList();
            // whether offset is 1 when there has item of calling record, or offset is 0.
            int offset = 0;
            if (mRecorderAdapter.isHasCallRecord() && !mRecorderAdapter.isShowCallRecord()) {
                offset = 1;
            }
            int position = reverseSortedPositions[0] - offset;

            entry = recordList.get(position);
            isDismiss = true;
        }

        @Override
        public void onClosed(int position, boolean fromRight) {
            if (isDismiss) {
                runDeleteAsyncTask(entry);
                isDismiss = false;
            }
        }
    };

    private void runDeleteAsyncTask(RecordEntry entry) {
        if (mAsyncTasks == null) {
            // put async task, cancel it on onDestroy method
            mAsyncTasks = new HashMap<>();
        }

        if (mAsyncTasks.containsKey(entry.hashCode())) {
            return;
        }

        // remove record from disk and database
        mAsyncDeleteTask = new AsyncDeleteTask(entry);
        mAsyncTasks.put(entry.hashCode(), mAsyncDeleteTask);
        mAsyncDeleteTask.execute();
    }

    private OnItemClickListener mItemClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (mRecorderAdapter.isActionMode()) {
                boolean flag = mSelectedList.get(position);
                mSelectedList.set(position, !flag);
                mRecorderAdapter.notifyDataSetChanged();
                changeSelectStatus();
                mActionMode.invalidate();
            } else {

                if (mRecorderAdapter.getItemViewType(position) == RecorderAdapter.ITEM_TYPE_CALL_SET) {
                    mRecorderAdapter.setShowCallRecord(true);
                    mRecorderAdapter.setRecordList(RecordDb.getInstance(getActivity()).getCallRecords());
                    mRecorderAdapter.notifyDataSetChanged();
                    ActionBarTool.changeActionBar(getActivity(), true);
                    getActivity().invalidateOptionsMenu();
                    updateActionBarAndBottomLayout();
                    return;
                }

                if (!RecordTool.canClick(500)) {
                    return;
                }

                Intent intent = new Intent(getActivity(), PlayRecordActivity.class);
                intent.putExtra(PlayRecordActivity.RECORD_ENTRY, (RecordEntry) (parent.getItemAtPosition(position)));
                getActivity().startActivity(intent);
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }

        }
    };

    private OnItemLongClickListener mLongClickListener = new OnItemLongClickListener() {

        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view,
                                       int position, long id) {
            if (mRecorderAdapter.isActionMode()) {
                return false;
            }

            getActivity().startActionMode(mCallback);

            // record which item is clicked
            boolean flag = mSelectedList.get(position);
            mSelectedList.set(position, !flag);
            mRecorderAdapter.notifyDataSetChanged();

            enterActionMode();
            return true;
        }

    };

    private OnClickListener mEditNameOnClickListener = new OnClickListener() {
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

    private void initBottomWidget() {
        leBottomWidget = (LeBottomWidget) getActivity().findViewById(R.id.bottom_widget);
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
                                        int type = mRecorderAdapter.getItemViewType(selecteds[i]);
                                        RecordDb db = RecordDb.getInstance(getActivity());

                                        if (type == RecorderAdapter.ITEM_TYPE_CALL_SET) {
                                            List<RecordEntry> callRecords = db.getCallRecords();
                                            RecordEntry entry = callRecords.get(selecteds[i]);
                                            runDeleteAsyncTask(entry);
                                        } else {
                                            List<RecordEntry> normalRecords = db.getNormalRecords();
                                            RecordEntry entry = normalRecords.get(selecteds[i]);
                                            runDeleteAsyncTask(entry);
                                        }

                                    }
//                                    refreshRecordList();
//                                    updateSherlockUI();
//                                    if (mRecorderAdapter.isActionMode()) {
//                                        mRecorderAdapter.setActionMode(true);
//                                    }
//                                    if (mActionMode != null) {
//                                        mActionMode.invalidate();
//                                    }
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
                                    new String[]{String.format(getResources().getString(R.string.delete_record_dialog_title), selecteds.length), getResources().getString(R.string.cancel)},
                                    null,
                                    null,
                                    null,
                                    getActivity().getResources().getColor(R.color.defalut_red),
                                    false
                            );
                            mBottomSheet.appear();
                            break;
                        case 2:

                            int type = mRecorderAdapter.getItemViewType(getSelectedIndexs()[0]);
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

                            mDialog.show(mRecorderAdapter.getItem(getSelectedIndexs()[0]), true);

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

    @Override
    public void onResume() {
        if (!isShowCallingRecordUI() && !mRecorderAdapter.isActionMode()) {
            tv_recordTime.setText(RecordTool.recordTimeFormat(RecorderService.recordRealDuring));
            refreshRecordList();
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        cancelNotRunTasks();
        super.onDestroy();
    }

    private void cancelNotRunTasks() {
        // cancel mAsyncDeleteTask
        if (mAsyncTasks != null && mAsyncTasks.size() > 0) {
            for (Integer id : mAsyncTasks.keySet()) {
                mAsyncTasks.get(id).cancel(true);
            }
            mAsyncTasks.clear();
        }
    }

    private ActionMode.Callback mCallback = new ActionMode.Callback() {

        @Override
        public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
            String itemXliff = getActivity().getResources().getString(R.string.select_item_xliff);
            mActionMode.setTitle(getSelectedCount() == 0 ? getActivity().getResources().getString(R.string.please_select_reoord) : String.format(itemXliff, getSelectedCount()));

            MediaRecorderState state = RecordApp.getInstance().getmState();
            if (MediaRecorderState.RECORDING == state || MediaRecorderState.PAUSED == state || MediaRecorderState.STOPPED == state) {
                actionBar.hide();
            } else {
                actionBar.show();
                if (mRecorderAdapter.isActionMode()) {
                    if (getSelectedCount() != mRecorderAdapter.getCount()) {
                        selectAllMenu.setVisible(true);
                        selectNoneMenu.setVisible(false);
                    } else {
                        selectAllMenu.setVisible(false);
                        selectNoneMenu.setVisible(true);
                    }
                } else {
                    if (mRecorderAdapter.getCount() == 0 || (mRecorderAdapter.getCount() == 1
                            && mRecorderAdapter.getItemViewType(0) == RecorderAdapter.ITEM_TYPE_CALL_SET)) {
                        getActivity().findViewById(R.id.bottom_widget).setVisibility(View.GONE);
                        getActivity().findViewById(R.id.record_control_layout).setVisibility(View.VISIBLE);
                    }
                }
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            exitActionMode();
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
                    mRecorderAdapter.selectAllFlag();
                    break;
                case R.id.select_none:
                    mRecorderAdapter.clearSelectFlag();
                    break;
                default:
                    break;
            }
            arg0.invalidate();
            return true;
        }
    };

    private void enterActionMode() {
        RecordApp.getInstance().setActionMode(true);
        TransitionManager.beginDelayedTransition(mLeListView, createEnterTransition());
        initSelectItem();
        changeSelectStatus();
        mActionMode.invalidate();
    }

    private void exitActionMode() {
        if (!mIsDeletingAnimRunning) {
            RecordApp.getInstance().setActionMode(false);
            mRecorderAdapter.setActionMode(false);
            // exit action mode anim
            TransitionManager.beginDelayedTransition(mLeListView, createExitTransition());
            getActivity().invalidateOptionsMenu();
            updateActionBarAndBottomLayout();
            updateSherlockUI();
            mRecorderAdapter.clearSelectFlag();
            mActionMode = null;
        }
    }

    private Transition createEnterTransition() {
        Transition transitionEnterSelectMode = mInflater
                .inflateTransition(com.android.internal.R.transition.listview_item_add_checkbox);
        return transitionEnterSelectMode;
    }

    private Transition createExitTransition() {
        Transition transitionExitSelectMode = mInflater
                .inflateTransition(com.android.internal.R.transition.listview_item_delete_checkbox);
        return transitionExitSelectMode;
    }

    private TransitionSet createDeleteItemsBottomTransition() {
        TransitionSet transitionDeleteItemsBottom = (TransitionSet)mInflater
                .inflateTransition(com.android.internal.R.transition.listview_delete_items_with_checkbox_slideup);

        final Transition transitionShowEmptyView = mInflater
                .inflateTransition(com.android.internal.R.transition.listview_show_empty_view);
        transitionShowEmptyView.addTarget(v_pauseMask);

        Transition.TransitionListener delete_listener = new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                mIsDeletingAnimRunning = true; // 正在做删除动画
                if (mActionMode != null) {
                    mActionMode.invalidate();
                }
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                mIsDeletingAnimRunning = false; // 删除动画已经做完
                if (mLeListView.getCount() - mLeListView.getFooterViewsCount() - mLeListView.getHeaderViewsCount() == 0) {
                    TransitionManager.beginDelayedTransition(mLeListView, transitionShowEmptyView);
                    mLeListView.setEmptyView(v_pauseMask);
                } else {
                    mLeListView.invalidate();
                }
            }

            @Override
            public void onTransitionCancel(Transition transition) {
                mIsDeletingAnimRunning = false;
                if (mLeListView.getCount() - mLeListView.getFooterViewsCount() - mLeListView.getHeaderViewsCount() == 0) {
                    TransitionManager.beginDelayedTransition(mLeListView, transitionShowEmptyView);
                    mLeListView.setEmptyView(v_pauseMask);
                } else {
                    mLeListView.invalidate();
                }
            }
        };
        // 给删除动画设置监听器
        transitionDeleteItemsBottom.addListener(delete_listener);

        // 将删除动画与 ListView 中的id 联系起来
        setListViewItemIdToTransition(transitionDeleteItemsBottom);
        return transitionDeleteItemsBottom;
    }

    private TransitionSet createDeleteItemsTopTransition() {
        TransitionSet transitionDeleteItemsTop = (TransitionSet)mInflater
                .inflateTransition(com.android.internal.R.transition.listview_delete_items_with_checkbox_slidedown);

        final Transition transitionShowEmptyView = mInflater
                .inflateTransition(com.android.internal.R.transition.listview_show_empty_view);
        transitionShowEmptyView.addTarget(v_pauseMask);

        Transition.TransitionListener delete_listener = new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                mIsDeletingAnimRunning = true; // 正在做删除动画
                if (mActionMode != null) {
                    mActionMode.invalidate();
                }
                mLeListView.invalidate();
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                mIsDeletingAnimRunning = false; // 删除动画已经做完
                if (mLeListView.getCount() - mLeListView.getFooterViewsCount() - mLeListView.getHeaderViewsCount() == 0) {
                    TransitionManager.beginDelayedTransition(mLeListView, transitionShowEmptyView);
                    mLeListView.setEmptyView(v_pauseMask);
                } else {
                    mLeListView.invalidate();
                }
            }

            @Override
            public void onTransitionCancel(Transition transition) {
                mIsDeletingAnimRunning = false;
                if (mLeListView.getCount() - mLeListView.getFooterViewsCount() - mLeListView.getHeaderViewsCount() == 0) {
                    TransitionManager.beginDelayedTransition(mLeListView, transitionShowEmptyView);
                    mLeListView.setEmptyView(v_pauseMask);
                } else {
                    mLeListView.invalidate();
                }
            }
        };
        // 给删除动画设置监听器
        transitionDeleteItemsTop.addListener(delete_listener);

        // 将删除动画与 ListView 中的id 联系起来
        setListViewItemIdToTransition(transitionDeleteItemsTop);
        return transitionDeleteItemsTop;
    }

    private void setListViewItemIdToTransition(TransitionSet transitionSet) {
        if (transitionSet == null)
            return;

        transitionSet.getTransitionAt(0).addTarget(R.id.back);
        TransitionSet temp = (TransitionSet) transitionSet.getTransitionAt(1);
        if (temp != null) {
            temp.getTransitionAt(0).addTarget(R.id.item_select);
        }
    }

    private void initSelectItem() {
        getActivity().findViewById(R.id.bottom_widget).setVisibility(View.VISIBLE);
        getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
        mRecorderAdapter.setActionMode(true);
        mSelectedList = mRecorderAdapter.getRecordSelectFlag();
        getActivity().invalidateOptionsMenu();
        updateSherlockUI();
    }

    public void changeSelectStatus() {
        getActivity().invalidateOptionsMenu();
        updateSherlockUI();
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
        int selectCount = getSelectedCount();
        if (mRecorderAdapter.isActionMode()) {

            leBottomWidget.setEnable(0, selectCount > 0);
            leBottomWidget.setEnable(1, selectCount > 0);
            leBottomWidget.setEnable(2, selectCount == 1);

            getActivity().findViewById(R.id.bottom_widget).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
        } else {
            getActivity().findViewById(R.id.bottom_widget).setVisibility(View.GONE);
            if (mRecorderAdapter.isShowCallRecord()) {
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
            } else {
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.VISIBLE);
            }
        }
    }

    protected void updateActionBarAndBottomLayout() {
        if (mRecorderAdapter.isShowCallRecord()) {
            getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
        } else {
            if (mRecorderAdapter.isActionMode()) {
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.GONE);
            } else {
                getActivity().findViewById(R.id.record_control_layout).setVisibility(View.VISIBLE);
            }
        }
    }

    public void refreshRecordList() {
        if (isShowCallingRecordUI() && mIsDeletingAnimRunning) {
            return;
        }

        MediaRecorderState state = RecordTool.getRecordState(getActivity());

        if (MediaRecorderState.RECORDING == state || MediaRecorderState.PAUSED == state /*|| MediaRecorderState.STOPPED == state*/) {
            if (mViewFlipper.getDisplayedChild() != PAGE_SHOW_RECORDING)
                mViewFlipper.setDisplayedChild(PAGE_SHOW_RECORDING);

            if (MediaRecorderState.PAUSED == state) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(v_pauseMask, "alpha", 0, 1);
                animator.setDuration(400);

                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        v_pauseMask.setVisibility(View.VISIBLE);
                        StatusBarTool.updateStausBar(getActivity(), true);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {}

                    @Override
                    public void onAnimationCancel(Animator animation) {}

                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });

                animator.start();

            } else {
                final ObjectAnimator animator = ObjectAnimator.ofFloat(v_pauseMask, "alpha", 1, 0);
                animator.setDuration(400);
                StatusBarTool.updateStausBar(getActivity(), false);
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        if (mRecordingView.getVisibility() == View.GONE) {
                            animator.cancel();
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        v_pauseMask.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {}

                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });

                animator.start();

            }

        } else {
            StatusBarTool.updateStausBar(getActivity(), false);
            boolean isShowList = false;
            RecordDb db = RecordDb.getInstance(getActivity());
            db.syncDBFromSdCard(getActivity());
            if (mRecorderAdapter.isShowCallRecord()) {
                if (db.getCallRecordCounts() > 0) {
                    mRecorderAdapter.setRecordList(db.getCallRecords());
                    isShowList = true;
                } else {
                    mRecorderAdapter.setShowCallRecord(false);
                    mRecorderAdapter.setRecordList(db.getNormalRecords());
                    if (db.getNormalRecordCounts() > 0) {
                        isShowList = true;
                    } else {
                        isShowList = false;
                    }
                }
            } else {
                mRecorderAdapter.setRecordList(db.getNormalRecords());
                if (db.getNormalRecordCounts() + db.getCallRecordCounts() > 0) {
                    isShowList = true;
                } else {
                    isShowList = false;
                }
            }
            if (isShowList) {
                mRecorderAdapter.notifyDataSetChanged();
                if (mViewFlipper != null && mViewFlipper.getDisplayedChild() != PAGE_SHOW_RECORD_LIST)
                    mViewFlipper.setDisplayedChild(PAGE_SHOW_RECORD_LIST);
            } else {
                if (mViewFlipper != null && mViewFlipper.getDisplayedChild() != PAGE_NO_RECORD) {
                    mViewFlipper.setDisplayedChild(PAGE_NO_RECORD);
                    if (mActionMode != null) {
                        mActionMode.finish();
                    }
                }

            }
            mRecorderAdapter.setHasCall(db.getCallRecordCounts() > 0);
        }
        updateActionBarAndBottomLayout();
    }

    public boolean onBackPressed() {
        if (mRecorderAdapter.isActionMode()) {
            exitActionMode();
            return true;
        }

        if (mRecorderAdapter.isShowCallRecord()) {
            mRecorderAdapter.setActionMode(false);
            mRecorderAdapter.setShowCallRecord(false);
            mRecorderAdapter.setRecordList(RecordDb.getInstance(getActivity()).getNormalRecords());
            mRecorderAdapter.notifyDataSetChanged();
            ActionBarTool.changeActionBar(getActivity(), false);
            updateSherlockUI();
            updateActionBarAndBottomLayout();
            getActivity().invalidateOptionsMenu();
            return true;
        }

        return false;
    }

    public String[] getSelectedPaths() {
        ArrayList<String> path = new ArrayList<String>();
        RecordTool.loge(this.getClass().getSimpleName(), "count:" + getSelectedCount());
        int cursor = 0;
        int[] selecteds = getSelectedIndexs();
        for (int i = 0; i < selecteds.length; i++) {
            int type = mRecorderAdapter.getItemViewType(selecteds[i]);
            if (type == RecorderAdapter.ITEM_TYPE_CALL_SET) {
                RecordDb db = RecordDb.getInstance(getActivity());
                List<RecordEntry> callRecords = db.getCallRecords();
                if (callRecords != null && callRecords.size() > 0) {
                    for (RecordEntry call : callRecords) {
                        path.add(call.getFilePath());
                    }
                }
            } else {
                path.add(mRecorderAdapter.getItem(selecteds[i]).getFilePath());
            }
        }
        String[] pathArr = new String[path.size()];
        return path.toArray(pathArr);
    }

    public int[] getSelectedIndexs() {
        int[] flagIndexs = new int[getSelectedCount()];
        int cursor = 0;
        for (int i = 0; i < mSelectedList.size(); i++) {
            if (mSelectedList.get(i)) {
                flagIndexs[cursor++] = i;
            }
        }

        return flagIndexs;
    }

    public int getSelectedCount() {
        mSelectedList = mRecorderAdapter.getRecordSelectFlag();
        if (mSelectedList == null) {
            return 0;
        }

        int count = 0;
        for (boolean flag : mSelectedList) {
            if (flag)
                count++;
        }
        return count;
    }

    @Override
    public void onClick(View v) {
    }

    private class AsyncDeleteTask extends AsyncTask<Void, Void, Integer> {

        private final int DELETE_SUCCESS = 0;
        private final int DELETE_FAILURE = -1;

        private final RecordEntry entry;

        public AsyncDeleteTask(RecordEntry entry) {
            super();
            this.entry = entry;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            String filePath = entry.getFilePath();
            File delFile = new File(filePath);
            if (delFile.delete()) {
                FileSyncContentProvider.removeImageFromLib(getActivity(),
                        filePath);
                RecordDb.getInstance(getActivity()).syncDBFromSdCard(getActivity());
                return DELETE_SUCCESS;
            }
            return DELETE_SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            switch (integer) {
                case DELETE_SUCCESS:
                    break;
                case DELETE_FAILURE:
                    break;
            }
            executeAnim();
            mAsyncTasks.remove(entry.hashCode());
        }

        private void executeAnim() {
            // 还有任务没有完成，不做更新的操作
            if (mAsyncTasks.size() != 1) {
                return;
            }

            if (mLeListView.getLastVisiblePosition() == mLeListView.getCount() - 1) {
                TransitionManager.beginDelayedTransition(mLeListView, createDeleteItemsTopTransition());
            } else {
                TransitionManager.beginDelayedTransition(mLeListView, createDeleteItemsBottomTransition());
            }
            refreshRecordList();
        }
    }
}
