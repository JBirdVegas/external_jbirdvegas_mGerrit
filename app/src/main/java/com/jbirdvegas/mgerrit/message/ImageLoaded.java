package com.jbirdvegas.mgerrit.message;

import android.content.Intent;
import android.graphics.Bitmap;

import com.jbirdvegas.mgerrit.objects.ChangedFileInfo;
import com.jbirdvegas.mgerrit.objects.GerritMessage;
import com.jbirdvegas.mgerrit.tasks.GerritService;

/*
 * Copyright (C) 2015 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2015
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
public class ImageLoaded extends GerritMessage {

    private final Bitmap mBitmap;

    public ImageLoaded(Intent intent, int queueId, Bitmap image) {
        super(intent, queueId);
        this.mBitmap = image;
    }

    public Bitmap getImage() { return mBitmap; }

    public String getFilePath() {
        return getIntent().getStringExtra(GerritService.FILE_PATH);
    }

    public ChangedFileInfo.Status getFileStatus() {
        return (ChangedFileInfo.Status) getIntent().getSerializableExtra(GerritService.FILE_STATUS);
    }
}
