package com.jbirdvegas.mgerrit.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GerritTeamsHelper;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 6/3/13 5:52 PM
 */
public class AddTeamView extends View {
    private static final String TAG = AddTeamView.class.getSimpleName();

    public interface RefreshCallback {
        public void refreshScreenCallback();
    }

    private final Context mContext;
    private final AlertDialog mAlertDialog;
    private Button mSendButton;
    private RefreshCallback mRefreshCallback;

    public AddTeamView(Context context, AlertDialog alertDialog) {
        super(context);
        mContext = context;
        mAlertDialog = alertDialog;
    }

    public View getView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = inflater.inflate(R.layout.add_team_dialog, null);
        mSendButton = (Button) root.findViewById(R.id.add_team_save);
        final EditText name = (EditText) root.findViewById(R.id.add_team_name_edittext);
        final EditText url = (EditText) root.findViewById(R.id.add_team_url_edittext);

        // disabled till they provide both a name and url
        mSendButton.setEnabled(false);

        // Request focus and show soft keyboard automatically
        //imm.showSoftInput(name, InputMethodManager.SHOW_IMPLICIT);
        name.requestFocusFromTouch();

        // set listeners
        // TODO: name should also be checked for validity
        //name.addTextChangedListener(ensureEditText(null));
        url.addTextChangedListener(ensureEditText(url));

        // set hints
        name.setHint(R.string.please_enter_gerrit_name);
        url.setHint(R.string.please_enter_gerrit_url);
        // preset preface
        url.setText("https://");

        Button saveButton = (Button) root.findViewById(R.id.add_team_save);
        saveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String teamName = name.getText().toString().trim();
                String teamUrl = url.getText().toString().trim();
                // ensure we end with /
                if ('/' != teamUrl.charAt(teamUrl.length() - 1)) {
                    teamUrl += "/";
                }
                Log.v(TAG, "saving url: " + teamUrl);
                GerritTeamsHelper.saveTeam(teamName, teamUrl);
                Prefs.setCurrentGerrit(getContext(), teamUrl);
                mAlertDialog.dismiss();
                if (mRefreshCallback != null) {
                    mRefreshCallback.refreshScreenCallback();
                }
            }
        });
        return root;
    }

    private TextWatcher ensureEditText(final EditText urlEditText) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                String chars = charSequence.toString().trim();
                if (chars.isEmpty()) {
                    mSendButton.setEnabled(false);
                } else {
                    if (urlIsAcceptable(chars)) {
                        mSendButton.setEnabled(true);
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                String chars = charSequence.toString().trim();
                if (chars.isEmpty()) {
                    mSendButton.setEnabled(false);
                } else {
                    if (urlIsAcceptable(chars)) {
                        mSendButton.setEnabled(true);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String chars = editable.toString().trim();
                if (chars.isEmpty()) {
                    mSendButton.setEnabled(false);
                } else {
                    if (urlIsAcceptable(chars)) {
                        mSendButton.setEnabled(true);
                    }
                }
            }
        };
    }

    private boolean urlIsAcceptable(String url) {
        return URLUtil.isHttpUrl(url)
                || URLUtil.isHttpsUrl(url)
                && url.contains(".");
    }

    public AddTeamView addRefreshScreenCallback(RefreshCallback callback) {
        mRefreshCallback = callback;
        return this;
    }
}
