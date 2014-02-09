package com.jbirdvegas.mgerrit.tasks;

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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.jbirdvegas.mgerrit.objects.CommitInfo;
import com.jbirdvegas.mgerrit.objects.CommitterObject;
import com.jbirdvegas.mgerrit.objects.JSONCommit;
import com.jbirdvegas.mgerrit.objects.Projects;
import com.jbirdvegas.mgerrit.objects.Reviewer;
import com.jbirdvegas.mgerrit.objects.ReviewerList;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public final class Deserializers {

    private static final String KEY_ALL = "all";

    // Custom deserializer as we are separating the one object into two (the committer is not nested)
    private static final JsonDeserializer<Reviewer> d_reviewer = new JsonDeserializer<Reviewer>() {

        @Override
        public Reviewer deserialize(JsonElement jsonElement,
                                    Type type,
                                    JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {

            Reviewer reviewer = new Gson().fromJson(jsonElement, type);
            reviewer.setCommitter(new Gson().fromJson(jsonElement, CommitterObject.class));
            return reviewer;
        }
    };

    // Custom deserializer to get all the reviewers and their associated labels
    private static final JsonDeserializer<ReviewerList> d_reviewers = new JsonDeserializer<ReviewerList>() {

        @Override
        public ReviewerList deserialize(JsonElement jsonElement,
                                        Type type,
                                        JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {

            ArrayList<Reviewer> reviewers = new ArrayList<>();

            JsonObject labels = jsonElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> labelParent : labels.entrySet()) {
                JsonObject label = labelParent.getValue().getAsJsonObject();
                if (label.has(KEY_ALL)) {
                    Reviewer[] rs = jsonDeserializationContext.deserialize(label.getAsJsonArray(KEY_ALL),
                            Reviewer[].class);
                    for (Reviewer r : rs) {
                        r.setLabel(labelParent.getKey()); // Set the label the value corresponds to
                    }
                    reviewers.addAll(Arrays.asList(rs));
                }
            }
            return new ReviewerList(reviewers);
        }
    };

    private static final JsonDeserializer<JSONCommit> d_commit = new JsonDeserializer<JSONCommit>() {

        @Override
        public JSONCommit deserialize(JsonElement jsonElement,
                                      Type type,
                                      JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {

            Gson gson = new Gson();
            JSONCommit commit = gson.fromJson(jsonElement, type);
            JsonObject object = jsonElement.getAsJsonObject();

            commit.setReviewers(jsonDeserializationContext.<ReviewerList>deserialize(object.getAsJsonObject("labels"),
                    ReviewerList.class));

            // Set the Revision number
            if (commit.getCurrentRevision() == null) {
                if (object.has(JSONCommit.KEY_REVISIONS)) {
                    JsonObject revisionsObj = object.get(JSONCommit.KEY_REVISIONS).getAsJsonObject();
                    Set<Map.Entry<String, JsonElement>> entries = revisionsObj.entrySet();
                    for (Map.Entry<String, JsonElement> entry : entries) {
                        commit.setCurrentRevision(entry.getKey());
                        break;
                    }
                }
            }

            // If we don't have a revision number, there is no further information
            String currentRevision = commit.getCurrentRevision();
            if (currentRevision == null) return commit;

            JsonObject revisionsObj = object.get(JSONCommit.KEY_REVISIONS).getAsJsonObject();
            JsonObject psObj = revisionsObj.get(currentRevision).getAsJsonObject();

            commit.setPatchSet(CommitInfo.deserialise(psObj, commit.getChangeId()));
            return commit;
        }
    };

    // Register all of the custom deserializers here
    public static void addDeserializers(@NotNull GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(Projects.class, new Projects());
        gsonBuilder.registerTypeAdapter(Reviewer.class, d_reviewer);
        gsonBuilder.registerTypeAdapter(ReviewerList.class, d_reviewers);
        gsonBuilder.registerTypeAdapter(JSONCommit.class, d_commit);
    }
}
