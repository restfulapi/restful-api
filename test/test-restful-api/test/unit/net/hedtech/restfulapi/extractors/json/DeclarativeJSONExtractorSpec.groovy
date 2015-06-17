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

import org.codehaus.groovy.grails.web.json.JSONObject

import spock.lang.*

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

    def "Test default short object closure for maps"() {
        setup:
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedShortObjectPaths:['customers']
        )
        JSONObject json = new JSONObject([customers:['smith':[_link:'/customers/1'],'johnson':[_link:'/customers/2']]])

        when:
        def map = extractor.extract(json)

        then:
        [customers:['smith':['id':'1'],'johnson':['id':'2']]] == map
    }

    def "Test date parsing"() {
        setup:
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedDatePaths:['customers.date1','customers.date2'],
            dateFormats:["yyyy-MM-dd'T'HH:mm:ssZ","dd.MM.yyyy HH:mm:ss"]
        )
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

    def "Test non-lenient date parsing"() {
        setup:
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedDatePaths:['date'],
            dateFormats:["yyyy-MM-dd"]
        )

        when:
        extractor.extract(new JSONObject(['date':'1982-99-99']))

        then:
        DateParseException e = thrown()
        '1982-99-99'         == e.params[0]
        400                  == e.getHttpStatusCode()
    }

   def "Test lenient date parsing"() {
        setup:
        DeclarativeJSONExtractor extractor = new DeclarativeJSONExtractor(
            dottedDatePaths:['date'],
            dateFormats:["yyyy-MM-dd"],
            lenientDates: true
        )
        def f = new SimpleDateFormat('yyy-MM-dd')
        f.setLenient(true)
        Date expected = f.parse("1982-99-99")

        when:
        def map = extractor.extract(new JSONObject(['date':'1982-99-99']))

        then:
        expected == map['date']
    }

    private String formatDate(Date d, String format) {
        new SimpleDateFormat(format).format(d)
    }
}
