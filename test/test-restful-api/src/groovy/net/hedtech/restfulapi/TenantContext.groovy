/* ****************************************************************************
 * Copyright 2013 Ellucian Company L.P. and its affiliates.
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
 * A thread local to hold the current tenant.
 * This is provided for illustration purposes only -- other implementations 
 * (e.g., setting the tenant onto the Session, persisting into a key-store, etc.) 
 * may be appropriate.
 */
public class TenantContext {


    private static ThreadLocal storage = new ThreadLocal()


    public static String get() {
        (storage.get() ?: '') as String
    }


    public static void set( String tenant ) {
        storage.set tenant
    }


    public static void clear() {
        storage.set null
    }

}


