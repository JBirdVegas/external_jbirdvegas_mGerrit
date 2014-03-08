package com.jbirdvegas.mgerrit.listeners;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.jbirdvegas.mgerrit.helpers.Tools;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.EstablishingConnection;
import com.jbirdvegas.mgerrit.message.Finished;
import com.jbirdvegas.mgerrit.message.HandshakeError;
import com.jbirdvegas.mgerrit.message.StartingRequest;
import com.jbirdvegas.mgerrit.objects.GerritMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultGerritReceivers {

    private Activity mActivity;

    // TODO: Add checks to make sure this is always >= 0
    private AtomicInteger mRequestsRunning;

    /**
     * Allows access to the default Gerrit receivers
     * @param activity An activity context (associated directly with a view).
     */
    public DefaultGerritReceivers(Activity activity) {
        mActivity = activity;
        mRequestsRunning = new AtomicInteger(0);
    }

    private final BroadcastReceiver startReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra(GerritMessage.MESSAGE);

            if (mRequestsRunning.get() < 0) mRequestsRunning.set(0);
            if (mRequestsRunning.getAndIncrement() == 0) {
                mActivity.setProgressBarIndeterminateVisibility(true);
            }
        }
    };

    private final BroadcastReceiver finishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mRequestsRunning.decrementAndGet() <= 0) {
                mActivity.setProgressBarIndeterminateVisibility(false);
            }
            if (mRequestsRunning.get() < 0) mRequestsRunning.set(0);
        }
    };

    private final BroadcastReceiver errorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String message = intent.getStringExtra(GerritMessage.MESSAGE),
            url = intent.getStringExtra(GerritMessage.URL);
            Exception exception = (Exception) intent.getSerializableExtra(GerritMessage.EXCEPTION);

            // Application context OK to use here
            Toast.makeText(context,
                    String.format("%s with webaddress: %s", message, url),
                    Toast.LENGTH_LONG).show();
            Tools.showErrorDialog(mActivity, exception);

            if (mRequestsRunning.decrementAndGet() <= 0) {
                mActivity.setProgressBarIndeterminateVisibility(false);
            }
            if (mRequestsRunning.get() < 0) mRequestsRunning.set(0);
        }
    };

    /**
     * Pass in a list of the types of messages to listen for and this will assign the
     *  default handler and register to receive messages for each message type.
     */
    public void registerReceivers(String ... gerritMessageType) {

        List<String> l = new ArrayList<>();
        Collections.addAll(l, gerritMessageType);

        HashMap<String, BroadcastReceiver> typeReceiver = new HashMap<>();
        typeReceiver.put(EstablishingConnection.TYPE, startReceiver);
        typeReceiver.put(StartingRequest.TYPE, startReceiver);
        typeReceiver.put(Finished.TYPE, finishedReceiver);
        typeReceiver.put(HandshakeError.TYPE, errorReceiver);
        typeReceiver.put(ErrorDuringConnection.TYPE, errorReceiver);

        for (Map.Entry<String, BroadcastReceiver> receiver : typeReceiver.entrySet()) {
            String type = receiver.getKey();

            if (l.contains(receiver.getKey())) {
                LocalBroadcastManager.getInstance(mActivity).registerReceiver(receiver.getValue(),
                        new IntentFilter(type));
                l.remove(type);
            }
        }
    }

    // Unregister all the receivers that were registered in registerReceivers
    public void unregisterReceivers() {

        BroadcastReceiver[] receivers = new BroadcastReceiver[] {startReceiver,
                finishedReceiver, errorReceiver};

        for (BroadcastReceiver receiver : receivers) {
            // This should ignore receivers that are not registered
            LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(receiver);
        }

        mRequestsRunning.set(0);
        mActivity.setProgressBarIndeterminateVisibility(false);
    }
}
