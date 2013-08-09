/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.extractors.json

import grails.test.mixin.*
import grails.test.mixin.web.*
import grails.test.mixin.support.*

import java.text.SimpleDateFormat

import net.hedtech.restfulapi.extractors.DateParseException

import org.codehaus.groovy.grails.web.json.JSONObject

import spock.lang.*


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

    def "Test date parsing"() {
        setup:
        BasicJSONExtractor.metaClass.getDatePaths << {
            [['customers','date1'],['customers','date2']]
        }
        BasicJSONExtractor.metaClass.getDateFormats << {
            ["yyyy-MM-dd'T'HH:mm:ssZ", "dd.MM.yyyy HH:mm:ss"]
        }
        BasicJSONExtractor extractor = new BasicJSONExtractor()
        def base = (1000*(new Date().getTime()/1000).toLong()).toLong()
        Date d1 = new Date( base )
        Date d2 = new Date( base + 55000 )
        JSONObject json = new JSONObject([customers:
            [
                ['date1':formatDate(d1,"yyyy-MM-dd'T'HH:mm:ssZ"),'date2':formatDate(d2,"dd.MM.yyyy HH:mm:ss")],
                ['date1':formatDate(d1,"yyyy-MM-dd'T'HH:mm:ssZ"),'date2':formatDate(d2,"dd.MM.yyyy HH:mm:ss")],
            ]])

        when:
        def map = extractor.extract(json)

        then:
        d1 == map['customers'][0]['date1']
        d2 == map['customers'][0]['date2']
        d1 == map['customers'][1]['date1']
        d2 == map['customers'][1]['date2']
    }

    def "Test invalid date parsing"() {
        setup:
        BasicJSONExtractor.metaClass.getDatePaths << {
            [['date']]
        }
        BasicJSONExtractor extractor = new BasicJSONExtractor()
        JSONObject json = new JSONObject(['date':'not a date'])

        when:
        extractor.extract(json)

        then:
        DateParseException e = thrown()
        'not a date'         == e.params[0]
        400                  == e.getHttpStatusCode()
    }

    private String formatDate(Date d, String format) {
        new SimpleDateFormat(format).format(d)
    }

}
