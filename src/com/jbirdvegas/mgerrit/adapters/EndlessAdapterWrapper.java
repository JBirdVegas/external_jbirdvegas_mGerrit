package com.jbirdvegas.mgerrit.adapters;


import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import com.jbirdvegas.mgerrit.R;

import org.jetbrains.annotations.Nullable;

/**
 * A wrapper for another adapter. This is a merge of the CommonsWare's
 *  AdapterWrapper and EndlessAdapter libraries of which neither support
 *  CursorAdapters.
 */
public abstract class EndlessAdapterWrapper extends BaseAdapter
    implements AbsListView.OnScrollListener {

    /**
     * The child adapter that this wraps. Most of the work will be delegated to this adapter
     *  and we need to observe when its data changes.
     */
    private BaseAdapter wrapped;
    private Context mContext;

    private View mPendingView;
    private boolean mLoadingMoreData = false;

    /**
     * An adapter that wraps this adapter so we can notify it when either the child adapter's
     *  data or this wraper's data changes.
     */
    private BaseAdapter mParentAdapter;

    public EndlessAdapterWrapper(Context context, BaseAdapter wrapped) {
        this(context, wrapped, R.layout.loading_placeholder);
    }

    /**
     * Constructor wrapping a supplied Adapter and
     * providing a id for a pending view.
     */
    public EndlessAdapterWrapper(Context context, BaseAdapter wrapped, int pendingResource) {
        this.wrapped = wrapped;
        this.mContext = context;

        // We need to intercept data change notifications from the underlying wrapper here
        wrapped.registerDataSetObserver(new DataSetObserver() {
            public void onChanged() {
                finishedDataLoading();
            }

            public void onInvalidated() {
                finishedDataLoading();
            }
        });
        setPendingResource(pendingResource);
    }

    /**
     * Override this method to perform the actual data loading. startDataLoading will be called
     *  for you automatically, and finishedDataLoading will be called when the child adapter's
     *  data changes.
     *
     *  This method should not be called directly, instead call startDataLoading, which will
     *   check if we are already sent a request to load more data and update the listview.
     *
     *  If after trying to load more data, no additional data was found, manually call
     *  finishedDataLoading.
     */
    public abstract void loadData();

    /**
     * Shows the pending view signalling more data is being loaded. Calling this when data
     *  is already being loaded will have no effect.
     *  When manually loading data, be sure to call this.
     */
    public void startDataLoading() {
        // No effect if we have already started loading or this is disabled
        if (mLoadingMoreData) return;
        mLoadingMoreData = true;
        /* We need to notify the listview's adapter that the data has changed (i.e.
         *  we have added the pending row. */
        if (mParentAdapter != null) mParentAdapter.notifyDataSetChanged();
        loadData();
    }

    /**
     * Hides the pending view signalling no data is being loaded. Call this if a load finished
     *  but there was no additional data to display.
     */
    public void finishedDataLoading() {
        mLoadingMoreData = false;
        /* We need to notify the listview's adapter that the data has changed (i.e.
         *  we have removed the pending row. */
        if (mParentAdapter != null) mParentAdapter.notifyDataSetChanged();
    }

    public void setParentAdatper(BaseAdapter child) {
        mParentAdapter = child;
    }

    /**
     * Sets the view to display when more data is being loaded.
     * @param pendingResource Layout to be used for the pending row
     */
    public void setPendingResource(int pendingResource) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mPendingView = inflater.inflate(pendingResource, null);
    }

    /** Sets whether this adapter is enabled or not */
    public void setEnabled(boolean enabled) {
        if (!enabled) mLoadingMoreData = false;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false; // The pending row is never enabled
    }

    @Override
    public boolean isEnabled(int position) {
        return position < wrapped.getCount() && wrapped.isEnabled(position);
    }

    @Override
    public int getCount() {
        if (mLoadingMoreData) {
            // Add an extra item for the pending row
            return wrapped.getCount() + 1;
        } else return wrapped.getCount();
    }

    @Override
    public Object getItem(int position) {
        if (position >= wrapped.getCount()) {
            return null;
        }
        return wrapped.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        if (position >= wrapped.getCount()) {
            return -1;
        }
        return wrapped.getItemId(position);
    }

    @Nullable
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position >= wrapped.getCount()) {
            return mPendingView;
        } else {
            return wrapped.getView(position, convertView, parent);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= wrapped.getCount()) return IGNORE_ITEM_VIEW_TYPE;
        return wrapped.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        // Add another view type for the placeholder
        return wrapped.getViewTypeCount() + 1;
    }

    @Override
    public boolean isEmpty() {
        return !mLoadingMoreData && wrapped.isEmpty();
    }

    /**
     * Returns the ListAdapter that is wrapped by the endless
     * logic.
     */
    public BaseAdapter getWrappedAdapter() {
        return wrapped;
    }

    @Override
    public void onScrollStateChanged(AbsListView listView, int scrollState) {
        if (scrollState == SCROLL_STATE_IDLE) {
            if (listView.getLastVisiblePosition() == wrapped.getCount() - 1) {
                startDataLoading();
            }
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Not used.
    }
}
