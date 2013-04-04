package com.aokp.gerrit.adapters;

import android.content.Context;
import android.graphics.Color;
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
        View rowView = inflater.inflate(R.layout.patchset_labels_list_item, parent, false);
        TextView path = (TextView) rowView.findViewById(R.id.changed_file_path);
        TextView inserted = (TextView) rowView.findViewById(R.id.changed_file_inserted);
        TextView deleted = (TextView) rowView.findViewById(R.id.changed_file_deleted);

        // we always have a path
        path.setText(mValues.get(position).path);
        // we may not have inserted lines so remove if unneeded
        if (mValues.get(position).inserted == Integer.MIN_VALUE) {
            inserted.setVisibility(View.GONE);
        } else {
            inserted.setText(mValues.get(position).inserted);
            inserted.setTextColor(Color.GREEN);
        }
        // we may not have deleted lines so remove if unneeded
        if (mValues.get(position).deleted == Integer.MIN_VALUE) {
            deleted.setVisibility(View.GONE);
        } else {
            deleted.setText(mValues.get(position).deleted);
            deleted.setTextColor(Color.RED);
        }
        return rowView;
    }
}