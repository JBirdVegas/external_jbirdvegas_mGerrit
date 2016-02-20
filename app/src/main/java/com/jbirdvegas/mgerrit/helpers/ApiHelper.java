/*
 * Copyright (C) 2016 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2016
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

package com.jbirdvegas.mgerrit.helpers;

import android.content.Context;

import com.google.gerrit.extensions.api.changes.ChangeApi;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.database.Changes;
import com.urswolfer.gerrit.client.rest.GerritRestApi;

import org.jetbrains.annotations.NotNull;

public class ApiHelper {

    /**
     * Fetch a change object using the Gerrit Rest API.
     *  This will check whether the change has a unique change ID and will use the change number instead
     * @param context Application context for querying the database if necessary
     * @param gerritApi An instance of the Gerrit Rest API, which has been configured
     * @param changeId The change ID
     * @param changeNumber The legacy change number (not used if the change ID is sufficient)
     * @return An instance of this change object for querying data against
     * @throws RestApiException Root exception type for JSON API failures
     */
    public static ChangeApi fetchChange(Context context, @NotNull GerritRestApi gerritApi,
                                        @NotNull String changeId, Integer changeNumber) throws RestApiException {
        ChangeApi change;
        try {
            change = gerritApi.changes().id(changeId);
        } catch (RestApiException e) {
            /* We may have a situation where multiple changes have the same change id
             * this usually occurs when cherry-picking or reverting a commit.
             * We have to fallback to the legacy change number
             * See: http://review.cyanogenmod.org/#/q/change:I6c7a14a9ab4090b4aabf5de7663f5de51bdc4615 */
            if (changeNumber == null) {
                changeNumber = Changes.getChangeNumberForChange(context, changeId);
            }
            if (changeNumber != null) {
                change = gerritApi.changes().id(changeNumber);
            } else {
                throw e;
            }
        }
        return change;
    }
}
