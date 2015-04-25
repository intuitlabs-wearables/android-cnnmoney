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
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

/**
 * <code>NumberPreference</code> is an Integer Preference, allowing the user to set the maximum number
 * of headlines and links to retain.
 */
public class NumberPreference extends DialogPreference {
    private static final int DEFAULT_SIZE = 12;
    private static final int MIN_SIZE = 0;
    private static final int MAX_SIZE = 100;

    private NumberPicker mPicker = null;
    private int mSize;

    /**
     * @inheritDoc
     */
    @SuppressWarnings("UnusedDeclaration")
    public NumberPreference(final Context context) {
        this(context, null);
    }

    /**
     * @inheritDoc
     */
    public NumberPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * @inheritDoc
     */
    public NumberPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setPositiveButtonText(R.string.dialog_positive);
        setNegativeButtonText(R.string.dialog_negative);
    }


    /**
     * Sets the dialog's value.
     *
     * @param k {@link int}
     */
    public void setSize(final int k) {
        mSize = k;
        persistInt(mSize);
        notifyDependencyChange(shouldDisableDependents());
        notifyChanged();
    }

    /**
     * Update the summary, shown in the preference screen.
     */
    public void updateSummary(final int k) {
        setSummary("Currently retaining up to " + k + " entries");
    }

    /**
     * @inheritDoc
     */
    @Override
    protected View onCreateDialogView() {
        mPicker = new NumberPicker(getContext());
        mPicker.setMinValue(MIN_SIZE);
        mPicker.setMaxValue(MAX_SIZE);
        mPicker.setValue(DEFAULT_SIZE);
        return mPicker;
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void onBindDialogView(@NonNull final View v) {
        super.onBindDialogView(v);
        mPicker.setValue(mSize);
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            final int k = mPicker.getValue();
            if (!callChangeListener(k)) {
                return;
            }
            setSize(k);
            updateSummary(k);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInteger(index, DEFAULT_SIZE);
    }

    /**
     * @inheritDoc
     */
    @Override
    protected void onSetInitialValue(final boolean restorePersistedValue, final Object defaultValue) {
        final int size = restorePersistedValue ? getPersistedInt(DEFAULT_SIZE) : (Integer) defaultValue;
        setSize(size);
        updateSummary(size);
    }
}