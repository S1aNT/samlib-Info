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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import android.preference.*;
import android.support.v4.app.NavUtils;

import android.util.Log;
import android.view.MenuItem;


import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;

import java.util.Arrays;
import java.util.List;


import monakhv.android.samlib.data.SettingsHelper;
import monakhv.samlib.db.TagController;
import monakhv.samlib.db.entity.SamLibConfig;
import monakhv.samlib.db.entity.Tag;


/**
 * @author monakhv
 */
public class SamlibPreferencesFragment extends PreferenceFragment
        implements OnSharedPreferenceChangeListener, Preference.OnPreferenceChangeListener {


    private static final String DEBUG_TAG = "SamlibPreferencesA";
    private static final int REQ_AUTH = 11;

    private final String[] autoSummaryFields = {"pref_key_update_Period", "pref_key_proxy_host",
            "pref_key_proxy_port", "pref_key_proxy_user", "pref_key_update_autoload_limit", "pref_key_book_lifetime",
            "pref_key_author_order", "pref_key_book_order", "pref_key_file_format", "pref_key_theme",
            "pref_key_mirror", "pref_key_directory", "pref_key_update_tag"};
    private List<String> autoSumKeys;
    private RingtonePreference ringtonePref;
    Preference googlePrefs;
    private EditTextPreference storageDir;
    private MyBaseAbstractFragment.DaggerCaller mCallbacks;

    /**
     * Called when the activity is first created.
     *
     * @param icicle Bundle
     */
    @Override
    public void onCreate(Bundle icicle) {

        super.onCreate(icicle);


        mCallbacks.getSettingsHelper().getDataDirectoryPath();//just to make sure that preference is loaded

        getPreferenceManager().setSharedPreferencesName(
                SettingsHelper.PREFS_NAME);
        addPreferencesFromResource(R.xml.prefs);


        autoSumKeys = Arrays.asList(autoSummaryFields);
        ringtonePref = (RingtonePreference) findPreference(getString(R.string.pref_key_notification_ringtone));
        googlePrefs = findPreference(getString(R.string.pref_key_google_account));
        storageDir = (EditTextPreference) findPreference(getString(R.string.pref_key_directory));
//        storageDir.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                return helper.isDirectoryWritable ((String )newValue);
//            }
//        });
        googlePrefs.setSummary(mCallbacks.getSettingsHelper().getGoogleAccount());
        googlePrefs.setOnPreferenceClickListener(preference -> {
            String email = mCallbacks.getSettingsHelper().getGoogleAccount();
            Account curAccount = (email == null) ? null : new Account(email, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
            Intent intent = AccountPicker.newChooseAccountIntent(curAccount, null,
                    new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, null, null, null, null);
            startActivityForResult(intent, REQ_AUTH);
            return true;

        });

        ListPreference updateTagPref = (ListPreference) findPreference(getString(R.string.pref_key_update_tag));

        TagController tagCtl = mCallbacks.getAuthorController().getTagController();
        List<Tag> tags = tagCtl.getAll();

        CharSequence[] entries = new CharSequence[1 + tags.size()];
        CharSequence[] entryValues = new CharSequence[1 + tags.size()];

        entryValues[0] = Integer.toString(SamLibConfig.TAG_AUTHOR_ALL);
        entries[0] = getActivity().getString(R.string.pref_update_all_title);
        int i = 1;
        for (Tag tag : tags) {
            entryValues[i] = Integer.toString(tag.getId());
            entries[i] = tag.getName();
            ++i;
        }


        updateTagPref.setEntries(entries);
        updateTagPref.setEntryValues(entryValues);


        updateTagPref.setValue(mCallbacks.getSettingsHelper().getUpdateTag());

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof MyBaseAbstractFragment.DaggerCaller)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }
        mCallbacks = (MyBaseAbstractFragment.DaggerCaller) activity;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent it) {

        super.onActivityResult(requestCode, resultCode, it);
        if (it == null) {
            return;
        }
        switch (requestCode) {
            case REQ_AUTH:
                mCallbacks.getSettingsHelper().setGoogleAccount(it.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
                googlePrefs.setSummary(mCallbacks.getSettingsHelper().getGoogleAccount());
                break;
        }
    }

    public void makeResume() {
        super.onResume();
        mCallbacks.getSettingsHelper().registerListener();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        Log.d(DEBUG_TAG, "onResume");

        for (String key : autoSumKeys) {
            updateSummary(key);
        }

        // A patch to overcome OnSharedPreferenceChange not being called by RingtonePreference bug 

        ringtonePref.setOnPreferenceChangeListener(this);
        updateRingtoneSummary(ringtonePref, mCallbacks.getSettingsHelper().getNotificationRingToneURI());
        storageDir.setOnPreferenceChangeListener(this);
        applyGoogleProxyDependencies();

    }


    public void makePause() {
        super.onPause();
        mCallbacks.getSettingsHelper().updateService();
        mCallbacks.getSettingsHelper().unRegisterListener();
        //if http instance already exist make sure we have right proxy settings
        final SamlibApplication samlibApplication = (SamlibApplication) getActivity().getApplication();

        samlibApplication.getApplicationComponent().getHttpClientController().setProxyData(mCallbacks.getSettingsHelper().getProxy());
        Log.d(DEBUG_TAG, "onPause");
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

    }

    private void updateSummary(String key) {

        if (key.equals(getString(R.string.pref_key_directory))) {

            EditTextPreference pr = (EditTextPreference) getPreferenceScreen().findPreference(key);
            pr.setSummary(mCallbacks.getSettingsHelper().getDataDirectoryPath());
            return;
        }
        if (autoSumKeys.contains(key)) {
            Preference pr = getPreferenceScreen().findPreference(key);
            if (pr instanceof ListPreference) {
                final ListPreference currentPreference = (ListPreference) pr;
                currentPreference.setSummary(currentPreference.getEntry());
            } else if (pr instanceof EditTextPreference) {
                final EditTextPreference currentPreference = (EditTextPreference) pr;
                currentPreference.setSummary(currentPreference.getText());
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;

        }
        return super.onOptionsItemSelected(item);

    }


    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary(key);
        if (key.equals(getString(R.string.pref_key_theme))) {
            AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
            adb.setTitle(R.string.Attention);

            String msg = getString(R.string.change_theme_alert);

            adb.setMessage(msg);
            adb.setIcon(android.R.drawable.ic_dialog_alert);
            adb.setPositiveButton(R.string.Yes, changeThemeListener);
            adb.setNegativeButton(R.string.No, changeThemeListener);
            adb.create();
            adb.show();
        }
        if (key.equals(getString(R.string.pref_key_use_google_proxy_flag))) {
            applyGoogleProxyDependencies();
        }
    }


    private void applyGoogleProxyDependencies() {
        CheckBoxPreference cbp1 = (CheckBoxPreference) getPreferenceScreen().findPreference(getString(R.string.pref_key_use_google_proxy_flag));
        boolean status = !cbp1.isChecked();
        final String[] keys = {getString(R.string.pref_key_proxy_host), getString(R.string.pref_key_proxy_port), getString(R.string.pref_key_proxy_user), getString(R.string.pref_key_proxy_password)};
        for (String k : keys) {
            EditTextPreference etp = (EditTextPreference) getPreferenceScreen().findPreference(k);
            etp.setEnabled(status);
        }
    }

    private final DialogInterface.OnClickListener changeThemeListener = (dialog, which) -> {
        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                // moveTaskToBack(true);
                Intent intent = new Intent();
                getActivity();
                getActivity().setResult(Activity.RESULT_OK, intent);
                getActivity().finish();
                break;
            case Dialog.BUTTON_NEGATIVE:
                break;
        }

    };

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(DEBUG_TAG, "onPreferenceChange: " + preference.getKey());
        if (preference.getKey().equals(getString(R.string.pref_key_directory))) {
            Log.d(DEBUG_TAG, "DIR - new " + newValue);
            Log.d(DEBUG_TAG, "DIR - old " + mCallbacks.getSettingsHelper().getDataDirectoryPath());
            return mCallbacks.getSettingsHelper().isDirectoryWritable((String) newValue);
        }
        if (preference.getKey().equalsIgnoreCase(getString(R.string.pref_key_notification_ringtone))) {
            Log.d(DEBUG_TAG, "new ringtone: " + newValue);
            updateRingtoneSummary((RingtonePreference) preference, Uri.parse((String) newValue));
        }

        return true;
    }

    private void updateRingtoneSummary(RingtonePreference preference, Uri ringtoneUri) {
        Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), ringtoneUri);
        if (ringtone != null) {
            preference.setSummary(ringtone.getTitle(getActivity()));
        } else {
            preference.setSummary(getString(R.string.pref_no_sound));
        }


    }
}
