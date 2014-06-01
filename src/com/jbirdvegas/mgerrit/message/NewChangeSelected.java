package com.jbirdvegas.mgerrit.message;

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
public class NewChangeSelected {

    String mChangeId;
    int mChangeNumber;
    String mStatus;
    boolean mExpand;

    public NewChangeSelected(String changeId, int changeNumber, String status) {
        this(changeId, changeNumber, status, true);
    }

    public NewChangeSelected(String changeId, int changeNumber, String status, boolean expand) {
        this.mChangeId = changeId;
        this.mChangeNumber = changeNumber;
        this.mStatus = status;
        this.mExpand = expand;
    }

    public NewChangeSelected(String changeId, String status, boolean expand) {
        this.mChangeId = changeId;
        this.mStatus = status;
        this.mExpand = expand;
    }

    public String getChangeId() {
        return mChangeId;
    }

    public int getChangeNumber() {
        return mChangeNumber;
    }

    public String getStatus() {
        return mStatus;
    }

    public boolean isExpanded() {
        return mExpand;
    }
}
