package com.jbirdvegas.mgerrit.cards;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.Comment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/12/13 4:03 PM
 */
public class PatchSetCommentsCard extends Card {
    private static final String TAG = PatchSetCommentsCard.class.getSimpleName();
    private final Comment mComment;
    private String mJsonString;
    private LayoutInflater mInflater;
    private List<Comment> mCommentList;

    public PatchSetCommentsCard(String jsonString) {
        mJsonString = jsonString;
        mCommentList = new LinkedList<Comment>();
        try {
            mComment = new Comment("test", new JSONObject(jsonString));
        } catch (JSONException e) {
            throw new ExceptionInInitializerError("Failed to parse comments!");
        }
    }

    @Override
    public View getCardContent(Context context) {
        mInflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup rootView = (ViewGroup) mInflater.inflate(R.layout.linear_layout, null);
        try {
            JSONObject object = new JSONObject(mJsonString);
            JSONArray keysArray = object.names();
            for (int i = 0; keysArray.length() > i; i++) {
                try {
                    String path = (String) keysArray.get(i);
                    mCommentList.add(new Comment(path, object.getJSONObject(path)));
                } catch (JSONException e) {
                    // no path?
                }
            }
            for (Comment comment : mCommentList) {
                rootView.addView(getCommentView(comment));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to generate comments!", e);
        }

        return rootView;
    }

    private View getCommentView(Comment comment) {
        View innerView = mInflater.inflate(R.layout.comments_card, null);
        setTextView(innerView, R.id.comment_card_comment, comment.getMessage());
        setTextView(innerView, R.id.comment_card_name, comment.getAuthorName());
        setTextView(innerView, R.id.comment_card_timestamp, comment.getTimeStamp());
        setTextView(innerView, R.id.comment_card_in_reply_to, comment.getInReplyTo());
        setTextView(innerView, R.id.comment_card_path, comment.getFile());
        setTextView(innerView, R.id.comment_card_line_number, String.valueOf(comment.getLineNumber()));
        return innerView;
    }

    private void setTextView(View innerView, int res, String text) {
        if (text == null) {
            innerView.findViewById(res).setVisibility(View.GONE);
        } else {
            ((TextView) innerView.findViewById(res)).setText(text);
        }
    }
}
