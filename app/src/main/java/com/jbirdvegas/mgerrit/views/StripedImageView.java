package com.jbirdvegas.mgerrit.views;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2014
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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.jbirdvegas.mgerrit.R;
import com.jbirdvegas.mgerrit.objects.FileInfo;

public class StripedImageView extends LinearLayout {

    private final int mAddedStripe_color;
    private final int mDeletedStripe_color;
    private final int mModifiedStripe_color;

    private final ImageView mImageView;
    private View mLeftStripe;
    private View mRightStripe;

    public StripedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StripedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StripedImageView, 0, 0);
        mAddedStripe_color = a.getColor(R.styleable.StripedImageView_addedStripeColor, R.color.text_green);
        mDeletedStripe_color = a.getColor(R.styleable.StripedImageView_deletedStripeColor, R.color.text_red);
        mModifiedStripe_color = a.getColor(R.styleable.StripedImageView_modifiedStripeColor, android.R.color.transparent);
        a.recycle();

        LayoutInflater.from(context).inflate(R.layout.striped_image, this);
        mLeftStripe = findViewById(R.id.striped_image_stripe_left);
        mRightStripe = findViewById(R.id.striped_image_stripe_right);
        mImageView = (ImageView) findViewById(R.id.striped_image);
    }

    public void setStripe(FileInfo.Status fileStatus) {
        if (fileStatus == FileInfo.Status.ADDED) {
            mLeftStripe.setBackgroundColor(mAddedStripe_color);
            mRightStripe.setBackgroundColor(mAddedStripe_color);
        } else if (fileStatus == FileInfo.Status.DELETED) {
            mLeftStripe.setBackgroundColor(mDeletedStripe_color);
            mRightStripe.setBackgroundColor(mDeletedStripe_color);
        } else {
            mLeftStripe.setBackgroundColor(mModifiedStripe_color);
            mRightStripe.setBackgroundColor(mModifiedStripe_color);
        }
    }

    public void setImageBitmap(Bitmap bitmap) {
        mImageView.setImageBitmap(bitmap);
        mImageView.setVisibility(VISIBLE);
    }
}
