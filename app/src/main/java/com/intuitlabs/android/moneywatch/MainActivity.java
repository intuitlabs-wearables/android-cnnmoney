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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.intuit.intuitwear.notifications.IWearNotificationContent;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The Main activity of this app is switching between the Settings and Placeholder fragment.
 */
public class MainActivity extends Activity {
    /**
     * Tag to identify a fragment
     */
    private static final String FRAG_TAG_SETTINGS = "tag";

    /**
     * Returns a unique id to address the user from the remote side.
     *
     * @param context {@link Context}
     * @return {@link String} unique id, most likely, the user's email address.
     */
    private static String getId(final Context context) {
        final Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        final Account[] accounts = AccountManager.get(context).getAccounts();
        String id = UUID.randomUUID().toString();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                id = account.name;
                break;
            }
        }
        return id;
    }

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
            GCMIntentService.register(getApplicationContext(), MainActivity.getId(this));
            if (findViewById(R.id.container) != null) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.container, new PlaceholderFragment())
                        .commit();
            }
            SettingsFragment.syncIfNeeded(getApplicationContext());
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private TextView mTitle;
        private TextView mText;
        private ImageButton mOpen;


        @Override
        public View onCreateView(final LayoutInflater inflater,
                                 final ViewGroup container,
                                 final Bundle savedInstanceState) {

            // Inflate the layout for this fragment
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            mTitle = (TextView) rootView.findViewById(R.id.title);
            mText = (TextView) rootView.findViewById(R.id.text);
            mOpen = (ImageButton) rootView.findViewById(R.id.btnOpen);
            // Tell the GCMIntentService about this Fragment, so that an incoming message
            // can immediately displayed, using the showMessage method.
            GCMIntentService.setHandler(this);
            return rootView;
        }


        /**
         * Show the title and text content of the notification.
         *
         * @param content {@link IWearNotificationContent} to be placed into the main layout's text view
         */
        public void showMessage(final IWearNotificationContent content) {

            mTitle.setText(content.getBigTextStyle().getBigContentTitle());
            mText.setText(content.getBigTextStyle().getBigText());
            mOpen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final Uri uri = Uri.parse(content.getActions().get(0).getIntentName());
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
                }
            });
        }
    }
}
