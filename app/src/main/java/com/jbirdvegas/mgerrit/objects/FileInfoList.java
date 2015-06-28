package com.jbirdvegas.mgerrit.objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

/**
 * Wrapper for a list of changed files. Provides methods to work with the list of changed files,
 *  delegating most of the work to the underlying FileInfo object.
 */
public class FileInfoList {

    // This list of changed files
    @SerializedName(CommitInfo.KEY_CHANGED_FILES)
    List<ChangedFileInfo> mList;

    private FileInfoList(List<ChangedFileInfo> fileList) {
        this.mList = fileList;
    }

    public List<ChangedFileInfo> getFiles() {
        return mList;
    }

    public static FileInfoList deserialize(JsonObject object) {
        List<ChangedFileInfo> newList = new ArrayList<>();
        Set<Map.Entry<String, JsonElement>> entries = object.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            newList.add(ChangedFileInfo.deserialise(entry.getValue().getAsJsonObject(), entry.getKey()));

        }
        return new FileInfoList(newList);
    }

    @Override
    public String toString() {
        return "FileInfoList{" +
                "mList=" + mList +
                '}';
    }
}
