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
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This preference will, when clicked, open a dialog showing the
 * preference controls to set a time on a 12 hour clock.
 * Inspired by https://gist.github.com/nickaknudson/5024416
 */
@SuppressWarnings("UnusedDeclaration")
public class TimePreference extends DialogPreference {
    private static final String LOG_TAG = NumberPreference.class.getName();
    private static final String DEFAULT_TIME = "00:00";

    private int mHour = 0;
    private int mMinute = 0;
    private TimePicker mPicker = null;

    /**
     * @inheritDoc
     */
    @SuppressWarnings("UnusedDeclaration")
    public TimePreference(final Context context) {
        this(context, null);
    }

    /**
     * @inheritDoc
     */
    public TimePreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * @inheritDoc
     */
    public TimePreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setPositiveButtonText(R.string.dialog_positive);
        setNegativeButtonText(R.string.dialog_negative);
    }

    /**
     * Find out if a quiet time has been defined and if the current time would fall into this period.
     *
     * @return {@link boolean} if now is inside the enabled quiet time
     */
    public static boolean isNowQuietTime() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        boolean result = false;

        if (sp.getBoolean(App.getContext().getString(R.string.preference_key_ts), false)) {

            final String s0 = sp.getString(App.getContext().getString(R.string.preference_key_t0), DEFAULT_TIME);
            final String s1 = sp.getString(App.getContext().getString(R.string.preference_key_t1), DEFAULT_TIME);
            final String sn = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());

            final Date d0 = TimePreference.toDate(s0);
            final Date d1 = TimePreference.toDate(s1);
            final Date dn = TimePreference.toDate(sn);

            boolean b = d0.before(d1);
            if (d0.before(d1)) {
                result = d0.before(dn) && dn.before(d1);
            } else {
                result = d0.before(dn) || dn.before(d1);
            }
            Log.d(LOG_TAG, "isNowQuiteTime = " + result);

        }
        return result;
    }

    /**
     * Extracts the hours from a string formatted time.
     *
     * @param time {@link String}
     * @return {@link int} hours
     */
    public static int getHour(final String time) {
        return Integer.parseInt(time.split(":")[0]);
    }

    /**
     * Extracts the minutes from a string formatted time.
     *
     * @param time {@link String}
     * @return {@link int} minutes
     */
    public static int getMinute(final String time) {
        return Integer.parseInt(time.split(":")[1]);
    }

    /**
     * Converts the given string into a Date.
     *
     * @param s {@link String} that can be using to instantiate a Date
     * @return {@link Date}
     */
    public static Date toDate(final String s) {
        try {
            return new SimpleDateFormat("HH:mm", Locale.US).parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Convert string-formatted time into a 12 hour w/ am,pm formatted time.
     *
     * @param s {@link String}
     * @return {@link String} time formatted like so: <i>hh:mm a</i>
     */
    public static String time24to12(final String s) {
        final Date inDate = toDate(s);
        return inDate != null ? new SimpleDateFormat("hh:mm a", Locale.US).format(inDate) : s;
    }

    /**
     * Convert the provide hour, min into a formatted string.
     *
     * @param hour   {@link int}
     * @param minute {@link int}
     * @return {@link String} time formatted, like so <i>hh:mm</i>
     */
    public static String toTime(int hour, int minute) {
        return String.valueOf(hour) + ":" + String.valueOf(minute);
    }

    /**
     * Sets the dialog's time.
     *
     * @param hour   {@link int}
     * @param minute {@link int}
     */
    public void setTime(final int hour, final int minute) {
        mHour = hour;
        mMinute = minute;
        persistString(toTime(mHour, mMinute));
        notifyDependencyChange(shouldDisableDependents());
        notifyChanged();
    }

    /**
     * Update the summary, shown in the preference screen.
     */
    public void updateSummary() {
        setSummary(time24to12(String.valueOf(mHour) + ":" + String.valueOf(mMinute)));
    }

    /**
     * @inheritDoc
     */
    @Override
    protected View onCreateDialogView() {
        mPicker = new TimePicker(getContext());
        return mPicker;
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void onBindDialogView(@NonNull final View v) {
        super.onBindDialogView(v);
        mPicker.setCurrentHour(mHour);
        mPicker.setCurrentMinute(mMinute);
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            final int currHour = mPicker.getCurrentHour();
            final int currMinute = mPicker.getCurrentMinute();

            if (!callChangeListener(toTime(currHour, currMinute))) {
                return;
            }
            setTime(currHour, currMinute);
            updateSummary();
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getString(index);
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void onSetInitialValue(final boolean restorePersistedValue, final Object defaultValue) {

        final String time = restorePersistedValue ? getPersistedString(DEFAULT_TIME) : defaultValue.toString();
        setTime(getHour(time), getMinute(time));
        updateSummary();
    }
}