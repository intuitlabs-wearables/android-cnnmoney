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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.intuit.intuitwear.notifications.ContentBuilder;

public class Archive {
    private static final Archive archive = new Archive();
    private static final String DELIMITER = "<<<>>>";
    private static final int MAX = 12;

    private Archive() {
        if (null == getItems()) {
            addItem(ContentBuilder.getAsset(App.getContext(), "notification.json"));
        }
    }

    public static Archive getInstance() {
        return archive;
    }

    /**
     * Saves the new item on top of already saved items, but keeps no more then MAX items.
     *
     * @param item {@link String} - original JSON payload
     */
    public void addItem(final String item) {
        final String KEY = App.getContext().getString(R.string.preference_key_archive);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());

        String archive = prefs.getString(KEY, "");
        if (archive.split(DELIMITER).length >= MAX - 1) {
            int k = archive.lastIndexOf(DELIMITER);
            archive = archive.substring(0, k);
        }
        archive = item + DELIMITER + archive;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY, archive);
        editor.apply();
    }

    public String[] getItems() {
        final String KEY = App.getContext().getString(R.string.preference_key_archive);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        final String archive = prefs.getString(KEY, null);
        return archive == null ? null : archive.split(DELIMITER);
    }
}
