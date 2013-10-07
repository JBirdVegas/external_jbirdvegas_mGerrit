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

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fima.cardsui.objects.RecyclableCard;
import com.jbirdvegas.mgerrit.GerritControllerActivity;
import com.jbirdvegas.mgerrit.Prefs;
import com.jbirdvegas.mgerrit.R;

public class ProjectCard extends RecyclableCard {

    private GerritControllerActivity mActivity;
    private String mProject;

    public ProjectCard(GerritControllerActivity gerritControllerActivity, String project) {
        mActivity = gerritControllerActivity;
        mProject = project;
    }

    private void removeMe() {
        super.OnSwipeCard();
    }

    @Override
    protected void applyTo(View convertView) {
        TextView project = (TextView) convertView.findViewById(R.id.content);
        project.setText(mProject);
        ImageView removeContentButton = (ImageView) convertView.findViewById(R.id.removable_content_button);
        removeContentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setCurrentProject(v.getContext(), "");
                removeMe();
                mActivity.refreshTabs();
            }
        });
    }

    @Override
    protected int getCardLayoutId() {
        return R.layout.removable_card;
    }
}
