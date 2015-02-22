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
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.jbirdvegas.mgerrit.objects.AccountEndpoints;
import com.jbirdvegas.mgerrit.tasks.GerritService;

public class SigninActivity extends FragmentActivity
{
    String mCurrentGerritUrl;
    TextView txtUser, txtPass;

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
        AccountEndpoints url = AccountEndpoints.self();
        Intent it = new Intent(this, GerritService.class);
        it.putExtra(GerritService.DATA_TYPE_KEY, GerritService.DataType.Account);
        it.putExtra(GerritService.URL_KEY, url);
        it.putExtra(GerritService.HTTP_USERNAME, txtUser.getText().toString());
        it.putExtra(GerritService.HTTP_PASSWORD, txtPass.getText().toString());
        startService(it);
    }
}
