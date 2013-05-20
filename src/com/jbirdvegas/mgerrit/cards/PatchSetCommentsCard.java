package com.jbirdvegas.mgerrit.cards;

import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.PatchSetViewerActivity;
import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.helpers.EmoticonSupportHelper;
import com.jbirdvegas.mgerrit.helpers.GravatarHelper;
import com.jbirdvegas.mgerrit.listeners.TrackingClickListener;
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
    private final PatchSetViewerActivity mPatchsetViewerActivity;

    public PatchSetCommentsCard(JSONCommit jsonCommit, PatchSetViewerActivity activity) {
        mJsonCommit = jsonCommit;
        mPatchsetViewerActivity = activity;
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
        // make and add a view for each comment
        for (CommitComment comment : commentsList) {
            mRootView.addView(getCommentView(comment));
        }
        return mRootView;
    }

    public View getCommentView(final CommitComment comment) {
        View commentView = mInflater.inflate(R.layout.commit_comment, null);
        // set author name
        TextView authorTextView = (TextView) commentView.findViewById(R.id.comment_author_name);
        authorTextView.setText(comment.getAuthorObject().getName());
        authorTextView.setOnClickListener(
                new TrackingClickListener(mPatchsetViewerActivity,
                        comment.getAuthorObject()));

        authorTextView.setTag(comment.getAuthorObject());
        mPatchsetViewerActivity.registerViewForContextMenu(authorTextView);
        // setup styled comments
        TextView commentMessage = (TextView) commentView.findViewById(R.id.comment_message);
        // replace replace emoticons with drawables
        commentMessage.setText(EmoticonSupportHelper.getSmiledText(mContext, comment.getMessage()));
        // use Linkify to automatically linking http/email/addresses
        Linkify.addLinks(commentMessage, Linkify.ALL);
        // set gravatar icon for commenter
        GravatarHelper.populateProfilePicture(
                (ImageView) commentView.findViewById(R.id.comment_gravatar),
                comment.getAuthorObject().getEmail());
        return commentView;
    }
}
