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

public class Archive {
    private static final Archive archive = new Archive();
    private static final String DELIMITER = "<<<>>>";

    public static Archive getInstance() {
        return archive;
    }

    /**
     * Saves the new item on top of already saved items, but keeps no more then MAX items.
     *
     * @param item {@link String} - original JSON payload
     */
    public void addItem(final String item) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        final int MAX = prefs.getInt(App.getContext().getString(R.string.preference_key_size), 12);
        final String KEY = App.getContext().getString(R.string.preference_key_archive);

        String archive = prefs.getString(KEY, "");
        if (item!=null) {
            archive = item + DELIMITER + archive;
        }
        String[] items = archive.split(DELIMITER);

        if (items.length > MAX) {
            archive = "";
            for (int i = 0; i < MAX; i++) {
                archive += items[i] + DELIMITER;
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY, archive);
        editor.apply();
    }

    public String[] getItems() {
        final String KEY = App.getContext().getString(R.string.preference_key_archive);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        final String archive = prefs.getString(KEY, null);
        return archive == null ? new String[0] : archive.split(DELIMITER);
    }
}
