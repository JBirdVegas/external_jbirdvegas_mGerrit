package com.jbirdvegas.mgerrit.tasks;

import android.content.Context;
import android.content.Intent;
import android.util.Base64;

import com.google.gerrit.extensions.restapi.BinaryResult;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.jbirdvegas.mgerrit.message.ChangeDiffLoaded;
import com.jbirdvegas.mgerrit.objects.CacheManager;
import com.urswolfer.gerrit.client.rest.GerritRestApi;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


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
public class TextDiffProcessor extends SyncProcessor<String> {


    final int mChangeNumber, mPatchsetNumber;

    TextDiffProcessor(Context context, Intent intent) {
        super(context, intent);
        mChangeNumber = intent.getIntExtra(GerritService.CHANGE_NUMBER, 0);
        mPatchsetNumber = intent.getIntExtra(GerritService.PATCHSET_NUMBER, 0);
    }

    @Override
    int insert(String data) {
        /* We don't store change diffs in the database - send out a message instead when we
         * have finished loading it. */
        EventBus.getDefault().postSticky(new ChangeDiffLoaded(getIntent(), getQueueId(), data, mChangeNumber, mPatchsetNumber));
        return 1;
    }

    @Override
    boolean isSyncRequired(Context context) {
        return true;
    }

    @Override
    int count(String version) {
        return 1;
    }

    @Override
    String getData(GerritRestApi gerritApi) throws RestApiException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        String baseString, decodedContent = null;
        BinaryResult binaryResult;
        if (mPatchsetNumber < 1) {
            binaryResult = gerritApi.changes().id(mChangeNumber).revision("current").patch();
        } else {
            binaryResult = gerritApi.changes().id(mChangeNumber).revision(mPatchsetNumber).patch();
        }

        try {
            baseString = binaryResult.asString();
            if (baseString != null) decodedContent = decodeBase64(baseString);

        } catch (IOException | IllegalArgumentException ex) {
            handleException(ex);
        } finally {
            try {
                binaryResult.close();
                byteArrayOutputStream.close();
            } catch (IOException ignored) { }
        }

        return decodedContent;
    }

    /* Save the decoded diff text to the cache.
     * This is pretty important as we fetch the diff for all text files in the change, but
     * will only display one file at a time. Hence, requesting other files in the change will
     * result in a cache hit instead of another query. */
    @Override
    void doPostProcess(String data) {
        CacheManager.put(CacheManager.getDiffKey(mChangeNumber, mPatchsetNumber), data, false);
        /* Cleanup diffs for any superceeded revisions of this change as we will never attempt
         * to fetch them again) */
        for (int i = 0; i < mPatchsetNumber; i++) {
            CacheManager.remove(CacheManager.getDiffKey(mChangeNumber, i), false);
        }
    }

    protected String retreiveFromCache(Intent intent) {
        CacheManager<String> cacheManager = new CacheManager<>();
        return cacheManager.get(CacheManager.getDiffKey(mChangeNumber, mPatchsetNumber), String.class, false);
    }

    /**
     * Android appears to be strict with padding the string according to the Base64 standard.
     * While using the URL_SAFE parameter does not throw an error, it will result in a corrupted string
     * when '+' or '/' characters are included (which could be the case).
     *
     * @see http://stackoverflow.com/questions/2941995/python-ignore-incorrect-padding-error-when-base64-decoding
     * @see https://code.google.com/p/gerrit/issues/detail?id=3312
     *
     * @param base64 The string to decode
     * @return A new string that has been decoded from base64
     */
    private String decodeBase64(String base64) throws IllegalArgumentException {
        int missingPadding = 4 - base64.length() % 4;
        switch (missingPadding) {
            case 1:
                base64 += '=';
                break;
            case 2:
                base64 += "==";
                break;
            case 3:
                base64 += "A==";
                break;
        }
        return new String(Base64.decode(base64, Base64.NO_PADDING));
    }
}
