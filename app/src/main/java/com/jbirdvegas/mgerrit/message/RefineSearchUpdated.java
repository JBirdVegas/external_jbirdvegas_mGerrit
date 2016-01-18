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

package com.jbirdvegas.mgerrit.message;

import com.jbirdvegas.mgerrit.search.SearchKeyword;

import java.util.Collection;

/**
 * Used by the refine search activity when the search keywords (filters) change.
 */
public class RefineSearchUpdated {

    private final Collection<SearchKeyword> mKeywords;

    public RefineSearchUpdated() {
        mKeywords = null;
    }

    public RefineSearchUpdated(Collection<SearchKeyword> keywords) {
        this.mKeywords = keywords;
    }

    public Collection<SearchKeyword> getKeywords() {
        return mKeywords;
    }
}
