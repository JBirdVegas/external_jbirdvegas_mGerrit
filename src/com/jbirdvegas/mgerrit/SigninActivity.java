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

import com.android.volley.AuthFailureError;
import com.dd.processbutton.iml.ActionProcessButton;
import com.jbirdvegas.mgerrit.database.Changes;
import com.jbirdvegas.mgerrit.database.Users;
import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;
import com.jbirdvegas.mgerrit.message.SigninCompleted;
import com.jbirdvegas.mgerrit.objects.EventQueue;
import com.jbirdvegas.mgerrit.requestbuilders.AccountEndpoints;
import com.jbirdvegas.mgerrit.requestbuilders.ChangeEndpoints;
import com.jbirdvegas.mgerrit.requestbuilders.RequestBuilder;
import com.jbirdvegas.mgerrit.tasks.GerritService;

import de.greenrobot.event.EventBus;

public class SigninActivity extends FragmentActivity
        implements LoaderManager.LoaderCallbacks<Cursor>
{
    private String mCurrentGerritUrl;
    private TextView txtUser, txtPass;
    private ActionProcessButton btnSignIn;

    // Whether this activity is already running. As we launch this automatically when a request
    // requires authentication, we don't want to launch this if it is currently being shown.
    private static boolean mActive = false;

    public static String CLOSE_ON_SUCCESSFUL_SIGNIN = "close on success";
    private boolean closeOnSuccess = false;
    private boolean isProtected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActive = true;

        setTheme(Prefs.getCurrentThemeID(this));

        // Action bar Up affordance
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mCurrentGerritUrl = Prefs.getCurrentGerrit(this);
        String mCurrentGerritName = Prefs.getCurrentGerritName(this);

        setTitle(String.format(getResources().getString(R.string.gerrit_signin_title), mCurrentGerritName));
        setContentView(R.layout.sign_in);

        closeOnSuccess = getIntent().getBooleanExtra(CLOSE_ON_SUCCESSFUL_SIGNIN, false);
        // Currently, we only set closeOnSuccess when prompting authorization for a protected Gerrit
        isProtected = closeOnSuccess;

        TextView textView = (TextView) findViewById(R.id.txtSigninHelp);

        CharSequence formatText;
        if (isProtected) {
            formatText = getResources().getText(R.string.gerrit_signin_protected_help);
            findViewById(R.id.imgProtected).setVisibility(View.VISIBLE);
        } else {
            formatText = getResources().getText(R.string.gerrit_signin_help);
            findViewById(R.id.imgProtected).setVisibility(View.GONE);
        }

        CharSequence linkText = getResources().getText(R.string.gerrit_signin_help_link_text);
        textView.setText(Html.fromHtml(String.format(formatText.toString(), "<a href=\"" + mCurrentGerritUrl + "#/settings/http-password\">" + linkText + "</a>")), TextView.BufferType.SPANNABLE);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        txtUser = (TextView) findViewById(R.id.txtUser);
        txtPass = (TextView) findViewById(R.id.txtPass);

        btnSignIn = (ActionProcessButton) findViewById(R.id.btnSignin);
        btnSignIn.setMode(ActionProcessButton.Mode.ENDLESS);

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().registerSticky(this);
        mActive = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        mActive = false;
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
        addUsernamePasswordToIntent(it, null, null);
        startService(it);
    }

    public void onLogout(View view) {
        Users.logout(this);
        Changes.unstarAllChanges(this);
        txtUser.setText("");
        txtPass.setText("");
        btnSignIn.setProgress(0);
        findViewById(R.id.btnLogout).setVisibility(View.GONE);
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
            findViewById(R.id.btnLogout).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> accountInfoLoader) {
        // Not used
    }

    public void onEventMainThread(SigninCompleted ev) {
        btnSignIn.setProgress(100);
        findViewById(R.id.txtAuthFailure).setVisibility(View.GONE);
        findViewById(R.id.btnLogout).setVisibility(View.VISIBLE);

        String username = ev.getUsername();
        String password = ev.getPassword();

        // Sign in successful, retry all the error messages we have queued
        EventQueue errorQueue = EventQueue.getInstance();
        ErrorDuringConnection error;
        do {
            error = (ErrorDuringConnection) errorQueue.dequeueWithError(AuthFailureError.class);
            if (error != null) {
                Intent it = error.getIntent();
                // Exclude account info checks as these are to check the login is correct
                if (it.getSerializableExtra(GerritService.DATA_TYPE_KEY) != GerritService.DataType.Account) {
                    // The intent may have already had the username and password included, so overwrite these with the latest
                    addUsernamePasswordToIntent(it, username, password);
                    startService(it);
                }
            }
        } while (error != null);

        // Get the starred changes
        RequestBuilder starredUrl = ChangeEndpoints.starred().setLimit(this.getResources().getInteger(R.integer.changes_limit));
        Intent it = new Intent(this, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Commit);
        it.putExtra(GerritService.URL_KEY, starredUrl);
        it.putExtra(GerritService.CHANGES_LIST_DIRECTION, GerritService.Direction.Older);
        addUsernamePasswordToIntent(it, username, password);
        startService(it);


        if (closeOnSuccess) {
            // If this activity was started automatically, then we should close it and show a toast
            Toast.makeText(this, "You have successfully signed in!", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    public void onEventMainThread(ErrorDuringConnection ev) {
        btnSignIn.setProgress(-1);
        if (ev.getException().getClass() == AuthFailureError.class) {
            findViewById(R.id.txtAuthFailure).setVisibility(View.VISIBLE);
        }
    }

    private Intent addUsernamePasswordToIntent(Intent it, String username, String password) {
        if (username == null) username = txtUser.getText().toString();
        if (password == null) password = txtPass.getText().toString();
        it.putExtra(GerritService.HTTP_USERNAME, username);
        it.putExtra(GerritService.HTTP_PASSWORD, password);
        return it;
    }

    public static boolean isActive() {
        return mActive;
    }

}
