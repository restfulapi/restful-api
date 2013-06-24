/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.extractors.json

import grails.test.mixin.*
import grails.test.mixin.web.*
import spock.lang.*
import grails.test.mixin.support.*
import org.codehaus.groovy.grails.web.json.JSONObject

@TestMixin([GrailsUnitTestMixin])
class DeclarativeJSONExtractorSpec extends Specification {

    def "Test rename paths"() {
        setup:
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedRenamedPaths:['outer.inner':'renamed','name':'lastName']
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.renameRules

        then:
        [['outer','inner']:'renamed',
         ['name']:'lastName'] == rules
    }

    def "Test default value paths"() {
        setup:
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedValuePaths:['outer.inner':true, 'name':'Smith']
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.defaultValueRules

        then:
        [['outer','inner']:true,
         ['name']:'Smith'] == rules
         [:] == extractor.transformer.rules.renameRules
    }

    def "Test default short object paths"() {
        setup:
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedShortObjectPaths:['outer.inner','customer']
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:BasicJSONExtractor.DEFAULT_SHORT_OBJECT_CLOSURE,
         ['customer']:BasicJSONExtractor.DEFAULT_SHORT_OBJECT_CLOSURE] == rules
    }

    def "Test overridden short object paths"() {
        setup:
        Closure shortObject = { def value -> }
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedShortObjectPaths:['outer.inner','customer'],
            shortObjectClosure:shortObject
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:shortObject,
         ['customer']:shortObject] == rules
    }

    def "Test flatten paths"() {
        setup:
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedFlattenedPaths:['outer.inner','customer']
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.flattenPaths

        then:
        [['outer','inner'],['customer']] == rules
    }

    def "Test default short object closure for singletons"() {
        setup:
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedShortObjectPaths:['customer']
        )
        JSONObject json = new JSONObject([customer:[_link:'/customers/1234']])

        when:
        def map = extractor.extract(json)

        then:
        [customer:[id:'1234']] == map
    }


    def "Test default short object closure for collections"() {
        setup:
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedShortObjectPaths:['customers']
        )
        JSONObject json = new JSONObject([customers:[[_link:'/customers/1'],[_link:'/customers/2']]])

        when:
        def map = extractor.extract(json)

        then:
        [customers:['1','2']] == map
        true                  == map['customers'].getClass().isArray()
    }
}