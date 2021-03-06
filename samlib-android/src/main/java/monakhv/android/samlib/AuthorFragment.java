package monakhv.android.samlib;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;


import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.view.*;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;


import in.srain.cube.views.ptr.PtrClassicFrameLayout;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import monakhv.android.samlib.adapter.AuthorAdapter;


import monakhv.android.samlib.adapter.AuthorAnimator;
import monakhv.android.samlib.adapter.AuthorLoader;
import monakhv.android.samlib.adapter.RecyclerAdapter;
import monakhv.android.samlib.dialogs.ContextMenuDialog;
import monakhv.android.samlib.dialogs.EnterStringDialog;

import monakhv.android.samlib.dialogs.MyMenuData;

import monakhv.android.samlib.recyclerview.DividerItemDecoration;
import monakhv.android.samlib.service.MessageConstructor;
import monakhv.samlib.service.AuthorGuiState;
import monakhv.samlib.service.GuiUpdateObject;
import monakhv.android.samlib.service.UpdateLocalService;
import monakhv.android.samlib.sortorder.AuthorSortOrder;


import monakhv.samlib.db.entity.Author;
import monakhv.samlib.db.entity.SamLibConfig;
import monakhv.samlib.log.Log;
import rx.Subscriber;


import java.util.List;

import static monakhv.android.samlib.ActivityUtils.getClipboardText;

/*
 * Copyright 2014  Dmitry Monakhov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 12/5/14.
 */
