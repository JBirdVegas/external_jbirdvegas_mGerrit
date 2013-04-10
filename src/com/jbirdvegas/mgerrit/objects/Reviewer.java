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

public class Reviewer {
    public static final String NO_SCORE = "No score";
    public static final String CODE_REVIEW_PLUS_TWO = "Looks good to me, approved";
    public static final String CODE_REVIEW_PLUS_ONE = "Looks good to me, but someone else must approve";
    public static final String CODE_REVIEW_MINUS_ONE = "I would prefer that you didn\u0027t submit this";
    public static final String CODE_REVIEW_MINUS_TWO = "Do not submit";
    public static final String VERIFIED_PLUS_ONE = "Verified";
    public static final String VERIFIED_MINUS_ONE = "Fails";

    private Reviewer(String val, String _name) {
        value = val;
        name = _name;
    }

    public static Reviewer getReviewerInstance(String val, String name) {
        return new Reviewer(val, name);
    }

    private String value;
    private String name;

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Reviewer");
        sb.append("{value='").append(value).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}