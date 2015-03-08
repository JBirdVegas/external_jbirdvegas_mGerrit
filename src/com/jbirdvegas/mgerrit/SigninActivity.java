package com.jbirdvegas.mgerrit;

/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.processbutton.iml.ActionProcessButton;
import com.jbirdvegas.mgerrit.database.Users;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.SigninCompleted;
import com.jbirdvegas.mgerrit.objects.AccountEndpoints;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class SigninActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor>
{
    private String mCurrentGerritUrl;
    private TextView txtUser, txtPass;
    private ActionProcessButton btnSignIn;

    List<ErrorDuringConnection> errorQueue;

    public static String CLOSE_ON_SUCCESSFUL_SIGNIN = "close on success";
    private boolean closeOnSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(Prefs.getCurrentThemeID(this));

        // Action bar Up affordance
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mCurrentGerritUrl = Prefs.getCurrentGerrit(this);
        String mCurrentGerritName = Prefs.getCurrentGerritName(this);

        setTitle(String.format(getResources().getString(R.string.gerrit_signin_title), mCurrentGerritName));
        setContentView(R.layout.sign_in);

        TextView textView = (TextView) findViewById(R.id.txtSigninHelp);
        CharSequence formatText = getResources().getText(R.string.gerrit_signin_help);
        CharSequence linkText = getResources().getText(R.string.gerrit_signin_help_link_text);
        textView.setText(Html.fromHtml(String.format(formatText.toString(), "<a href=\"" + mCurrentGerritUrl + "#/settings/http-password\">" + linkText + "</a>")), TextView.BufferType.SPANNABLE);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        txtUser = (TextView) findViewById(R.id.txtUser);
        txtPass = (TextView) findViewById(R.id.txtPass);

        btnSignIn = (ActionProcessButton) findViewById(R.id.btnSignin);
        btnSignIn.setMode(ActionProcessButton.Mode.ENDLESS);

        closeOnSuccess = getIntent().getBooleanExtra(CLOSE_ON_SUCCESSFUL_SIGNIN, false);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSignin(View view) {
        btnSignIn.setProgress(1);

        AccountEndpoints url = AccountEndpoints.self();
        Intent it = new Intent(this, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Account);
        it.putExtra(GerritService.URL_KEY, url);
        it.putExtra(GerritService.HTTP_USERNAME, txtUser.getText().toString());
        it.putExtra(GerritService.HTTP_PASSWORD, txtPass.getText().toString());
        startService(it);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return Users.getSelf(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> accountInfoLoader, Cursor info) {
        if (info.moveToFirst()) {
            String username = info.getString(info.getColumnIndex(Users.C_USENRAME));
            String password = info.getString(info.getColumnIndex(Users.C_PASSWORD));
            txtUser.setText(username);
            txtPass.setText(password);

            btnSignIn.setProgress(100);
            findViewById(R.id.txtAuthFailure).setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> accountInfoLoader) {
        // Not used
    }

    public void onEventMainThread(SigninCompleted ev) {
        btnSignIn.setProgress(100);
        findViewById(R.id.txtAuthFailure).setVisibility(View.GONE);

        // Sign in successful, retry all the error messages we have queued
        if (errorQueue != null) {
            for (ErrorDuringConnection error : errorQueue) {
                startService(error.getIntent());
            }
        }
        if (closeOnSuccess) {
            // If this activity was started automatically, then we should close it and show a toast
            Toast.makeText(this, "You have successfully signed in!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    public void onEventMainThread(ErrorDuringConnection ev) {
        btnSignIn.setProgress(-1);
        if (ev.getException().getClass() == com.android.volley.AuthFailureError.class) {
            findViewById(R.id.txtAuthFailure).setVisibility(View.VISIBLE);
        }
        // Add the error onto the queue so we can retry it when sign in succeeds.
        if (errorQueue == null) {
            errorQueue = new ArrayList<>();
        }
        errorQueue.add(ev);
    }


}