public class AuthorFragment extends MyBaseAbstractFragment implements
        PtrHandler,
        ListSwipeListener.SwipeCallBack,
        RecyclerAdapter.CallBack,
        LoaderManager.LoaderCallbacks<List<Author>> {
    AuthorGuiState getGuiState() {
        return new AuthorGuiState(selectedTag, order.getOrder());
    }

    interface Callbacks {

        void onAuthorSelected(long id);

        void cleanBookSelection();

        void drawerToggle();


    }

    private static final String DEBUG_TAG = "AuthorFragment";
    private static final int AUTHOR_LOADER_ID = 201;

    private RecyclerView authorRV;
    private ProgressBar mProgressBar;
    private AuthorAdapter adapter;


    private AuthorSortOrder order;
    private GestureDetector detector;
    private PtrClassicFrameLayout mPtrFrame;
    private boolean updateAuthor = false;//true update the only selected author
    private Author author = null;//for context menu selection
    private TextView emptyTagAuthor;
    private ContextMenuDialog contextMenu;

    private View empty;
    private boolean canUpdate;
    //private SettingsHelper settingsHelper;
    private int selectedTag = SamLibConfig.TAG_AUTHOR_ALL;
    private int aId = -1;//preserve selection

    private boolean mBound;
    private int mAppBarOffset;
    private UpdateLocalService mUpdateService;
    private Menu mMenu;
    private MessageConstructor mMessageConstructor;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            UpdateLocalService.LocalBinder binder = (UpdateLocalService.LocalBinder) service;
            mUpdateService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    void setAppBarOffset(int appBarOffset) {
        mAppBarOffset = appBarOffset;
    }

    @Override
    public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {

        return
                canUpdate &&
                        (mAppBarOffset == 0) &&
                        (adapter.getItemCount() != 0) &&
                        (mUpdateService != null) &&
                        !mUpdateService.isRunning() &&
                        PtrDefaultHandler.checkContentCanBePulledDown(frame, content, header);
    }


    private Callbacks mCallbacks;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_TAG, "onCreate");
        if (savedInstanceState != null) {
            Log.d(DEBUG_TAG,"onCreate: Have not NULL Statel ");
        }

        order = AuthorSortOrder.valueOf(getSettingsHelper().getAuthorSortOrderString());
        detector = new GestureDetector(getActivity(), new ListSwipeListener(this));
        Intent service = new Intent(getActivity(), UpdateLocalService.class);
        getActivity().bindService(service, mConnection, Context.BIND_AUTO_CREATE);
        mMessageConstructor = new MessageConstructor(getActivity(), getSettingsHelper());

    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }
        mCallbacks = (Callbacks) activity;
    }

    private View view;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreateView");
        canUpdate = true;
        view = inflater.inflate(R.layout.author_fragment,
                container, false);
        authorRV = (RecyclerView) view.findViewById(R.id.authorRV);
        empty = view.findViewById(R.id.add_author_panel);
        mProgressBar = (ProgressBar) view.findViewById(R.id.authorProgress);
        emptyTagAuthor = (TextView) view.findViewById(R.id.emptyTagAuthor);


        authorRV.setHasFixedSize(true);
        final LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getActivity());
        authorRV.setLayoutManager(mLinearLayoutManager);


        authorRV.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));


        makePulToRefresh();


        authorRV.setOnTouchListener((v, event) -> {
            detector.onTouchEvent(event);
            return false;
        });
        adapter = new AuthorAdapter(this);

        authorRV.setAdapter(adapter);


        mProgressBar.setVisibility(View.VISIBLE);
        authorRV.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);
        emptyTagAuthor.setVisibility(View.GONE);
        getLoaderManager().initLoader(AUTHOR_LOADER_ID, null, this);

        authorRV.setItemAnimator(new AuthorAnimator());
        return view;

    }


    /**
     * Create initialization for pull to Refresh interface
     */
    private void makePulToRefresh() {
        mPtrFrame = (PtrClassicFrameLayout) view.findViewById(R.id.ptr_frame);
        mPtrFrame.setPtrHandler(this);
        mPtrFrame.setLastUpdateTimeKey(UpdateLocalService.PREF_NAME, UpdateLocalService.PREF_KEY_LAST_UPDATE);

    }


    @Override
    public void makeNewFlip(Author a) {
        getSamlibOperation().makeAuthorRead(a, getGuiState());

    }

    @Override
    public Loader<List<Author>> onCreateLoader(int id, Bundle args) {
        return new AuthorLoader(getActivity(), getAuthorController(), selectedTag, order.getOrder());
    }

    @Override
    public void onLoadFinished(Loader<List<Author>> loader, List<Author> data) {
        adapter.setData(data);
        mProgressBar.setVisibility(View.GONE);
        checkEmptyViewState();
    }

    private void checkEmptyViewState(){
        if (adapter.getItemCount() == 0) {
            authorRV.setVisibility(View.GONE);
            if (selectedTag == SamLibConfig.TAG_AUTHOR_ALL) {
                empty.setVisibility(View.VISIBLE);
                emptyTagAuthor.setVisibility(View.GONE);
            } else {
                empty.setVisibility(View.GONE);
                emptyTagAuthor.setVisibility(View.VISIBLE);
            }
        } else {
            empty.setVisibility(View.GONE);
            emptyTagAuthor.setVisibility(View.GONE);
            authorRV.setVisibility(View.VISIBLE);
            if (aId > 0) {
                selectAuthor(aId);
                aId = -1;
            }
        }
    }
    @Override
    public void onLoaderReset(Loader<List<Author>> loader) {
        adapter.setData(null);
    }

    /**
     * Update Author List preserve currently selected Author
     */
    private void updateAdapter() {

        if (adapter.getSelected() != null) {
            aId = adapter.getSelected().getId();
        }

        getLoaderManager().restartLoader(AUTHOR_LOADER_ID, null, this);

    }

    @Override
    public void onRefreshBegin(PtrFrameLayout view) {
        Log.d(DEBUG_TAG, "onRefreshBegin: Start update service");
        adapter.cleanSelection();//clean selection before check updates
        canUpdate = false;
        isProgressShow(true);
        if (getActivity() == null) {
            return;//try to prevent some ANR reports
        }

        if (updateAuthor) {
            updateAuthor(author);
            //UpdateServiceIntent.makeUpdateAuthor(getActivity(), author.getId());

        } else {
            updateTag();
            //UpdateServiceIntent.makeUpdate(getActivity(), selectedTag);

        }
    }

    /**
     * Stop GUI update state
     * Call - from on pause
     * call - from Main activity as a result for broadcast
     */
    private void onRefreshComplete() {
        Log.d(DEBUG_TAG, "onRefreshComplete: Stop updating state");
        mPtrFrame.refreshComplete();
        canUpdate = true;
        updateAuthor = false;
        isProgressShow(false);
        checkEmptyViewState();
    }

    /**
     * Change view for refresh menu item
     * Progress and static icon switcher
     *
     * @param isShow if true we show progress
     */
    private void isProgressShow(boolean isShow) {
        if (mMenu == null) {
            return;
        }
        MenuItem item = mMenu.findItem(R.id.menu_refresh);
        if (item != null) {
            if (isShow) {
                item.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                item.setActionView(null);
            }
        }
    }


    @Override
    public boolean singleClick(MotionEvent e) {
        authorRV.playSoundEffect(SoundEffectConstants.CLICK);
        int position = authorRV.getChildAdapterPosition(authorRV.findChildViewUnder(e.getX(), e.getY()));

        Log.d(DEBUG_TAG, "singleClick: Selected position: " + position);
        adapter.toggleSelection(position);
        authorRV.refreshDrawableState();
        final Author author1 = adapter.getSelected();

        if (author1 == null) {
            Log.e(DEBUG_TAG, "singleClick: position: " + position + "  Author is NULL");
            return false;
        }


        mCallbacks.onAuthorSelected(author1.getId());
        return true;
        //return false;

    }

    @Override
    public boolean swipeRight(MotionEvent e) {
        int position = authorRV.getChildAdapterPosition(authorRV.findChildViewUnder(e.getX(), e.getY()));
        adapter.toggleSelection(position, false);

        author = adapter.getSelected();

        if (author == null) {
            return false;
        }

        adapter.makeSelectedRead();
        return true;

    }

    @Override
    public boolean swipeLeft(MotionEvent e) {
        int position = authorRV.getChildAdapterPosition(authorRV.findChildViewUnder(e.getX(), e.getY()));
        adapter.toggleSelection(position);

        author = adapter.getSelected();
        adapter.cleanSelection();

        if (author == null) {
            return false;
        }

        launchBrowser(author);

        return true;
    }

    private final int read_option_item = 21;

    private final int browser_option_item = 23;
    private final int edit_author_option_item = 24;
    private final int delete_option_item = 25;
    private final int update_option_item = 35;

    @Override
    public void longPress(MotionEvent e) {
        final int position = authorRV.getChildAdapterPosition(authorRV.findChildViewUnder(e.getX(), e.getY()));
        adapter.toggleSelection(position);

        author = adapter.getSelected();

        if (author == null) {
            return;
        }
        final MyMenuData menu = new MyMenuData();

        if (author.isIsNew()) {
            menu.add(read_option_item, getString(R.string.menu_read));
        }

        menu.add(browser_option_item, getString(R.string.menu_open_web));
        menu.add(edit_author_option_item, getString(R.string.menu_edit));
        menu.add(delete_option_item, getString(R.string.menu_delete));
        menu.add(update_option_item, getString(R.string.menu_refresh));

        contextMenu = ContextMenuDialog.getInstance(menu, (parent, view1, position1, id) -> {
            int item = menu.getIdByPosition(position1);
            contextSelector(item, position);
            contextMenu.dismiss();
        }, author.getName());

        contextMenu.show(getActivity().getSupportFragmentManager(), "authorContext");

    }

    private void contextSelector(int item, int position) {


        if (item == delete_option_item) {
            Dialog alert = createDeleteAuthorAlert(author.getName());
            alert.show();
        }

        if (item == read_option_item) {
            adapter.makeSelectedRead();
        }

        if (item == browser_option_item) {
            launchBrowser(author);
        }
        if (item == update_option_item) {
            updateAuthor = true;
            startRefresh();
        }
        if (item == edit_author_option_item) {
            EnterStringDialog dDialog = new EnterStringDialog(getActivity(), txt -> {
                author.setName(txt);
                getAuthorController().update(author);
                adapter.notifyChange(author, position);
            }, getText(R.string.dialog_title_edit_author).toString(), author.getName());

            dDialog.show();
        }

    }


    private void startRefresh() {
        if (canUpdate) {
            mPtrFrame.performRefresh();
        }

    }

    /**
     * Launch Browser to load Author home page
     *
     * @param a Author object
     */
    private void launchBrowser(Author a) {
        Uri uri = Uri.parse(a.getUrlForBrowser(getSettingsHelper()));
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uri);
        getActivity().startActivity(launchBrowser);

    }

    /**
     * Create Alert Dialog to wrn about Author delete
     *
     * @param authorName Name of the author
     * @return Warning Author delete dialog
     */
    private Dialog createDeleteAuthorAlert(String authorName) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
        adb.setTitle(R.string.Attention);

        String msg = getString(R.string.alert_delete_author);
        msg = msg.replaceAll("__", authorName);

        adb.setMessage(msg);
        adb.setIcon(android.R.drawable.ic_dialog_alert);
        adb.setPositiveButton(R.string.Yes, deleteAuthorListener);
        adb.setNegativeButton(R.string.No, deleteAuthorListener);
        return adb.create();

    }

    private final DialogInterface.OnClickListener deleteAuthorListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    if (author != null) {
                        getSamlibOperation().makeAuthorDel(author, getGuiState());
                        mCallbacks.cleanBookSelection();
                    }
                    break;
                case Dialog.BUTTON_NEGATIVE:
                    break;
            }

        }
    };

    void searchOrAdd() {


        empty.setVisibility(View.VISIBLE);

        String txt = null;
        try {
            txt = getClipboardText(getActivity());
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, "Clipboard Error!", ex);
        }

        if (txt != null) {

            if (SamLibConfig.getParsedUrl(txt) != null) {
                EditText editText = (EditText) getActivity().findViewById(R.id.addUrlText);
                editText.setText(txt);
            }
        }
    }


    /**
     * Get selection string for author search
     *
     * @return Selected ag
     */
    int getSelection() {
        return selectedTag;
    }


    /**
     * Restore Author selection
     *
     * @param id id of Author to make selected
     */
    private void selectAuthor(long id) {
        Log.d(DEBUG_TAG, "selectAuthor: id = " + id);

        int pos = adapter.findAndSelect(id);
        if (pos < 0) {
            Log.e(DEBUG_TAG, "selectAuthor: id not found - " + id);
            return;
        }
        authorRV.smoothScrollToPosition(pos);
    }

    void cleanSelection() {
        adapter.cleanSelection();
    }

    /**
     * update sort order and selection parameters and restart loader
     *
     * @param selectedTag selection tag id
     * @param so          sort order string
     */
    void selectTag(int selectedTag, AuthorSortOrder so) {
        Log.d(DEBUG_TAG, "selectTag: set Selection: " + selectedTag);
        cleanSelection();
        this.selectedTag = selectedTag;
        if (so != null) {
            order = so;
        }


        updateAdapter();
    }


    Subscriber<GuiUpdateObject> getSubscriber(){
        return
        new Subscriber<GuiUpdateObject>() {

            @Override
            public void onCompleted() {
                Log.i(DEBUG_TAG, "onCompleted");
                onRefreshComplete();
            }

            @Override
            public void onError(Throwable e) {
                Log.e(DEBUG_TAG, "onError", e);
            }

            @Override
            public void onNext(GuiUpdateObject guiUpdateObject) {
                if (guiUpdateObject.isAuthor()) {
                    adapter.cleanSelection();
                    Author author = (Author) guiUpdateObject.getObject();
                    Log.d(DEBUG_TAG,"onNext: get Author: "+author.getName()+" new: "+author.isIsNew());
                    int sort = guiUpdateObject.getSortOrder();
                    if (sort == -1) {
                        getSamlibOperation().makeAuthorReload(author,getGuiState());
                        return;
                    }
                    GuiUpdateObject.UpdateType updateType = guiUpdateObject.getUpdateType();
                    switch (updateType) {
                        case DELETE:
                            adapter.remove(sort);
                            break;
                        case ADD:
                            adapter.add(author, sort);
                            authorRV.scrollToPosition(sort);
                            break;
                        default:
                            LinearLayoutManager llm = (LinearLayoutManager) authorRV.getLayoutManager();
                            int firstPosition = llm.findFirstVisibleItemPosition();
                            adapter.notifyChange(author, sort);
                            authorRV.scrollToPosition(firstPosition);
                    }


                    //adapter.toggleSelection(sort);
                    //authorRV.scrollToPosition(sort);
                    //Log.d(DEBUG_TAG, "updateAdapter: scroll to position: " + sort);
                }
                if (guiUpdateObject.isResult()) {
                    mMessageConstructor.showMessage(guiUpdateObject);
                    onRefreshComplete();
                }
            }
        };
    }

    @Override
    public void refresh() {
        Log.d(DEBUG_TAG, "refresh: call ");
        updateAdapter();
    }


    /**
     * set sort order and restart loader to make is  actual
     *
     * @param so new sort order
     */
    void setSortOrder(AuthorSortOrder so) {
        cleanSelection();
        order = so;
        updateAdapter();
    }

    AuthorSortOrder getSortOrder() {
        return order;
    }

    @Override
    public void onDestroy() {
        Log.d(DEBUG_TAG, "onDestroy call");
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
        super.onDestroy();
        getLoaderManager().destroyLoader(AUTHOR_LOADER_ID);

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(DEBUG_TAG, "onResume call");
        canUpdate = true;
        if (mBound) {
            isProgressShow(mUpdateService.isRunning());
            if (!mUpdateService.isRunning()) {
                mPtrFrame.refreshComplete();
            }
        }
    }


    private void updateTag() {

        UpdateLocalService.makeUpdate(getActivity(), null, getGuiState());
    }


    private void updateAuthor(Author author) {

        UpdateLocalService.makeUpdate(getContext(), author, getGuiState());
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.options_menu, menu);
        mMenu = menu;
        if (mBound) {
            isProgressShow(mUpdateService.isRunning());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int sel = item.getItemId();
        if (sel == android.R.id.home) {
            mCallbacks.drawerToggle();

        }
        if (sel == R.id.menu_refresh) {
            startRefresh();

        }

        if (sel == R.id.selected_option_item) {
            Log.d(DEBUG_TAG, "go to Selected");
            //cleanSelection();
            mCallbacks.onAuthorSelected(SamLibConfig.SELECTED_BOOK_ID);
        }
        return super.onOptionsItemSelected(item);
    }
}
