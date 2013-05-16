package com.jbirdvegas.mgerrit.interfaces;

import android.view.View;
import com.jbirdvegas.mgerrit.objects.CommitterObject;

/**
 * Created by jbird on 5/16/13.
 */
public interface OnContextItemSelectedCallback {
    public boolean menuItemSelected(CommitterObject committerObject, int position);
}
