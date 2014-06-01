package com.jbirdvegas.mgerrit.search;

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

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.jbirdvegas.mgerrit.objects.ServerVersion;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class SearchKeyword implements Parcelable {

    public static final String TAG = "SearchKeyword";

    private final String mOpName;
    private final String mOpParam;
    private String mOperator;

    // Initialise the map of search keywords supported
    private static final Set<Class<? extends SearchKeyword>> _CLASSES;
    private static Map<String, Class<? extends SearchKeyword>> _KEYWORDS;
    static {
        _KEYWORDS = new HashMap<>();
        _CLASSES = new HashSet<>();

        // Add each search keyword here
        _CLASSES.add(ChangeSearch.class);
        _CLASSES.add(SubjectSearch.class);
        _CLASSES.add(ProjectSearch.class);
        _CLASSES.add(OwnerSearch.class);
        _CLASSES.add(TopicSearch.class);
        _CLASSES.add(BranchSearch.class);
        _CLASSES.add(AgeSearch.class);

        // This will load the class calling the class's static block
        for (Class<? extends SearchKeyword> clazz : _CLASSES) {
            try {
                Class.forName(clazz.getName());
            } catch (ClassNotFoundException e) {
                Log.e(TAG, String.format("Could not load class '%s'", clazz.getSimpleName()));
            }
        }
    }

    // TODO: All keywords are currently getting these operators. Make an overridable method
    //  to determine whether a keyword supports an operator. Or assume '=' and ignore it
    //  if it doesn't.
    /** Supported searching operators - these are used directly in the SQL query */
    protected static String[] operators = { "=", "<", ">", "<=", ">=" };

    public SearchKeyword(String name, String param) {
        this(name, null, param);
    }

    // We can allow nulls for the parameter but not the name
    public SearchKeyword(String name, String operator, String param) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(String.format("The keyword name of %s was not valid", name));
        }
        mOpName = name;
        mOperator = operator;
        mOpParam = param;
    }

    protected static SearchKeyword getInstance(Parcel in) {
        return buildToken(in.readString());
    }

    protected static void registerKeyword(String opName, Class<? extends SearchKeyword> clazz) {
        _KEYWORDS.put(opName, clazz);
    }

    public String getName() { return mOpName; }
    public String getParam() { return mOpParam; }
    public String getOperator() { return mOperator; }

    @Override
    public String toString() {
        // Keywords with empty parameters are ignored
        if (mOpParam == null || mOpParam.isEmpty()) return "";

        StringBuilder builder = new StringBuilder().append(mOpName).append(":\"");
        if (mOperator != null) builder.append(mOperator);
        builder.append(mOpParam).append("\"");
        return builder.toString();
    }

    @Contract("null -> false")
    protected static boolean isParameterValid(String param) {
        return param != null && !param.isEmpty();
    }

    /**
     * Build a search keyword given a name and its parameter
     * @param name The name of the keyword (a key of _KEYWORDS)
     * @param param Arguments for the token - will not be processed
     * @return A search keyword matching name:param
     */
    private static SearchKeyword buildToken(@NotNull String name, String param) {

        for (Entry<String, Class<? extends SearchKeyword>> entry : _KEYWORDS.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                Constructor<? extends SearchKeyword> constructor;
                try {
                    constructor = entry.getValue().getDeclaredConstructor(String.class);
                    return constructor.newInstance(param);
                } catch (Exception e) {
                    Log.e(TAG, "Could not call constructor for " + name, e);
                }
            }
        }
        return null;
    }

    @Nullable
    private static SearchKeyword buildToken(@NotNull String tokenStr) {
        String[] s = tokenStr.split(":", 2);
        if (s.length == 2) {
            // Remove the beginning and ending double quotes
            s[1] = s[1].replaceAll("^\"|\"$", "");
            return buildToken(s[0], s[1]);
        }
        else return null;
    }

    /**
     * Given a raw search query, this will attempt to process it
     *  into the categories to search for (keywords) and their
     *  arguments.
     * @param query A raw search query in the form that Gerrit uses
     * @return A set of SearchKeywords that can be used to construct
     *  the database query
     */
    public static Set<SearchKeyword> constructTokens(String query) {
        Set<SearchKeyword> set = new HashSet<>();
        StringBuilder currentToken = new StringBuilder();

        for (int i = 0, n = query.length(); i < n; i++) {
            char c = query.charAt(i);
            if (Character.isWhitespace(c)) {
                if (currentToken.length() > 0) {
                    addToSetIfNotNull(buildToken(currentToken.toString()), set);
                    currentToken.setLength(0);
                }
            } else if (c == '"') {
                i = processTo(query, currentToken, i, '"');
            } else if (c == '{') {
                i = processTo(query, currentToken, i, '}');
            } else {
                currentToken.append(c);
            }
        }

        // Have to check if a token was terminated by end of string
        if (currentToken.length() > 0) {
            addToSetIfNotNull(buildToken(currentToken.toString()), set);
        }
        return set;
    }

    public static String getQuery(Set<SearchKeyword> tokens) {
        String query = "";
        for (SearchKeyword token : tokens) {
            query += token.toString() + " ";
        }
        return query.trim();
    }

    private static void addToSetIfNotNull(SearchKeyword token, Set<SearchKeyword> set) {
        if (token != null)  set.add(token);
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
    public String[] getEscapeArgument() {
        return new String[] { getParam() };
    }

    /**
     * Get the Gerrit search query that this keyword corresponds to.
     *  Some keywords do not have corresponding queries supported by Gerrit, so
     *  it is safe to return an empty string in that case. The default implementation
     *  returns an empty string.
     * @param serverVersion The version of the Gerrit instance running on the server
     */
    public String getGerritQuery(ServerVersion serverVersion) {
        return "";
    }

    public static String replaceKeyword(final String query, final SearchKeyword keyword) {
        Set<SearchKeyword> tokens = SearchKeyword.constructTokens(query);
        tokens = replaceKeyword(tokens, keyword);
        return SearchKeyword.getQuery(tokens);
    }

    public static Set<SearchKeyword> replaceKeyword(final Set<SearchKeyword> tokens,
                                                    SearchKeyword keyword) {
        Set<SearchKeyword> retVal = removeKeyword(tokens, keyword.getClass());
        if (isParameterValid(keyword.getParam())) {
            retVal.add(keyword);
        }
        return retVal;
    }

    /**
     * @param tokens A list of search keywords
     * @param keyword An additional age search keyword to be added to the list
     * @return A new set of search keywords, retaining only the oldest AgeSearch keyword
     */
    public static Set<SearchKeyword> retainOldest(final Set<SearchKeyword> tokens,
                                                  @NotNull AgeSearch keyword) {
        List<AgeSearch> ageSearches = new ArrayList<>();
        List<SearchKeyword> otherSearches = new ArrayList<>();

        ageSearches.add(keyword);

        if (tokens.size() > 0) {
            for (SearchKeyword o : tokens) {
                if (o instanceof AgeSearch) ageSearches.add((AgeSearch) o);
                else otherSearches.add(o);
            }

            Collections.sort(ageSearches);
            otherSearches.add(ageSearches.get(0));
        }

        return new HashSet<>(otherSearches);
    }

    public static String addKeyword(String query, SearchKeyword keyword) {
        if (keyword != null && isParameterValid(keyword.getParam())) {
            Set<SearchKeyword> tokens = SearchKeyword.constructTokens(query);
            tokens.add(keyword);
            return SearchKeyword.getQuery(tokens);
        }
        return query;
    }

    public static Set<SearchKeyword> removeKeyword(Set<SearchKeyword> tokens,
                                                   Class<? extends SearchKeyword> clazz) {
        Iterator<SearchKeyword> it = tokens.iterator();
        while (it.hasNext()) {
            Object token = it.next();
            if (token.getClass().equals(clazz)) {
                it.remove();
            }
        }
        return tokens;
    }

    public static int findKeyword(Set<SearchKeyword> tokens, Class<? extends SearchKeyword> clazz) {
        return findKeyword(tokens, clazz, 0);
    }

    public static int findKeyword(Set<SearchKeyword> tokens, Class<? extends SearchKeyword> clazz,
                                  int start) {
        if (start < 0) start = 0;
        int i = 0;
        for (Object token : tokens) {
            if (i < start) i++;
            else if (token.getClass().equals(clazz)) return i;
            else i++;
        }
        return -1;
    }

    protected static String extractOperator(String param) {
        String op = "=";
        for (String operator : operators) {
            if (param.startsWith(operator)) op = operator;
        }
        // '==' also refers to '='
        if (param.startsWith("==")) op = "=";
        return op;
    }

    private static int processTo(String query, StringBuilder currentToken, int i, char token) {
        if (i + 1 >= query.length()) {
            return i + 1;
        } else {
            int index = query.indexOf(token, i + 1);
            if (index < 0) return i + 1;
            // We don't want to store these braces
            currentToken.append(query.substring(i + 1, index));
            return index;
        }
    }

    // --- Parcelable methods
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(toString());
    }

    public static final Parcelable.Creator<SearchKeyword> CREATOR
            = new Parcelable.Creator<SearchKeyword>() {
        public SearchKeyword createFromParcel(Parcel source) {
            return getInstance(source);
        }

        public SearchKeyword[] newArray(int size) {
            return new SearchKeyword[size];
        }
    };
}
