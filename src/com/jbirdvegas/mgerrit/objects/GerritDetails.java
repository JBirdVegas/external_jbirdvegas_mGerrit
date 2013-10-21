package com.jbirdvegas.mgerrit.objects;

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
 *
 *
 *  Very basic container class for the Gerrit switcher
 */
public class GerritDetails implements Comparable<GerritDetails> {
    String gerritName;
    String gerritUrl;

    public GerritDetails(String gerritName, String gerritUrl) {
        this.gerritName = gerritName;
        this.gerritUrl = gerritUrl;
    }

    public String getGerritName() {
        return gerritName;
    }

    public void setGerritName(String gerritName) {
        this.gerritName = gerritName;
    }

    public String getGerritUrl() {
        return gerritUrl;
    }

    public void setGerritUrl(String gerritUrl) {
        this.gerritUrl = gerritUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GerritDetails that = (GerritDetails) o;

        return gerritUrl.equals(that.gerritUrl);
    }

    public boolean equals(String s) {
        if (s == null) return false;
        return gerritUrl.equals(s);
    }

    @Override
    public int hashCode() {
        return gerritUrl.hashCode();
    }

    @Override
    public int compareTo(GerritDetails another) {
        return gerritName.compareTo(another.gerritName);
    }
}
