package com.aokp.gerrit.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.aokp.gerrit.R;
import com.aokp.gerrit.objects.ChangedFile;

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

    public PatchSetChangedFilesAdapter(Context context, List<ChangedFile> values) {
        super(context, R.layout.patchset_file_changed_list_item, values);
        this.mContext = context;
        this.mValues = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.patchset_file_changed_list_item, null);
        TextView path = (TextView) rowView.findViewById(R.id.changed_file_path);
        TextView inserted = (TextView) rowView.findViewById(R.id.changed_file_inserted);
        TextView deleted = (TextView) rowView.findViewById(R.id.changed_file_deleted);

        ChangedFile changedFile = mValues.get(position);
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
            } else {
                inserted.setText('+' + String.valueOf(changedFile.getInserted()));
                inserted.setTextColor(Color.GREEN);
            }
            // we may not have deleted lines so remove if unneeded
            if (changedFile.getDeleted() == Integer.MIN_VALUE) {
                deleted.setVisibility(View.GONE);
            } else {
                deleted.setText('-' + String.valueOf(changedFile.getDeleted()));
                deleted.setTextColor(Color.RED);
            }
        }
        return rowView;
    }
}