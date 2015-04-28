package com.jbirdvegas.mgerrit.objects;

/*
 * Copyright (C) 2014 Android Open Kang Project (AOKP)
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

/**
 * Created by Evan on 31/12/2014.
 */
public interface Categorizable {

    /**
     * Category name used for displaying list headers
     * @param position index of item in list
     * @return the category text for the list item at a given position which can be used
     *  for displaying list headers
     */
    public String categoryName(int position);

    public long categoryId(int position);
}
