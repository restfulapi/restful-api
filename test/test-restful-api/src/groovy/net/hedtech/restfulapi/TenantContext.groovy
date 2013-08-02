/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
*******************************************************************************/
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


