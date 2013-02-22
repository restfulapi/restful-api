/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import grails.converters.JSON

import grails.test.mixin.*
import org.junit.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(RestfulApiController)
class RestfulApiControllerTests {

    public void setUp() {
        JSONExtractorConfigurationHolder.clear()
    }

    public void tearDown() {
        JSONExtractorConfigurationHolder.clear()
    }

    void testUnmappedAcceptForList() {
        def mock = mockFor(ThingService)
        mock.demand.list(1..1) { Map params -> return [totalCount:0,instances:['foo']] }
        controller.metaClass.getService = {-> mock.createMock()}
        //test that if the format of the response is not an xml or json variant, that
        //a 406 is returned
        response.format = 'html'
        request.method = "GET"
        params.pluralizedResourceName = 'things'
        controller.list()

        assert response.status == 406
        assert 0 == response.getContentLength()

        mock.verify()
    }

    void testUnmappedAcceptForShow() {
        def mock = mockFor(ThingService)
        mock.demand.show(1..1) { Map params -> return [instance:'foo'] }
        controller.metaClass.getService = {-> mock.createMock()}
        //test that if the format of the response is not an xml or json variant, that
        //a 406 is returned
        response.format = 'html'
        request.method = "GET"
        params.pluralizedResourceName = 'things'
        params.id = '1'
        controller.show()

        assert response.status == 406
        assert 0 == response.getContentLength()

        mock.verify()
    }

    void testUnmappedAcceptForSave() {
        JSONExtractorConfigurationHolder.registerExtractor( "things", "json", new DefaultJSONExtractor() )

        def mock = mockFor(ThingService)
        mock.demand.create(1..1) { Map params -> return [instance:'foo'] }
        controller.metaClass.getService = {-> mock.createMock()}
        //test that if the format of the response is not an xml or json variant, that
        //a 406 is returned
        request.format = 'json'
        response.format = 'html'
        request.method = "POST"
        params.pluralizedResourceName = 'things'
        controller.save()

        assert response.status == 406
        assert 0 == response.getContentLength()

        mock.verify()
    }

    void testUnmappedAcceptForUpdate() {
        JSONExtractorConfigurationHolder.registerExtractor( "things", "json", new DefaultJSONExtractor() )

        def mock = mockFor(ThingService)
        mock.demand.update(1..1) { Map params -> return [instance:'bar'] }
        controller.metaClass.getService = {-> mock.createMock()}
        //test that if the format of the response is not an xml or json variant, that
        //a 406 is returned
        request.format = 'json'
        response.format = 'html'
        request.method = "PUT"
        params.pluralizedResourceName = 'things'
        params.id = '1'
        controller.update()

        assert response.status == 406
        assert 0 == response.getContentLength()

        mock.verify()
    }

}
