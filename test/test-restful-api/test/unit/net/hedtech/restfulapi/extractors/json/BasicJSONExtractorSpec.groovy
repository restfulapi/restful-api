/* ***************************************************************************
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
package net.hedtech.restfulapi.extractors.json

import grails.test.mixin.*
import grails.test.mixin.web.*
import grails.test.mixin.support.*

import java.text.SimpleDateFormat

import net.hedtech.restfulapi.extractors.DateParseException
import net.hedtech.restfulapi.extractors.ShortObjectExtractionException

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

    def "Test null date parsing"() {
        setup:
        BasicJSONExtractor.metaClass.getDatePaths << {
            [['date']]
        }
        BasicJSONExtractor extractor = new BasicJSONExtractor()
        JSONObject json = new JSONObject('{"date":null}')

        when:
        def map = extractor.extract(json)

        then:
        null == map['date']
    }

    def "Test that JSONObject.NULL is extracted as a java null"() {
        setup:
        BasicJSONExtractor extractor = new BasicJSONExtractor()
        def s = '''
        {"name":null,
          address:{"line1":"404 lane","line2":null},
          colors:['blue',null,'red']
        }
        '''
        JSONObject json = new JSONObject(s)

        when:
        def map = extractor.extract(json)

        then:
        null                == map['name']
        '404 lane'          == map['address']['line1']
        null                == map['address']['line2']
        ['blue',null,'red'] == map['colors']
    }

    def "Test invalid short object extraction when the collection doesn't contain maps"() {
        setup:
        BasicJSONExtractor.metaClass.getShortObjectPaths << {
            [['foo']]
        }

        BasicJSONExtractor extractor = new BasicJSONExtractor()
        JSONObject json = new JSONObject(['foo':['not a short-object']])

        when:
        extractor.extract(json)

        then:
        ShortObjectExtractionException e = thrown()
        'not a short-object'             == e.params[0]
        400                              == e.getHttpStatusCode()
    }

    def "Test invalid short object extraction when the collection objects don't contain a _link attribute"() {
        setup:
        BasicJSONExtractor.metaClass.getShortObjectPaths << {
            [['foos']]
        }

        BasicJSONExtractor extractor = new BasicJSONExtractor()
        JSONObject json = new JSONObject(['foos':[[name:'bad ref']]])

        when:
        extractor.extract(json)

        then:
        ShortObjectExtractionException e = thrown()
        [name:'bad ref']                 == e.params[0]
        400                              == e.getHttpStatusCode()
    }

    def "Test invalid short object extraction when a map of short-objects doesn't contain a _link attribute"() {
        setup:
        BasicJSONExtractor.metaClass.getShortObjectPaths << {
            [['foos']]
        }

        BasicJSONExtractor extractor = new BasicJSONExtractor()
        JSONObject json = new JSONObject(['foos':[one:[name:'bad ref']]])

        when:
        extractor.extract(json)

        then:
        ShortObjectExtractionException e = thrown()
        [one:[name:'bad ref']]           == e.params[0]
        400                              == e.getHttpStatusCode()
    }

    def "Test invalid short object extraction the value isn't a collection or map"() {
        setup:
        BasicJSONExtractor.metaClass.getShortObjectPaths << {
            [['foo']]
        }

        BasicJSONExtractor extractor = new BasicJSONExtractor()
        JSONObject json = new JSONObject(['foo':'not a short-object'])

        when:
        extractor.extract(json)

        then:
        ShortObjectExtractionException e = thrown()
        'not a short-object'             == e.params[0]
        400                              == e.getHttpStatusCode()
    }

    private String formatDate(Date d, String format) {
        new SimpleDateFormat(format).format(d)
    }

}
