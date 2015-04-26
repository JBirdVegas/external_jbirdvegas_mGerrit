package com.jbirdvegas.mgerrit.cards;

import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2014
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

/**
 * Interface for including card in the change details list.
 */
public interface CardBinder {

    /**
     * Binds the next row of the cursor column to the specified container view.
     * @param cursor The cursor to get the data from
     * @param convertView The old view to reuse, if possible. You should check that this view is
     *                    non-null and of an appropriate type before using. If it is not possible
     *                    to convert this view to display the correct data, this method can create
     *                    a new view. It is not guaranteed that the convertView will have been
     *                    previously created.
     * @param parent The parent that this view will eventually be attached to
     * @return The view where the data was bound
     */
    public View setViewValue(Cursor cursor, View convertView, ViewGroup parent);
}
