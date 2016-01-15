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
package monakhv.android.samlib.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


import in.srain.cube.views.ptr.util.PrefsUtil;
import monakhv.android.samlib.data.DataExportImport;
import monakhv.android.samlib.data.SettingsHelper;
import monakhv.android.samlib.sortorder.AuthorSortOrder;
import monakhv.samlib.data.AbstractSettings;
import monakhv.samlib.db.AuthorController;

import monakhv.samlib.db.DaoBuilder;
import monakhv.samlib.db.entity.SamLibConfig;


import monakhv.samlib.db.entity.Author;
import monakhv.samlib.db.entity.Book;
import monakhv.samlib.log.Log;
import monakhv.samlib.service.GuiUpdate;
import monakhv.samlib.service.SamlibService;

/**
 * Service to making check for author updates Can be called from activity or
 * from alarm manager
 *
 * @author monakhv
 */
public class UpdateServiceIntent extends MyServiceIntent {

    public static final String PREF_NAME="monakhv.android.samlib.service.UpdateServiceIntent";
    public static final String PREF_KEY_LAST_UPDATE=PREF_NAME+".LAST_UPDATE";
    public static final String PREF_KEY_CALLER=PREF_NAME+".CALLER";


    private static final String SELECT_INDEX = "SELECT_INDEX";
    private static final String SELECT_ID = "SELECT_ID";

    private static final String DEBUG_TAG = "UpdateServiceIntent";
    private Context context;
    private SettingsHelper settings;
    private DataExportImport dataExportImport;
    private final List<Author> updatedAuthors;
    private SharedPreferences mSharedPreferences;

    public UpdateServiceIntent() {
        super("UpdateServiceIntent");
        updatedAuthors = new ArrayList<>();
        // Log.d(DEBUG_TAG, "Constructor Call");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        context = this.getApplicationContext();
        updatedAuthors.clear();
        settings = new SettingsHelper(context);
        Log.d(DEBUG_TAG, "Got intent");
        dataExportImport = new DataExportImport(context);
        int currentCaller = intent.getIntExtra(AndroidGuiUpdater.CALLER_TYPE, 0);
        int idx = intent.getIntExtra(SELECT_INDEX, SamLibConfig.TAG_AUTHOR_ALL);
        mSharedPreferences= PrefsUtil.getSharedPreferences(context, PREF_NAME);

        settings.requestFirstBackup();
        if (currentCaller == 0) {
            Log.e(DEBUG_TAG, "Wrong Caller type");

            return;
        }
        mSharedPreferences.edit().putInt(PREF_KEY_CALLER,currentCaller).commit();
        AuthorController ctl = new AuthorController(getHelper());
        List<Author> authors;
        GuiUpdate guiUpdate = new AndroidGuiUpdater(context, currentCaller);

        if (currentCaller == AndroidGuiUpdater.CALLER_IS_RECEIVER) {
            String stag = settings.getUpdateTag();
            idx = Integer.parseInt(stag);
            if (!settings.haveInternetWIFI()) {
                Log.d(DEBUG_TAG, "Ignore update task - we have no internet connection");

                return;
            }
        }
        if (currentCaller == AndroidGuiUpdater.CALLER_IS_ACTIVITY) {
            if (!SettingsHelper.haveInternet(context)) {
                Log.e(DEBUG_TAG, "Ignore update - we have no internet connection");

                guiUpdate.finishUpdate(false, updatedAuthors);
                return;
            }

        }
        if (idx == SamLibConfig.TAG_AUTHOR_ID) {//Check update for the only Author

            int id = intent.getIntExtra(SELECT_ID, 0);//author_id
            Author author = ctl.getById(id);
            if (author != null) {
                authors = new ArrayList<>();
                authors.add(author);
                Log.i(DEBUG_TAG, "Check single Author: " + author.getName());
            } else {
                Log.e(DEBUG_TAG, "Can not fing Author: " + id);
                return;
            }
        } else {
            authors = ctl.getAll(idx, AuthorSortOrder.DateUpdate.getOrder());

            Log.i(DEBUG_TAG, "selection index: " + idx);
        }


        SamlibService service = new SpecialSamlibService(getHelper(), guiUpdate, settings);


        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, DEBUG_TAG);
        wl.acquire();

        service.runUpdate(authors);

        if (settings.getLimitBookLifeTimeFlag()) {
            CleanBookServiceIntent.start(context);
        }
        mSharedPreferences.edit().putLong(PREF_KEY_LAST_UPDATE, Calendar.getInstance().getTime().getTime()).commit();

        wl.release();
    }


    /**
     * Start service - use for receiver Calls
     *
     * @param ctx - Context
     */
    public static void makeUpdate(Context ctx) {
        Intent updater = new Intent(ctx, UpdateServiceIntent.class);
        updater.putExtra(AndroidGuiUpdater.CALLER_TYPE, AndroidGuiUpdater.CALLER_IS_RECEIVER);
        ctx.startService(updater);
    }



    /**
     * Special Service with loadBook method
     */
    public class SpecialSamlibService extends SamlibService {

        public SpecialSamlibService(DaoBuilder sql, GuiUpdate guiUpdate, AbstractSettings settingsHelper) {
            super(sql, guiUpdate, settingsHelper);
        }

        @Override
        public void loadBook(Author a) {

            for (Book book : authorController.getBookController().getBooksByAuthor(a)) {//book cycle for the author to update
                if (book.isIsNew() && settings.testAutoLoadLimit(book) && dataExportImport.needUpdateFile(book)) {
                    Log.i(DEBUG_TAG, "Auto Load book: " + book.getId());
                    DownloadBookServiceIntent.start(context, book.getId(), AndroidGuiUpdater.CALLER_IS_RECEIVER);//we do not need GUI update
                }
            }


        }
    }
}
