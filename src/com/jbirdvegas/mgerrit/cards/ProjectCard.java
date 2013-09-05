package com.jbirdvegas.mgerrit.cards;

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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.fima.cardsui.objects.Card;
import com.jbirdvegas.mgerrit.GerritControllerActivity;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;

public class ProjectCard extends Card {

    private GerritControllerActivity mGerritControllerActivity;
    private String mProject;

    public ProjectCard(GerritControllerActivity gerritControllerActivity, String project) {
        mGerritControllerActivity = gerritControllerActivity;
        mProject = project;
    }

    @Override
    public View getCardContent(Context context) {
        LayoutInflater layoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View root = layoutInflater.inflate(R.layout.removable_card, null);
        TextView project = (TextView) root.findViewById(R.id.content);
        project.setText(mProject);
        ImageView removeContentButton = (ImageView) root.findViewById(R.id.removable_content_button);
        removeContentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setCurrentProject(v.getContext(), "");
                removeMe();
                mGerritControllerActivity.refreshTabs();
            }
        });
        return root;
    }

    private void removeMe() {
        super.OnSwipeCard();
    }
}
