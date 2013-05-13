package com.jbirdvegas.mgerrit.cards;

import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.objects.CommitComment;
import com.jbirdvegas.mgerrit.objects.JSONCommit;

import java.util.LinkedList;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 5/13/13 12:54 AM
 */
public class PatchSetCommentsCard extends Card {

    private JSONCommit mJsonCommit;

    public PatchSetCommentsCard(JSONCommit jsonCommit) {
        mJsonCommit = jsonCommit;
    }

    private LayoutInflater mInflater;
    private Context mContext;
    private ViewGroup mRootView;

    @Override
    public View getCardContent(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = (ViewGroup) mInflater.inflate(R.layout.comments_card, null);
        LinkedList<CommitComment> commentsList = (LinkedList<CommitComment>) mJsonCommit.getMessagesList();
        for (CommitComment comment : commentsList) {
            mRootView.addView(getCommentView(comment));
        }
        return mRootView;
    }

    public View getCommentView(CommitComment comment) {
        View commentView = mInflater.inflate(R.layout.commit_comment, null);
        ((TextView) commentView.findViewById(R.id.comment_author_name))
                .setText(comment.getAuthorObject().getName());
        TextView commentMessage = (TextView) commentView.findViewById(R.id.comment_message);
        commentMessage.setText(comment.getMessage());
        Linkify.addLinks(commentMessage, Linkify.ALL);
        GravatarHelper.populateProfilePicture(
                (ImageView) commentView.findViewById(R.id.comment_gravatar),
                comment.getAuthorObject().getEmail());
        return commentView;
    }
}
