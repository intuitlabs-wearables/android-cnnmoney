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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intuit.intuitwear.exceptions.IntuitWearException;
import com.intuit.intuitwear.notifications.IWearNotificationContent;
import com.intuit.intuitwear.notifications.IWearNotificationSender;
import com.intuit.intuitwear.notifications.IWearNotificationType;
import com.intuit.mobile.png.sdk.PushNotificationsV2;
import com.intuit.mobile.png.sdk.UserTypeEnum;
import com.intuit.mobile.png.sdk.callback.RegisterUserCallback;

import java.lang.ref.WeakReference;


/**
 * GCMIntentService extends Google's {@link GCMBaseIntentService}, setting up the communication with
 * the Push Notification Gateway. Here we are adding the registration with Intuit's Notification
 * Server and also implement a few callbacks.
 */
public class GCMIntentService extends GCMBaseIntentService {

    static final String MSG_KEY = GCMIntentService.class.getName() + "_MSG_KEY";
    private static final String GCM_PROJECT_NUMBER = App.getContext().getString(R.string.GCM_PROJECT_NUMBER);
    private static final String INTUIT_SENDER_ID = App.getContext().getString(R.string.INTUIT_SENDER_ID);
    private static final String LOG_TAG = GCMIntentService.class.getSimpleName();
    private static final String REG_URL = "https://png.d2d.msg.intuit.com";

    static MyHandler handler;

    private static String userid;
    private static String[] groups;

    public GCMIntentService() {
        super(GCM_PROJECT_NUMBER);
    }

    /**
     * Register with GCM, which will eventually trigger {@link #onRegistered} to be called.
     *
     * @param context {@link Context} Application context
     * @param userid  {@link String} how your app refers to this user
     */
    public static void register(final Context context, final String userid) {
        GCMIntentService.userid = userid;
        GCMIntentService.groups = context.getResources().getStringArray(R.array.feed_defaults);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.preference_key_userid), userid);
        editor.putBoolean(context.getString(R.string.preference_key_sync), true);
        editor.apply();

        PushNotificationsV2.URL_OVERRIDE = REG_URL;
        PushNotificationsV2.Environment environment = PushNotificationsV2.Environment.SANDBOX;
        PushNotificationsV2.initialize(INTUIT_SENDER_ID, GCM_PROJECT_NUMBER, environment);
        PushNotificationsV2.setLogging(true);
        PushNotificationsV2.registerForGCMNotifications(context);
    }

    /**
     * Handler which can be used to display a message, in case this app is running and in teh foreground.
     *
     * @param a {@link MainActivity} activity, which provides the capability to show a message.
     */
    public static void setHandler(final MainActivity.PlaceholderFragment a) {
        handler = new MyHandler(a);
    }

    /**
     * Google will call this method, providing you a unique registrationId for this device.
     * We recommended to save the registrationId to local preferences for later use.
     * e.g. saveRegistrationId(registrationId);
     *
     * @param context        {@link Context} Application context
     * @param registrationId {@link String} unique registrationId for this device
     */
    @Override
    protected void onRegistered(final Context context, final String registrationId) {
        PushNotificationsV2.registerUser(
                this,
                GCMIntentService.userid,
                UserTypeEnum.OTHER,
                GCMIntentService.groups,
                registrationId,
                new RegisterUserCallback() {

                    @Override
                    public void onUserRegistered() {
                        Log.i(LOG_TAG, "Registration call to PNG servers was accepted");
                    }

                    @Override
                    public void onError(String code, String description) {
                        Log.i(LOG_TAG, String.format("Received error callback from PNG. Error code= %s, description= %s", code, description));
                    }
                });
    }

    /**
     * This callback method is invoked when GCM delivers a notification to the device.
     * <p/>
     * Assuming that the json encoded message is a valid (see IntuitWear JSONSchema) document,
     * we acquire an instance of a {@link IWearNotificationSender.Factory} to create a NotificationSender,
     * which will send the generated notification to the wearable device.
     *
     * @param context {@link Context} Application context
     * @param intent  {@link Intent} received with the push notification
     */
    @Override
    protected void onMessage(final Context context, final Intent intent) {
        Log.v(LOG_TAG, "Received onMessage call");
        if (!TimePreference.isNowQuietTime(context)) {
            final String message = intent.getStringExtra("payload").replaceAll("[\r\n]+$", "");
            Log.v(LOG_TAG, "Not inside quiet time, so let's display a notification :" + message);
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("lastMsg", message).apply();
            try {
                IWearNotificationSender.Factory.getsInstance().createNotificationSender(IWearNotificationType.ANDROID, this, message).sendNotification(this);
            } catch (IntuitWearException e) {
                Log.e(LOG_TAG, e.toString());
            }

            if (handler != null) {
                Message m = new Message();
                Bundle b = new Bundle();
                b.putString(MSG_KEY, message);
                m.setData(b);
                handler.sendMessage(m);
            }
        }
    }

    /**
     * Callback called upon a GCM error.
     *
     * @param context {@link Context} Application context
     * @param msg     {@link String} Error string
     */
    @Override
    protected void onError(final Context context, final String msg) {
        Log.e(LOG_TAG, "Error related to GCM: " + msg);
    }

    /**
     * Callback called when device is unregistered from GCM.
     *
     * @param context {@link Context} Application context
     * @param msg     Unregister message
     */
    @Override
    protected void onUnregistered(final Context context, final String msg) {
        Log.i(LOG_TAG, "Received unregistered call");
    }

    /**
     * Handler that can display incoming messages, if the registered Activity is still around.
     */
    static class MyHandler extends Handler {
        //Using a weak reference means we won't prevent garbage collection
        private final WeakReference<MainActivity.PlaceholderFragment> myClassWeakReference;

        public MyHandler(final MainActivity.PlaceholderFragment myClassInstance) {
            myClassWeakReference = new WeakReference<>(myClassInstance);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void handleMessage(final Message msg) {
            MainActivity.PlaceholderFragment fragment = myClassWeakReference.get();
            if (fragment != null && msg != null) {
                final IWearNotificationContent content;
                try {
                    content = new Gson().fromJson(msg.getData().getString(MSG_KEY, ""), IWearNotificationContent.class);
                    fragment.showMessage(content);
                } catch (JsonSyntaxException e) {
                    Log.e(LOG_TAG, "IWearNotificationContent instantiation failed" + e.toString());
                }
            }
        }
    }
}