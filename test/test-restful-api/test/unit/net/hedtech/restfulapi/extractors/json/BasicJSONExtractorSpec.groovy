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
class BasicJSONExtractorSpec extends Specification {

    def "Test rename paths"() {
        setup:
        BasicJSONExtractor.metaClass.getRenamePaths << {
            [['outer','inner']:'renamed',
             ['name']:'lastName']
        }

        when:
        BasicJSONExtractor extractor = new BasicJSONExtractor()
        def rules = extractor.transformer.rules.renameRules

        then:
        [['outer','inner']:'renamed',
         ['name']:'lastName'] == rules
    }

    def "Test default value paths"() {
        setup:
        BasicJSONExtractor.metaClass.getDefaultValuePaths << {
            [['outer','inner']:true,
             ['name']:'Smith']
        }

        when:
        BasicJSONExtractor extractor = new BasicJSONExtractor()
        def rules = extractor.transformer.rules.defaultValueRules

        then:
        [['outer','inner']:true,
         ['name']:'Smith'] == rules
         [:] == extractor.transformer.rules.renameRules
    }

    def "Test default short object paths"() {
        setup:
        BasicJSONExtractor.metaClass.getShortObjectPaths << {
            [['outer','inner'],['customer']]
        }

        when:
        BasicJSONExtractor extractor = new BasicJSONExtractor()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:BasicJSONExtractor.DEFAULT_SHORT_OBJECT_CLOSURE,
         ['customer']:BasicJSONExtractor.DEFAULT_SHORT_OBJECT_CLOSURE] == rules
    }

    def "Test overridden short object paths"() {
        setup:
        BasicJSONExtractor.metaClass.getShortObjectPaths << {
            [['outer','inner'],['customer']]
        }
        Closure shortObject = { def value -> }
        BasicJSONExtractor.metaClass.getShortObjectClosure << {
            shortObject
        }

        when:
        BasicJSONExtractor extractor = new BasicJSONExtractor()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:shortObject,
         ['customer']:shortObject] == rules
    }

    def "Test flatten paths"() {
        setup:
        BasicJSONExtractor.metaClass.getFlattenPaths << {
            [['outer','inner'],['customer']]
        }

        when:
        BasicJSONExtractor extractor = new BasicJSONExtractor()
        def rules = extractor.transformer.rules.flattenPaths

        then:
        [['outer','inner'],['customer']] == rules
    }

    def "Test default short object closure for singletons"() {
        setup:
        BasicJSONExtractor.metaClass.getShortObjectPaths << {
            [['customer']]
        }
        JSONObject json = new JSONObject([customer:[_link:'/customers/1234']])
        BasicJSONExtractor extractor = new BasicJSONExtractor()

        when:
        def map = extractor.extract(json)

        then:
        [customer:[id:'1234']] == map
    }


    def "Test default short object closure for collections"() {
        setup:
        BasicJSONExtractor.metaClass.getShortObjectPaths << {
            [['customers']]
        }
        JSONObject json = new JSONObject([customers:[[_link:'/customers/1'],[_link:'/customers/2']]])
        BasicJSONExtractor extractor = new BasicJSONExtractor()

        when:
        def map = extractor.extract(json)

        then:
        [customers:['1','2']] == map
        true                  == map['customers'].getClass().isArray()

    }

    def "Test default short object closure for maps"() {
        setup:
        BasicJSONExtractor.metaClass.getShortObjectPaths << {
            [['customers']]
        }
        JSONObject json = new JSONObject([customers:['smith':[_link:'/customers/1'],'johnson':[_link:'/customers/2']]])
        BasicJSONExtractor extractor = new BasicJSONExtractor()

        when:
        def map = extractor.extract(json)

        then:
        [customers:['smith':['id':'1'],'johnson':['id':'2']]] == map
    }
}