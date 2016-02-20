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

import com.jbirdvegas.mgerrit.message.ErrorDuringConnection;

import org.greenrobot.eventbus.EventBus;

import java.util.HashSet;
import java.util.Iterator;


/**
 * Singleton class facilitating a publisher-subscriber model of events processing.
 * EventBus only retains the last event of that type so we need to add events to a queue
 * Consumers can use EventBus to notify when a new item may have been added to the queue if we want
 * to process more than one event.
 *
 * Note: This class is not thread safe and assumes that there will be only one consumer for each
 * type of event
 */
public class EventQueue {
    // Using an array list so we can push all messages on and filter for only the messages we are
    //  interested in
    private HashSet<GerritMessage> mEvents;
    private static EventQueue mQueue;
    private EventBus mEventBus;

    private EventQueue() {
        mEvents = new HashSet<>();
        mEventBus = EventBus.getDefault();
    }

    public static EventQueue getInstance() {
        if (mQueue == null) {
            mQueue = new EventQueue();
        }
        return mQueue;
    }

    public void enqueue(GerritMessage event, boolean sticky) {
        mEvents.add(event);
        if (sticky) {
            mEventBus.postSticky(event);
        } else {
            mEventBus.post(event);
        }
    }

    public GerritMessage dequeue(Class<? extends GerritMessage> clazz) {
        for (Iterator<GerritMessage> i = mEvents.iterator(); i.hasNext();) {
            GerritMessage element = i.next();
            if (element.getClass() == clazz) {
                i.remove();
                return element;
            }
        }
        return null;
    }

    // TODO: Make a matcher class in the ErrorDuringConnectionClass and pass (a superclass) of this in
    public GerritMessage dequeueWithError(Class<? extends Exception> exceptionClazz) {
        for (Iterator<GerritMessage> i = mEvents.iterator(); i.hasNext();) {
            GerritMessage element = i.next();
            if (element.getClass() == ErrorDuringConnection.class) {
                ErrorDuringConnection error = (ErrorDuringConnection) element;
                if (error.getException().getClass() == exceptionClazz) {
                    i.remove();
                    return element;
                }
            }
        }
        return null;
    }

    public int size() {
        return mEvents.size();
    }

    public boolean isEmpty() {
        return mEvents.isEmpty();
    }
}

