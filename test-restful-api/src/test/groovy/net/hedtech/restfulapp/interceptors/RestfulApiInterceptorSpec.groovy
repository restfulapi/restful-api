package net.hedtech.restfulapp.interceptors


import grails.test.mixin.TestFor
import net.hedtech.restfulapi.RestfulApiInterceptor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(RestfulApiInterceptor)
class RestfulApiInterceptorSpec extends Specification {

    def setup() {
    }

    def cleanup() {

    }

    void "Test my interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"restfulApi")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }

    void "Test my interceptor not matching init action"() {
        when:"A request matches the interceptor"
            withRequest(controller:"restfulApi", action:"init")

        then:"The interceptor does match"
            !interceptor.doesMatch()
    }

    void "Test my interceptor matching non-init action"() {
        when:"A request matches the interceptor"
            withRequest(controller:"restfulApi", action:"list")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }


}
