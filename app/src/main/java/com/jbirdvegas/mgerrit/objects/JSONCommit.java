package com.jbirdvegas.mgerrit.objects;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Jon Stanford (JBirdVegas), 2013
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

import android.content.Context;

import com.google.gerrit.extensions.common.ChangeInfo;
import com.jbirdvegas.mgerrit.R;

public class JSONCommit extends ChangeInfo {

    // public
    public static final String KEY_STATUS_OPEN = "open";
    public static final String KEY_STATUS_MERGED = "merged";
    public static final String KEY_STATUS_ABANDONED = "abandoned";
    public static final String KEY_STATUS = "status";
    public static final String KEY_ID = "id";

    // internal
    public static final String KEY_REVISIONS = "revisions";

    public enum Status {
        NEW {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explanation_new);
            }
        },
        SUBMITTED {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explanation_submitted);
            }
        },
        MERGED {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explanation_merged);
            }
        },
        ABANDONED {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explanation_abandoned);
            }
        },
        DRAFT {
            @Override
            public String getExplaination(Context c) {
                return c.getString(R.string.status_explanation_draft);
            }
        };

        /**
         * Returns an explanation of the status users can understand
         *
         * @param c Context of caller to access resources
         * @return human/user readable status explanation
         */
        public String getExplaination(Context c) {
            return null;
        }

        public static Status getStatusFromString(final String status) {
            String status_lower = status.toLowerCase();
            switch (status_lower) {
                case KEY_STATUS_OPEN:
                case "new":
                    return NEW;
                case KEY_STATUS_MERGED:
                    return MERGED;
                case KEY_STATUS_ABANDONED:
                    return ABANDONED;
            }
            return SUBMITTED;
        }

        // Convert the status to a Status enum instance and back again
        public static String getStatusString(final String status) {
            return getStatusFromString(status).toString();
        }
    }
}
