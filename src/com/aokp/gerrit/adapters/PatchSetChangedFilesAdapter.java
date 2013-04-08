package com.aokp.gerrit.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.aokp.gerrit.R;
import com.aokp.gerrit.objects.ChangedFile;
import com.aokp.gerrit.objects.JSONCommit;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jbird
 * Date: 4/4/13 12:45 AM
 */
public class PatchSetChangedFilesAdapter extends ArrayAdapter<ChangedFile> {
    private static final String TAG = PatchSetChangedFilesAdapter.class.getSimpleName();
    private static final boolean VERBOSE = false;
    private final Context mContext;
    private final List<ChangedFile> mValues;
    private final JSONCommit mCommit;

    public PatchSetChangedFilesAdapter(Context context, List<ChangedFile> values, JSONCommit commit) {
        super(context, R.layout.patchset_file_changed_list_item, values);
        this.mContext = context;
        this.mValues = values;
        this.mCommit = commit;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.patchset_file_changed_list_item, null);
        final ChangedFile changedFile = mValues.get(position);
        Log.d(TAG, "Total number of changed Files: " + mValues.size());
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String base = "http://gerrit.sudoservers.com/#/c/%d/%d/%s";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(base,
                        mCommit.getCommitNumber(),
                        mCommit.getPatchSetNumber(),
                        changedFile.getPath())));
                mContext.startActivity(browserIntent);
            }
        });
        TextView path = (TextView) rowView.findViewById(R.id.changed_file_path);
        TextView inserted = (TextView) rowView.findViewById(R.id.changed_file_inserted);
        TextView deleted = (TextView) rowView.findViewById(R.id.changed_file_deleted);
        TextView insText = (TextView) rowView.findViewById(R.id.inserted_text);
        TextView delText = (TextView) rowView.findViewById(R.id.deleted_text);
        String changedFilePath = changedFile.getPath();
        int insertedInFile = changedFile.getInserted();
        int deletedInFile = changedFile.getDeleted();
        if (VERBOSE) {
            Log.d(TAG, "File change stats Path=" + changedFilePath
                    + " inserted=" + insertedInFile
                    + " deleted=" + deletedInFile
                    + " objectToString()=" + changedFile.toString());
        }
        // we always have a path
        if (path != null) {
            path.setText(changedFilePath);
            // we may not have inserted lines so remove if unneeded
            if (changedFile.getInserted() == Integer.MIN_VALUE) {
                inserted.setVisibility(View.GONE);
                insText.setVisibility(View.GONE);
            } else {
                inserted.setText('+' + String.valueOf(changedFile.getInserted()));
                inserted.setTextColor(Color.GREEN);
                insText.setTextColor(Color.GREEN); //remove?
            }
            // we may not have deleted lines so remove if unneeded
            if (changedFile.getDeleted() == Integer.MIN_VALUE) {
                deleted.setVisibility(View.GONE);
                deleted.setVisibility(View.GONE);
            } else {
                deleted.setText('-' + String.valueOf(changedFile.getDeleted()));
                deleted.setTextColor(Color.RED);
                delText.setTextColor(Color.RED); //remove?
            }
        }
        return rowView;
    }
}