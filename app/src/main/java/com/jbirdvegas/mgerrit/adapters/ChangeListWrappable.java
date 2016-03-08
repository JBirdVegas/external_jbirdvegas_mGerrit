/*
 * Copyright (C) 2016 Android Open Kang Project (AOKP)
 *  Author: Evan Conway (P4R4N01D), 2016
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

package com.jbirdvegas.mgerrit.adapters;

import android.widget.BaseAdapter;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Base class for any top-level adapter managing the change list.
 * Since the StickyListHeaders library requires the adapter implement an interface to specify
 * the section headers directly, any class which could potentially be used as the parent adapter
 * for the change list ListView MUST extend this class.
 */
public abstract class ChangeListWrappable extends BaseAdapter implements StickyListHeadersAdapter {
    // There is  intentionally no methods here.
}
