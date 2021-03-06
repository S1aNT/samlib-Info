package monakhv.android.samlib;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import monakhv.android.samlib.data.DataExportImport;
import monakhv.android.samlib.data.SettingsHelper;
import monakhv.android.samlib.sql.DatabaseHelper;
import monakhv.samlib.db.AuthorController;
import monakhv.samlib.service.GuiEventBus;
import monakhv.samlib.service.SamlibOperation;
import monakhv.samlib.service.AuthorSearchService;
import monakhv.samlib.service.BookDownloadService;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;


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
 * 12/11/14.
 */
public class MyBaseAbstractActivity extends AppCompatActivity implements MyBaseAbstractFragment.DaggerCaller {
    private volatile DatabaseHelper helper;
    private SamlibApplication mSamlibApplication;
    private SamlibOperation mSamlibOperation;
    private CompositeSubscription mCompositeSubscription;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mSamlibApplication=(SamlibApplication) getApplication();
        setTheme(mSamlibApplication.getSettingsHelper().getTheme());
        super.onCreate(savedInstanceState);
        mCompositeSubscription = new CompositeSubscription();

    }

    @Override
    public SettingsHelper getSettingsHelper(){
        return mSamlibApplication.getSettingsHelper();
    }

    @Override
    public DataExportImport getDataExportImport(){
        return mSamlibApplication.getDataExportImport();
    }

    @Override
    public AuthorController getAuthorController() {
        return mSamlibApplication.getDatabaseComponent(getDatabaseHelper()).getAuthorController();
    }

    @Override
    public SamlibOperation getSamlibOperation() {
        if (mSamlibOperation == null){
            mSamlibOperation=getSamlibOperationInternal();
        }
        return mSamlibOperation;
    }

    private SamlibOperation getSamlibOperationInternal() {
        return mSamlibApplication.getDatabaseComponent(getDatabaseHelper()).getSamlibOperation();
    }
    @Override
    public GuiEventBus getBus(){
        return mSamlibApplication.getApplicationComponent().getGuiEventBus();
    }
    @Override
    public AuthorSearchService getSearchService(){
        return mSamlibApplication.getApplicationComponent().getSearchService();
    }
    @Override
    public BookDownloadService getBookDownloadService(){
        return mSamlibApplication.getApplicationComponent().getBookDownloadService();
    }
    @Override
    public void addSubscription(Subscription subscription){
        mCompositeSubscription.add(subscription);
    }
    /**
     * Get a helper for this action.
     */
    private DatabaseHelper getDatabaseHelper() {
        if (helper == null) {
            helper = getHelperInternal(this);
        }
        return helper;
    }

    /**
     * Get a connection source for this action.
     */
//    public ConnectionSource getConnectionSource() {
//        return getDatabaseHelper().getConnectionSource();
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (helper != null) {
            releaseHelper(helper);
        }
        if (mSamlibOperation != null){
            mSamlibOperation=null;
        }
        if (mCompositeSubscription != null){
            mCompositeSubscription.unsubscribe();
        }
        mSamlibApplication.releaseDatabaseComponent();
    }


    /**
     * This is called internally by the class to populate the helper object instance. This should not be called directly
     * by client code unless you know what you are doing. Use {@link #getDatabaseHelper()} to get a helper instance. If you are
     * managing your own helper creation, override this method to supply this activity with a helper instance.
     * <p/>
     * <p>
     * <b> NOTE: </b> If you override this method, you most likely will need to override the
     * <p/>
     * </p>
     */
    protected DatabaseHelper getHelperInternal(Context context) {
        @SuppressWarnings({"unchecked", "deprecation"})
        DatabaseHelper newHelper =  OpenHelperManager.getHelper(context, DatabaseHelper.class);

        return newHelper;
    }

    /**
     * Release the helper instance created in {@link #getHelperInternal(Context)}. You most likely will not need to call
     * this directly since {@link #onDestroy()} does it for you.
     * <p/>
     * <p>
     * <b> NOTE: </b> If you override this method, you most likely will need to override the
     * {@link #getHelperInternal(Context)} method as well.
     * </p>
     */
    protected void releaseHelper(DatabaseHelper helper) {
        OpenHelperManager.releaseHelper();

        this.helper = null;
    }


}
