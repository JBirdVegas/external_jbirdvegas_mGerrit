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
 */

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Projects implements JsonDeserializer<Projects> {

    private List<Project> projects;

    public Projects() {
        projects = new ArrayList<Project>();
    }

    @Override
    public Projects deserialize(JsonElement jsonElement, Type type,
                               JsonDeserializationContext deserializationContext)
            throws JsonParseException {

        JsonObject json = jsonElement.getAsJsonObject();
        Iterator<Map.Entry<String, JsonElement>> it = json.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, JsonElement> project = it.next();
            String path = project.getKey();
            JsonObject details = project.getValue().getAsJsonObject();
            String kind = details.get("kind").getAsString();
            String id = details.get("id").getAsString();
            projects.add(new Project(path, kind, id));
        }

        return this;
    }

    public List<Project> getAsList() {
        return projects;
    }
}
