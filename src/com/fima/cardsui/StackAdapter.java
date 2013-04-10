package com.fima.cardsui;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.fima.cardsui.objects.AbstractCard;
import com.fima.cardsui.objects.CardStack;

import java.util.ArrayList;

public class StackAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<AbstractCard> mStacks;
    private boolean mSwipeable;

    public StackAdapter(Context context, ArrayList<AbstractCard> stacks,
                        boolean swipable) {
        mContext = context;
        mStacks = stacks;
        mSwipeable = swipable;

    }

    @Override
    public int getCount() {
        return mStacks.size();
    }

    @Override
    public CardStack getItem(int position) {
        return (CardStack) mStacks.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final CardStack stack = getItem(position);
        stack.setAdapter(this);
        stack.setPosition(position);

        // TODO: caching is not working well

        // if (convertView != null) {
        // CardStack tagStack = (CardStack) convertView.getTag();
        // ArrayList<Card> tagCards = tagStack.getCards();
        // ArrayList<Card> cards = stack.getCards();
        // Card lastTagCard = tagCards.get(tagCards.size()-1);
        // if (!lastTagCard.equals(cards.get(cards.size()-1))) {
        // convertView = stack.getView(mContext);
        // convertView.setTag(stack);
        // }
        // } else if (convertView == null) {
        convertView = stack.getView(mContext, mSwipeable);
        // convertView.setTag(stack);
        // }

        return convertView;
    }

    public void setItems(ArrayList<AbstractCard> stacks) {
        mStacks = stacks;
        notifyDataSetChanged();
    }

    public void setSwipeable(boolean b) {
        mSwipeable = b;
    }

    public void setItems(CardStack cardStack, int position) {
        mStacks.set(position, cardStack);
    }

}
