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
package net.hedtech.restfulapi.extractors.xml

import grails.converters.XML
import grails.test.mixin.*
import grails.test.mixin.web.*
import spock.lang.*
import grails.test.mixin.support.*

import java.text.SimpleDateFormat

@TestMixin([GrailsUnitTestMixin])
class DeclarativeXMLExtractorSpec extends Specification {

    def "Test rename paths"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
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
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
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
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedShortObjectPaths:['outer.inner','customer']
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:BasicXMLExtractor.DEFAULT_SHORT_OBJECT_CLOSURE,
         ['customer']:BasicXMLExtractor.DEFAULT_SHORT_OBJECT_CLOSURE] == rules
    }

    def "Test overridden short object paths"() {
        setup:
        Closure shortObject = { def value -> }
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
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
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
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
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedShortObjectPaths:['customer']
        )
        def xml = XML.parse """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customer>
                <_link>/customers/1234</_link>
            </customer>
        </thing>"""

        when:
        def map = extractor.extract(xml)

        then:
        [customer:[id:'1234']] == map
    }


    def "Test default short object closure for collections"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedShortObjectPaths:['customers']
        )
        def xml = XML.parse """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers array="true">
                <shortObject><_link>/customers/1</_link></shortObject>
                <shortObject><_link>/customers/2</_link></shortObject>
            </customers>
        </thing>"""

        when:
        def map = extractor.extract(xml)

        then:
        [customers:['1','2']] == map
        true                  == map['customers'].getClass().isArray()
    }

    def "Test default short object closure for maps"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedShortObjectPaths:['customers']
        )
        def xml = XML.parse """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true">
                <entry key="smith">
                    <_link>/customers/1</_link>
                </entry>
                <entry key="johnson">
                    <_link>/customers/2</_link>
                </entry>
            </customers>
        </thing>"""

        when:
        def map = extractor.extract(xml)

        then:
        [customers:['smith':['id':'1'],'johnson':['id':'2']]] == map
    }

    def "Test date parsing"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedDatePaths:['customers.date1','customers.date2'],
            dateFormats:["yyyy-MM-dd'T'HH:mm:ssZ","dd.MM.yyyy HH:mm:ss"]
        )
        def base = (1000*(new Date().getTime()/1000).toLong()).toLong()
        Date d1 = new Date( base )
        Date d2 = new Date( base + 55000 )
        def text = """
            <object>
                <customers array="true">
                    <customer>
                        <date1>${formatDate(d1,"yyyy-MM-dd'T'HH:mm:ssZ")}</date1>
                        <date2>${formatDate(d2,"dd.MM.yyyy HH:mm:ss")}</date2>
                    </customer>
                    <customer>
                        <date1>${formatDate(d1,"yyyy-MM-dd'T'HH:mm:ssZ")}</date1>
                        <date2>${formatDate(d2,"dd.MM.yyyy HH:mm:ss")}</date2>
                    </customer>
                </customers>
            </object>
        """
        def xml = XML.parse text

        when:
        def map = extractor.extract(xml)

        then:
        d1 == map['customers'][0]['date1']
        d2 == map['customers'][0]['date2']
        d1 == map['customers'][1]['date1']
        d2 == map['customers'][1]['date2']
    }

    private String formatDate(Date d, String format) {
        new SimpleDateFormat(format).format(d)
    }
}
