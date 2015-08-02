package com.jbirdvegas.mgerrit.tasks;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.fragments.PrefsFragment;
import com.jbirdvegas.mgerrit.message.ImageLoaded;
import com.jbirdvegas.mgerrit.objects.ChangedFileInfo;
import com.urswolfer.gerrit.client.rest.GerritRestApi;
import com.urswolfer.gerrit.client.rest.RestClient;

import org.apache.http.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.zip.ZipInputStream;

import de.greenrobot.event.EventBus;

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
public class ImageProcessor extends SyncProcessor<Bitmap> {

    private static final String DEFAULT_CHARSET = "UTF-8";

    int mChangeNumber, mPatchsetNumber;
    final String mPath;
    final ChangedFileInfo.Status mFileStatus;

    ImageProcessor(Context context, Intent intent) {
        super(context, intent);
        mChangeNumber = intent.getIntExtra(GerritService.CHANGE_NUMBER, 0);
        mPatchsetNumber = intent.getIntExtra(GerritService.PATCHSET_NUMBER, 0);
        mPath = intent.getStringExtra(GerritService.FILE_PATH);
        mFileStatus = (ChangedFileInfo.Status) intent.getSerializableExtra(GerritService.FILE_STATUS);
    }

    @Override
    int insert(Bitmap data) {
        /* We don't store images in the database -send out a message instead when we
         * have finished loading it.
         * TODO Cache responses. */
        EventBus.getDefault().post(new ImageLoaded(getIntent(), getQueueId(), data));
        return 1;
    }

    @Override
    boolean isSyncRequired(Context context) {
        return true;
    }

    @Override
    int count(Bitmap version) {
        return 1;
    }

    @Override
    Bitmap getData(GerritRestApi gerritApi) throws RestApiException {
        ZipInputStream zis = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            HttpResponse response = gerritApi.restClient().requestRest(getBinaryDownloadUrl(), null,
                    RestClient.HttpVerb.GET);

            zis = new ZipInputStream(response.getEntity().getContent());

            int bytesRead;
            byte[] buffer = new byte[8192];
            if (zis.getNextEntry() != null) {
                while ((bytesRead = zis.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            handleException(e);
        } finally {
            try {
                output.close();
                if (zis != null) zis.close();
            } catch (IOException ignored) { }
        }

        return getBitmap(output.toByteArray());
    }

    /** Format: <Gerrit>/cat/<change number>,<mRevision number>,<path>^<parent>
     * Gerrit: The current Gerrit instance
     * Change number: The change number of the change where the file was added/modified/removed
     * Revision number: The mRevision number
     * Path: Full file path of the file to retrieve
     * Parent: 0 to get new file (added), 1 to get old file (removed)
     */
    private String getBinaryDownloadUrl() throws UnsupportedEncodingException {
        boolean wasDeleted = (mFileStatus == ChangedFileInfo.Status.DELETED);
        // Url Encoding must be applied to the change and mRevision args
        String needsEncoded = URLEncoder.encode(String.format("%d,%d", mChangeNumber, mPatchsetNumber),
                DEFAULT_CHARSET);
        // Url Encoding must also be applied to the postpended arg
        String postPend = URLEncoder.encode("^", DEFAULT_CHARSET);
        char parent = (wasDeleted ? '1' : '0');
        return String.format("/cat/%s,%s%s%c", needsEncoded, mPath, postPend, parent);
    }

    private Bitmap getBitmap(byte[] data) {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        return bitmap;
    }
}
