package net.hedtech.restfulapi

import grails.artefact.Interceptor

class RestfulApiInterceptor implements Interceptor {

    def headerNameService

    RestfulApiInterceptor() {
       match(controller: "restfulApi", action: "*").except(action:"init")
    }

    /**
     * Executed before a matched action
     *
     * @return Whether the action should continue and execute
     */
    boolean before() {

        // COR headers
        header( "Access-Control-Allow-Origin", "http://localhost:8080" )
        header( "Access-Control-Allow-Credentials", "true" )
        header( "Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE" )
        header( "Access-Control-Max-Age", "3600" )
        header( 'Access-Control-Expose-Headers', true )

        // request id setup
        String requestIdHeader = headerNameService.getHeaderName('requestId', 'X-Request-ID')
        headerNameService.setRequestIdAttribute(request, requestIdHeader)
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
