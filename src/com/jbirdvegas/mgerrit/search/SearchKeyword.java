package com.jbirdvegas.mgerrit.search;

import android.content.Context;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import com.jbirdvegas.mgerrit.database.UserChanges;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/*
 * Copyright (C) 2013 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2013
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
public abstract class SearchKeyword {

    public static final String TAG = "SearchKeyword";

    private final String mOpName;
    private final String mOpParam;
    private String mOperator;

    // Initialise the map of search keywords supported
    private static final Set<Class<? extends SearchKeyword>> _CLASSES;
    private static Map<String, Class<? extends SearchKeyword>> _KEYWORDS;
    static {
        _KEYWORDS = new HashMap<String, Class<? extends SearchKeyword>>();
        _CLASSES = new HashSet<Class<? extends SearchKeyword>>();

        // Add each search keyword here
        _CLASSES.add(ChangeSearch.class);
        _CLASSES.add(SubjectSearch.class);

        // This will load the class calling the class's static block
        for (Class<? extends SearchKeyword> clazz : _CLASSES) {
            try {
                Class.forName(clazz.getName());
            } catch (ClassNotFoundException e) {
                Log.e(TAG, String.format("Could not load class '%s'", clazz.getSimpleName()));
            }
        }
    }

    public SearchKeyword(String name, String param) {
        this.mOpName = name;
        this.mOpParam = param;
    }

    public SearchKeyword(String name, String operator, String param) {
        mOpName = name;
        mOperator = operator;
        mOpParam = param;
    }

    protected static void registerKeyword(String opName, Class<? extends SearchKeyword> clazz) {
        _KEYWORDS.put(opName, clazz);
    }

    public String getName() { return mOpName; }
    public String getParam() { return mOpParam; }
    public String getOperator() { return mOperator; }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder().append(mOpName).append(":\"");
        if (mOperator != null) builder.append(mOperator);
        builder.append(mOpParam).append("\"");
        return builder.toString();
    }

    /**
     * Build a search keyword given a name and its parameter
     * @param name
     * @param param
     * @return
     */
    private static SearchKeyword buildToken(String name, String param) {

        Iterator<Entry<String, Class<? extends SearchKeyword>>> it = _KEYWORDS.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Class<? extends SearchKeyword>> entry = it.next();
            if (name.equalsIgnoreCase(entry.getKey())) {
                Constructor<? extends SearchKeyword> constructor = null;
                try {
                    constructor = entry.getValue().getDeclaredConstructor(String.class);
                    return constructor.newInstance(param);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private static SearchKeyword buildToken(String tokenStr) {
        String[] s = tokenStr.split(":", 2);
        if (s.length == 2) return buildToken(s[0], s[1]);
        else return null;
    }

    public static Set<SearchKeyword> constructTokens(String query) {
        Set<SearchKeyword> set = new HashSet<SearchKeyword>();
        String currentToken = "";

        for (int i = 0, n = query.length(); i < n; i++) {
            char c = query.charAt(i);
            if (Character.isWhitespace(c)) {
                if (currentToken.length() > 0) {
                    addToSetIfNotNull(buildToken(currentToken), set);
                    currentToken = "";
                }
            } else if (c == '"') {
                int index = query.indexOf('"', i + 1);
                currentToken += query.substring(i, index);
                i = index + 1; // We have processed this many characters
            } else if (c == '{') {
                int index = query.indexOf('}', i + 1);
                currentToken += query.substring(i, index + 1);
                i = index + 1; // We have processed this many characters
            } else {
                currentToken += c;
            }
        }

        // Have to check if a token was terminated by end of string
        if (currentToken.length() > 0) {
            addToSetIfNotNull(buildToken(currentToken), set);
        }
        return set;
    }

    private static void addToSetIfNotNull(SearchKeyword token, Set<SearchKeyword> set) {
        if (token != null) {
            set.add(token);
        }
    }

    public static String constructDbSearchQuery(Set<SearchKeyword> tokens) {
        StringBuilder whereQuery = new StringBuilder();
        Iterator<SearchKeyword> it = tokens.iterator();
        while (it.hasNext()) {
            SearchKeyword token = it.next();
            if (token == null) continue;
            whereQuery.append(token.buildSearch());
            if (it.hasNext()) whereQuery.append(" AND ");
        }
        return whereQuery.toString();
    }

    public abstract String buildSearch();

    /**
     * Formats the bind argument for query binding.
     * May be overriden to include wildcards in the parameter for like queries
     */
    public String getEscapeArgument() {
        return getParam();
    }
}
