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

import com.google.gerrit.extensions.common.FileInfo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jbirdvegas.mgerrit.helpers.Tools;

public class ChangedFileInfo extends FileInfo {

    public String path;

    // Need to do a lot of null checking here as the Gerrit API likes returning nulls
    public ChangedFileInfo(String filePath, FileInfo info) {
        path = filePath;
        oldPath = info.oldPath;
        linesInserted = (info.linesInserted == null) ? 0 : info.linesInserted;
        linesDeleted = (info.linesDeleted == null) ? 0 : info.linesDeleted;
        status = (info.status == null) ? 'M' : info.status;
        binary = (info.binary == null) ? false : info.binary;
    }

    // File status
    public enum Status {
        ADDED ('A'),
        DELETED('D'),
        RENAMED('R'),
        COPIED('C'),
        REWRITTEN('W'),
        MODIFIED('M');

        private final Character statusCode;

        Status(char statusCode) {
            this.statusCode = statusCode;
        }

        public Character getStatusCode() {
            return statusCode;
        }

        public static Status getValue(final String value) {
            if (value == null) return MODIFIED;
            Character status = value.charAt(0);
            for (Status s : values()) {
                if (status.equals(s.getStatusCode())) return s;
                else if (status.equals(s.name())) return s;
            }

            return MODIFIED;
        };
    }

    public boolean isImage() {
        return (binary && Tools.isImage(path));
    }

    public static ChangedFileInfo deserialise(JsonObject object, String _path) {
        ChangedFileInfo file = new Gson().fromJson(object, ChangedFileInfo.class);
        file.path = _path;
        return file;
    }

    @Override
    public String toString() {
        return "FileInfo{" +
                "path='" + path + '\'' +
                ", oldPath='" + oldPath + '\'' +
                ", inserted=" + linesInserted +
                ", deleted=" + linesDeleted +
                ", status=" + status +
                ", isBinary=" + binary +
                '}';
    }
}