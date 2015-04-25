/*
 * Copyright (c) 2015 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuitlabs.android.moneywatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gcm.GCMRegistrar;
import com.intuit.intuitwear.exceptions.IntuitWearException;
import com.intuit.intuitwear.notifications.ContentBuilder;
import com.intuit.intuitwear.notifications.IWearNotificationSender;
import com.intuit.intuitwear.notifications.IWearNotificationType;
import com.intuit.mobile.png.sdk.PushNotificationsV2;
import com.intuit.mobile.png.sdk.UserTypeEnum;
import com.intuit.mobile.png.sdk.callback.RegisterUserCallback;
import com.intuit.mobile.png.sdk.callback.RemoveUserFromGroupCallback;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>SettingsFragment</code> shows a multi-select list of information sources as well as
 * two time time pickers to define a <i>quite time</i>, during which notifications are blocked.
 * Additionally, two buttons are available:
 * <ul>
 * <li>To synchronize info source selection with the PNG server, in case a previous selection could not be saved. </li>
 * <li>To simulate an incoming notification</li>
 * </ul>
 *
 * @inheritDoc
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOG_TAG = SettingsFragment.class.getName();

    /**
     * The selection of information sources, translates directly into PNG Groups.
     * Here we register a user with a PNG group for every selected information-source and unregister the user from
     * every png group that is mapped to unselected information source.
     */
    public static void syncGroups() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        final Set<String> defaultFeeds = new HashSet<>(Arrays.asList(App.getContext().getResources().getStringArray(R.array.feed_defaults)));
        final Set<String> ss = prefs.getStringSet(App.getContext().getString(R.string.preference_key_sources), defaultFeeds);
        final String userId = prefs.getString(App.getContext().getString(R.string.preference_key_userid), "");

        final String[] s0 = App.getContext().getResources().getStringArray(R.array.feed_values);

        for (final String s : s0) { // all group values
            boolean subscribe = false;
            for (final String t : ss) { // selected or default group values
                if (s.equals(t)) {
                    subscribe = true;
                    break;
                }
            }
            if (!subscribe) {
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        PushNotificationsV2.removeUserFromGroup(App.getContext(), userId, s, new RemoveUserFromGroupCallback() {
                            @Override
                            public void onUserRemovedFromGroup() {
                                setInSyncFlag(true);
                                Log.i(LOG_TAG, "syncGroupNames onUserRemovedFromGroup " + s);
                            }

                            @Override
                            public void onError(final String s, final String s2) {
                                setInSyncFlag(false);
                                Log.e(LOG_TAG, "syncGroupNames removeUserFromGroup " + s + s2);
                            }
                        });
                    }
                }.start();
            }
        }
        // Subscribe to selected groups:
        new Thread() {
            @Override
            public void run() {
                super.run();
                PushNotificationsV2.registerUser(App.getContext(), userId, UserTypeEnum.OTHER, ss.toArray(new String[ss.size()]),
                        GCMRegistrar.getRegistrationId(App.getContext()),
                        new RegisterUserCallback() {
                            @Override
                            public void onUserRegistered() {
                                setInSyncFlag(true);
                                Log.i(LOG_TAG, "syncGroupNames onUserRegistered for new groups " + ss.size());
                            }

                            @Override
                            public void onError(final String s, final String s2) {
                                setInSyncFlag(false);
                                Log.e(LOG_TAG, "syncGroupNames registerUser " + s + s2);
                            }
                        });
            }
        }.start();
    }

    /**
     * To synchronize info source selection with the PNG server, in case a previous selection could not be saved.
     *
     * @param context {@link android.content.Context}
     */
    static void syncIfNeeded(final Context context) {
        if (!PreferenceManager.getDefaultSharedPreferences(App.getContext()).getBoolean(context.getString(R.string.preference_key_sync), false)) {
            Log.i(LOG_TAG, "New sync. attempt w/ PNG Server");
            syncGroups();
        }
    }

    /**
     * Sets a flag an keeps it in DefaultSharedPreferences, indicating if the last sync attempt was successful or not.
     *
     * @param inSync {@link boolean}
     */
    private static void setInSyncFlag(final boolean inSync) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(App.getContext().getString(R.string.preference_key_sync), inSync);
        editor.apply();
        if (!inSync) {
            Log.i(LOG_TAG, "setInSyncFlag has been set to false");
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // Implement an OnPreferenceClickListener for the sync button, to resend info to push notification gateway
        final Preference button = findPreference(getString(R.string.preference_key_sync));
        if (button != null) {
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    syncGroups();
                    getActivity().getFragmentManager().popBackStack();
                    return true;
                }
            });
        }

        // Implement an OnPreferenceClickListener for the demo button
        final Preference btnDemo = findPreference(getString(R.string.preference_key_demo));
        if (btnDemo != null) {
            btnDemo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    createDemoNotification(App.getContext());
                    getActivity().getFragmentManager().popBackStack();
                    return true;
                }
            });
        }
    }

    /**
     * A {@link PreferenceFragment} is usually drawn with a transparent background,
     * so here we set a background color, making it more readable.
     */
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view != null) {
            view.setBackgroundColor(getResources().getColor(android.R.color.white));
        }
        return view;
    }

    /**
     * Register a this instance as a {@link SharedPreferences.OnSharedPreferenceChangeListener}
     *
     * @see #onPause for unregistering
     */
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        // disable sync if unnecessary
        boolean inSync = getPreferenceScreen().getSharedPreferences().getBoolean(getString(R.string.preference_key_sync), false);
        findPreference(getString(R.string.preference_key_sync)).setEnabled(!inSync);
    }

    /**
     * Unregister a this instance as a {@link SharedPreferences.OnSharedPreferenceChangeListener}
     *
     * @see #onResume for registering
     */
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    //
    // Implement SharedPreferences.OnSharedPreferenceChangeListener
    //

    /**
     * Take action to sync settings with the Push Notification Gateway server,
     * if the information source selection has changed.
     *
     * @param sharedPreferences {@link SharedPreferences}
     * @param key               {@link String}
     */
    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
        if (isAdded()) {
            if (key.equals(getString(R.string.preference_key_sources))) {
                Log.d(LOG_TAG, "Source selection changed");
                syncGroups();
            } else if (key.equals(getString(R.string.preference_key_sync))) {
                boolean inSync = getPreferenceScreen().getSharedPreferences().getBoolean(getString(R.string.preference_key_sync), false);
                findPreference(getString(R.string.preference_key_sync)).setEnabled(!inSync);
            } else if (key.equals(getString(R.string.preference_key_size))) {
                Archive.getInstance().addItem(null); // shrinks the archive array if needed.
            }
        }
    }

    /**
     * Create a demo notification
     */
    private void createDemoNotification(final Context context) {
        final String message = ContentBuilder.getAsset(context, "notification.json");
        Log.v(LOG_TAG, "Received a notification: " + message);

        if (!TimePreference.isNowQuietTime()) {
            Log.v(LOG_TAG, "Not inside quiet time, so let's send a new notification");
            try {
                IWearNotificationSender.Factory.getsInstance().createNotificationSender(IWearNotificationType.ANDROID, context, message).sendNotification(context);
            } catch (IntuitWearException e) {
                Log.e(LOG_TAG, e.toString());
            }
        }
        Archive.getInstance().addItem(message);
    }
}
