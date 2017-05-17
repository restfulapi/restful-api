package net.hedtech.restfulapi

import grails.artefact.Interceptor
import net.hedtech.restfulapp.TenantContext

// This interceptor supports testing of tenant retrieval
// in RestfulApiControllerFunctionalSpec
class TenantInterceptor implements Interceptor {


    TenantInterceptor() {
       match(controller: "restfulApi", action: "*").except(action:"init")
    }

    /**
     * Executed before a matched action
     *
     * @return Whether the action should continue and execute
     */
    boolean before() {
        if (params.tenant) {
            // The tenant will be in the params map if URL mapping is configured
            // to recognizes a 'tenant' URL part or if a query parameter was used.
            TenantContext.set( params.tenant )
        }
        true
    }

    /**
     * Executed after the action executes but prior to view rendering
     *
     * @return True if view rendering should continue, false otherwise
     */
        boolean after() { true }

    /**
     * Executed after view rendering completes
     */
        void afterView() {}
}
