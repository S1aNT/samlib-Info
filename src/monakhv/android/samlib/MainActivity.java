/*
 * Copyright 2013 Dmitry Monakhov.
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
 */
package monakhv.android.samlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import monakhv.android.samlib.search.SearchAuthorActivity;
import monakhv.android.samlib.data.SettingsHelper;
import monakhv.android.samlib.search.SearchAuthorsListFragment;
import monakhv.android.samlib.service.CleanNotificationData;
import monakhv.android.samlib.sql.AuthorController;
import monakhv.android.samlib.sql.entity.Author;
import monakhv.android.samlib.sql.entity.Book;
import monakhv.android.samlib.sql.entity.SamLibConfig;
import monakhv.android.samlib.tasks.AddAuthor;

public class MainActivity extends SherlockFragmentActivity implements AuthorListFragment.Callbacks, BookListFragment.Callbacks,
        SlidingPaneLayout.PanelSlideListener {

    private SlidingPaneLayout pane;

    public void onAuthorSelected(int id) {
        books.setAuthorId(id);
        //pane.closePane();
        
    }

    public void onPanelSlide(View view, float f) {

    }

    public void onPanelOpened(View view) {
        Log.d(DEBUG_TAG, "panel is opened");
        getSupportActionBar().setTitle(title);
        isOpen = true;
        invalidateOptionsMenu();
        listHelper.setHasOptionsMenu(true);
        books.setHasOptionsMenu(false);
        getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_HOME_AS_UP);
    }

    public void onPanelClosed(View view) {
        isOpen = false;
        invalidateOptionsMenu();
        listHelper.setHasOptionsMenu(false);
        books.setHasOptionsMenu(true);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        int author_id = books.getAuthorId();
        Log.d(DEBUG_TAG, "panel is closed, author_id = " + author_id);

        if (author_id == 0) {
            return;
        }
        if (author_id != SamLibConfig.SELECTED_BOOK_ID) {
            AuthorController sql = new AuthorController(this);
            Author a = sql.getById(author_id);
            if (a == null) {
                Log.e(DEBUG_TAG, "Can not find author for id: " + author_id);
                return;
            }
            title = getSupportActionBar().getTitle().toString();
            getSupportActionBar().setTitle(a.getName());
        } else {
            title = getSupportActionBar().getTitle().toString();
            getSupportActionBar().setTitle(getText(R.string.menu_selected_go));
        }

    }

    public void cleanAuthorSelection() {
        listHelper.cleanSelection();
        //books.setAuthorId(0);
    }

    private static final String DEBUG_TAG = "MainActivity";
    private static final String STATE_SELECTION  ="STATE_SELECTION";
    private static final String STATE_AUTHOR_POS = "STATE_AUTHOR_ID";
    public static String CLEAN_NOTIFICATION = "CLEAN_NOTIFICATION";
    public static final int ARCHIVE_ACTIVITY = 1;
    public static final int SEARCH_ACTIVITY = 2;
    //AddAuthorDialog addAuthorDilog;
    private UpdateActivityReceiver updateReceiver;
    private DownloadReceiver downloadReceiver;
    private AuthorListFragment listHelper;

    private BookListFragment books = null;
    private boolean isOpen = true;

    private String title;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.main_twopane);

        Bundle bundle = getIntent().getExtras();
        //CleanNotificationData.start(this);
        String clean = null;
        if (bundle != null) {
            clean = bundle.getString(CLEAN_NOTIFICATION);
        }
        if (clean != null) {
            CleanNotificationData.start(this);

        }
        title = getString(R.string.app_name);
        SettingsHelper.addAuthenticator(this.getApplicationContext());
        //getActionBarHelper().setRefreshActionItemState(refreshStatus);
        books = (BookListFragment) getSupportFragmentManager().findFragmentById(R.id.listBooksFragment);
        listHelper = (AuthorListFragment) getSupportFragmentManager().findFragmentById(R.id.listAuthirFragment);
        listHelper.setHasOptionsMenu(true);
        pane = (SlidingPaneLayout) findViewById(R.id.pane);
        pane.setPanelSlideListener(this);
        pane.setParallaxDistance(10);

        ActivityUtils.setShadow(pane);

        Log.d(DEBUG_TAG, "Faiding color: " + pane.getSliderFadeColor());
        isOpen = true;
        
        if (bundle != null){
            onRestoreInstanceState(bundle);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle){
        super.onSaveInstanceState(bundle);
        bundle.putString(STATE_SELECTION, listHelper.getSelection());
        bundle.putInt(STATE_AUTHOR_POS, listHelper.getSelectedAuthorPosition());
        
    }
    @Override
    public void onRestoreInstanceState(Bundle bundle){
        super.onRestoreInstanceState(bundle);
        if (bundle == null){
            return;
        }
        
        
        listHelper.refresh(bundle.getString(STATE_SELECTION), null);
        listHelper.restoreSelection(bundle.getInt(STATE_AUTHOR_POS));
        books.setAuthorId(listHelper.getSelectedAuthorId());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        pane.openPane();
        IntentFilter updateFilter = new IntentFilter(UpdateActivityReceiver.ACTION_RESP);
        IntentFilter downloadFilter = new IntentFilter(DownloadReceiver.ACTION_RESP);

        updateFilter.addCategory(Intent.CATEGORY_DEFAULT);
        downloadFilter.addCategory(Intent.CATEGORY_DEFAULT);

        updateReceiver = new UpdateActivityReceiver();
        downloadReceiver = new DownloadReceiver();
        //getActionBarHelper().setRefreshActionItemState(refreshStatus);
        registerReceiver(updateReceiver, updateFilter);
        registerReceiver(downloadReceiver, downloadFilter);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) { //Back key pressed
            if (!isOpen) {
                pane.openPane();
                return true;
            }
            if (listHelper.getSelection() != null) {
                listHelper.refresh(null, null);
            } else {
                finish();
            }

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateReceiver);
        unregisterReceiver(downloadReceiver);
        //Stop refresh status
        listHelper.onRefreshComplete();
        //getActionBarHelper().setRefreshActionItemState(refreshStatus);
    }

    /**
     * Return from ArchiveActivity or SearchActivity
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Log.d(DEBUG_TAG, "Wrong result code from onActivityResult");
            return;
        }
        if (requestCode == ARCHIVE_ACTIVITY) {

            int res = data.getIntExtra(ArchiveActivity.UPDATE_KEY, -1);
            if (res == ArchiveActivity.UPDATE_LIST) {
                Log.d(DEBUG_TAG, "Reconstruct List View");
                listHelper.refresh(null, null);

            }
        }
        if (requestCode == SEARCH_ACTIVITY) {
            AddAuthor aa = new AddAuthor(getApplicationContext());
            aa.execute(data.getStringExtra(SearchAuthorsListFragment.AUTHOR_URL));
        }
    }

    /**
     * Add new Author to SQL Store
     *
     * @param view
     */
    public void addAuthor(View view) {
        EditText editText = (EditText) findViewById(R.id.addUrlText);
        String text = editText.getText().toString();
        View v = findViewById(R.id.add_author_panel);
        editText.setText("");
        v.setVisibility(View.GONE);
        if (SamLibConfig.reduceUrl(text) != null) {
            AddAuthor aa = new AddAuthor(this.getApplicationContext());
            aa.execute(text);
        } else {
            if (TextUtils.isEmpty(text)) {
                return;
            }
            Intent prefsIntent = new Intent(getApplicationContext(),
                    SearchAuthorActivity.class);
            prefsIntent.putExtra(SearchAuthorActivity.EXTRA_PATTERN, text);
            prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            startActivityForResult(prefsIntent, SEARCH_ACTIVITY);
        }

    }

    public void onOpenPanel() {
        if (!isOpen) {
            pane.openPane();
        }
    }

    public void onClosePanel() {
        if (isOpen) {
            pane.closePane();
        }
    }

    /**
     * Receive updates from Update Service
     */
    public class UpdateActivityReceiver extends BroadcastReceiver {

        public static final String ACTION_RESP = "monakhv.android.samlib.action.UPDATED";
        public static final String TOAST_STRING = "TOAST_STRING";
        public static final String ACTION = "ACTION";
        public static final String ACTION_TOAST = "TOAST";
        public static final String ACTION_PROGRESS = "PROGRESS";

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getStringExtra(ACTION);
            if (action.equalsIgnoreCase(ACTION_TOAST)) {
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, intent.getCharSequenceExtra(TOAST_STRING), duration);
                toast.show();

                listHelper.onRefreshComplete();
            }//
            if (action.equalsIgnoreCase(ACTION_PROGRESS)) {
                listHelper.updateProgress(intent.getStringExtra(TOAST_STRING));
            }

        }
    }

    public class DownloadReceiver extends BroadcastReceiver {

        public static final String ACTION_RESP = "monakhv.android.samlib.action.BookDownload";
        public static final String MESG = "MESG";
        public static final String RESULT = "RESULT";
        public static final String BOOK_ID = "BOOK_ID";
        private static final String DEBUG_TAG = "DownloadReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(DEBUG_TAG, "Starting onReceive");
            String mesg = intent.getStringExtra(MESG);
            long book_id = intent.getLongExtra(BOOK_ID, 0);

            boolean res = intent.getBooleanExtra(RESULT, false);

            AuthorController sql = new AuthorController(context);
            Book book = sql.getBookController().getById(book_id);

            if (books != null) {
                if (books.progress != null) {
                    books.progress.dismiss();
                }
            }

            if (res) {
                Log.d(DEBUG_TAG, "Starting web for url: " + book.getFileURL());
//               
                if (books != null) {
                    books.launchReader(book);
                }
            } else {
                Toast toast = Toast.makeText(context, mesg, Toast.LENGTH_SHORT);

                toast.show();
            }
        }
    }
}
