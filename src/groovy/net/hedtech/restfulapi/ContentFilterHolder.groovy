/* ***************************************************************************
 * Copyright 2016 Ellucian Company L.P. and its affiliates.
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


/**
 * A ThreadLocal map of information needed for filtering request content.
 **/
class ContentFilterHolder {

    private static ThreadLocal threadLocal = new ThreadLocal()

    /**
     * Associates a map with a thread.
     * @map map of information needed for filtering request content
     **/
    public static void set( Map map ) {
        threadLocal.set( map)
    }


    /**
     * Returns the map associated with the current thread.
     */
    public static Map get() {
        if (threadLocal.get() != null) {
            return threadLocal.get()
        } else {
            [:]
        }
    }


    /**
     * Clears any Locale associated with the current thread.  This MUST be
     * called prior to returning the thread to the pool.
     */
    public static void clear() {
        threadLocal.set( null )
    }


    /**
     * Returns a string representation of the map
     */
    public static String getString() {
        StringBuffer sb = new StringBuffer()
        sb.append( "ContentFilterHolder " + ContentFilterHolder.get()  )
        return sb.toString()
    }
}
