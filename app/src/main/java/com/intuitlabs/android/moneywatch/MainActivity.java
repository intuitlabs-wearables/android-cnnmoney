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

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.intuit.intuitwear.notifications.IWearNotificationContent;

/**
 * The Main activity of this app is switching between the Settings and Placeholder fragment.
 */
public class MainActivity extends Activity {
    private static final String LOG_TAG = MainActivity.class.getName();
    /**
     * Tag to identify a fragment
     */
    private static final String FRAG_TAG_SETTINGS = "tag";

    /**
     * Setting up the main ui, i.e. loading an image into the PlaceholderFragment.
     * Registering the app the PushNotificationGateway, using the {@link GCMIntentService#register}
     *
     * @param savedInstanceState {@link Bundle}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getActionBar() != null) {
            getActionBar().setDisplayShowHomeEnabled(true);
        }
        if (savedInstanceState == null) {
            GCMIntentService.register(App.getId());
            if (findViewById(R.id.container) != null) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, new NotificationArchiveFragment())
                        .commit();
            }
            // todo refactor this to a better place
            SettingsFragment.syncIfNeeded(App.getContext());
        }
    }


    /**
     * @inheritDoc
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Besides the Home button, the only thing added into the actionbar is the settings icon.
     * Here we implement to either show(add) or hide(pop) the {@link SettingsFragment} depending on
     * its current visibility status.
     *
     * @param item {@link MenuItem}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Fragment frag = getFragmentManager().findFragmentByTag(FRAG_TAG_SETTINGS);
            if (frag == null || !frag.isVisible()) {
                //
                // show the SettingsFragment
                //
                getFragmentManager()
                        .beginTransaction()
                        .add(R.id.container, new SettingsFragment(), FRAG_TAG_SETTINGS)
                        .addToBackStack(null)
                        .commit();
            } else {
                //
                // Remove the SettingsFragment,
                // if is is visible and the settings action icon gets clicked.
                //
                getFragmentManager().popBackStack();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A fragment containing a simple list view of archived notifications.
     */
    public static class NotificationArchiveFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private ListView mListView;

        /**
         * @inheritDoc
         */
        @Override
        public View onCreateView(final LayoutInflater inflater,
                                 final ViewGroup container,
                                 final Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            mListView = (ListView) rootView.findViewById(R.id.listView);
            return rootView;
        }

        /**
         * @inheritDoc
         */
        @Override
        public void onResume() {
            super.onResume();
            mListView.setAdapter(new LstAdapter<>(App.getContext(), R.layout.list_item, Archive.getInstance().getItems()));
            PreferenceManager.getDefaultSharedPreferences(App.getContext()).registerOnSharedPreferenceChangeListener(this);
        }

        /**
         * @inheritDoc
         */
        @Override
        public void onPause() {
            PreferenceManager.getDefaultSharedPreferences(App.getContext()).unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
            if (key.equals(getString(R.string.preference_key_archive))) {
                mListView.setAdapter(new LstAdapter<>(App.getContext(), R.layout.list_item, Archive.getInstance().getItems()));
            }
        }

        class LstAdapter<T> extends ArrayAdapter<T> {
            /**
             * @inheritDoc
             */
            LstAdapter(Context context, int resource, T[] objects) {
                super(context, resource, objects);
            }

            /**
             * @inheritDoc
             */
            @Override
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                final LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.list_item, parent, false);
                final TextView titleView = (TextView) rowView.findViewById(R.id.title);
                final TextView textView = (TextView) rowView.findViewById(R.id.text);
                final String json = String.valueOf(getItem(position));
                final IWearNotificationContent content = new Gson().fromJson(json, IWearNotificationContent.class);
                if (content != null) {
                    titleView.setText(content.getBigTextStyle().getBigContentTitle());
                    textView.setText(content.getBigTextStyle().getBigText());
                    rowView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            try {
                                final Uri uri = Uri.parse(content.getActions().get(0).getExtras()[0]);
                                startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
                            } catch (Exception e) {
                                Log.i(LOG_TAG, e.toString());
                            }
                        }
                    });
                }
                return rowView;
            }
        }
    }
}
