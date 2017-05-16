package net.hedtech.restfulapi

import grails.artefact.Interceptor

import static java.util.UUID.randomUUID


class RestfulApiInterceptor implements Interceptor {

    def requestIdHeader = "X-Request-ID"

    RestfulApiInterceptor() {
        //matchAll().excludes(controller:"restfulApi", action:"init")
    }

    /**
     * Executed before a matched action
     *
     * @return Whether the action should continue and execute
     */
    boolean before() {
        // we'll set an attribute that may be used for logging
        if (request.getHeader(requestIdHeader)) {
            request.request_id = request.getHeader(requestIdHeader)
        }
        else {
            request.request_id = randomUUID()
        }
        request.request_id
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
