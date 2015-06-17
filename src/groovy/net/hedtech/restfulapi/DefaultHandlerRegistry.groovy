/* ***************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/

package net.hedtech.restfulapi

import java.util.concurrent.atomic.AtomicInteger

/*
Attribution:
Although made generic, this implementation is based on the grails converter configuration
by Siegfried Puchbauer.
*/
/**
 * Mutable implementation of a registry of handlers, that allows them to be
 * arranged in priority order.
 * When asked for a handler for an instance of type T, will iterate over handlers in
 * priority order, return the first one that supports the instance.
 * Handlers with a higher priority are checked first.
 * If two handlers are registered with the same priority, the handler registered second
 * takes priority.
 **/
class DefaultHandlerRegistry<T, H extends Handler<T>> implements HandlerRegistry<T,H> {
    public static final int DEFAULT_PRIORITY = 0

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0)

    private final SortedSet<Entry> handlers = new TreeSet<Entry>();

    void add(H handler) {
        add(handler, DEFAULT_PRIORITY)
    }

    void add(H handler, int priority) {
        handlers.add(new Entry(handler,priority))
    }

    H getHandler(T t) {
        for (Entry entry : handlers) {
            if (entry.handler.supports(t)) {
                return entry.handler
            }
        }
        return null
    }

    List<H> getOrderedHandlers() {
        List<H> list = new ArrayList<H>()
        for (Entry entry : handlers) {
            list.add(entry.handler);
        }
        list
    }


    class Entry implements Comparable<Entry> {
        protected final H handler
        private final int priority
        private final int seq

        private Entry(H handler, int priority) {
            this.handler = handler
            this.priority = priority
            seq = SEQUENCE.incrementAndGet();
        }

        public int compareTo(Entry entry) {
            //if two handlers have the same priority, the one registered last is used
            //the assumption is the last handler registered should override previous ones
            //this ordering is inverted; higher priority/higher sequences handlers are considered
            //to be less, so that they are ordered first in the set.
            return priority == entry.priority ? entry.seq - seq : entry.priority - priority;
        }
    }

}
