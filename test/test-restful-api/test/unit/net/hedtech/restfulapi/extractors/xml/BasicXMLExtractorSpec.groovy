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

import grails.test.mixin.*
import grails.test.mixin.web.*
import spock.lang.*
import grails.test.mixin.support.*
import grails.converters.XML

import java.text.SimpleDateFormat

import net.hedtech.restfulapi.extractors.DateParseException
import net.hedtech.restfulapi.extractors.ShortObjectExtractionException


@TestMixin([GrailsUnitTestMixin])
class BasicXMLExtractorSpec extends Specification {

    def "Test rename paths"() {
        setup:
        BasicXMLExtractor.metaClass.getRenamePaths << {
            [['outer','inner']:'renamed',
             ['name']:'lastName']
        }

        when:
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def rules = extractor.transformer.rules.renameRules

        then:
        [['outer','inner']:'renamed',
         ['name']:'lastName'] == rules
    }

    def "Test default value paths"() {
        setup:
        BasicXMLExtractor.metaClass.getDefaultValuePaths << {
            [['outer','inner']:true,
             ['name']:'Smith']
        }

        when:
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def rules = extractor.transformer.rules.defaultValueRules

        then:
        [['outer','inner']:true,
         ['name']:'Smith'] == rules
         [:] == extractor.transformer.rules.renameRules
    }

    def "Test default short object paths"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['outer','inner'],['customer']]
        }

        when:
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:BasicXMLExtractor.DEFAULT_SHORT_OBJECT_CLOSURE,
         ['customer']:BasicXMLExtractor.DEFAULT_SHORT_OBJECT_CLOSURE] == rules
    }

    def "Test overridden short object paths"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['outer','inner'],['customer']]
        }
        Closure shortObject = { def value -> }
        BasicXMLExtractor.metaClass.getShortObjectClosure << {
            shortObject
        }

        when:
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:shortObject,
         ['customer']:shortObject] == rules
    }

    def "Test flatten paths"() {
        setup:
        BasicXMLExtractor.metaClass.getFlattenPaths << {
            [['outer','inner'],['customer']]
        }

        when:
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def rules = extractor.transformer.rules.flattenPaths

        then:
        [['outer','inner'],['customer']] == rules
    }

    def "Test default short object closure for singletons"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['customer']]
        }
        def content = '<?xml version="1.0" encoding="UTF-8"?><thing><customer><_link>/customers/1234</_link></customer></thing>'
        def xml = XML.parse content
        BasicXMLExtractor extractor = new BasicXMLExtractor()

        when:
        def map = extractor.extract(xml)

        then:
        [customer:[id:'1234']] == map
    }


    def "Test default short object closure for collections"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['customers']]
        }
        def content = '<?xml version="1.0" encoding="UTF-8"?><thing><customers array="true"><shortObject><_link>/customers/1</_link></shortObject><shortObject><_link>/customers/2</_link></shortObject></customers></thing>'
        def xml = XML.parse content
        BasicXMLExtractor extractor = new BasicXMLExtractor()

        when:
        def map = extractor.extract(xml)

        then:
        [customers:['1','2']] == map
    }

    def "Test default short object closure for maps"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['customers']]
        }
        def content = """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true">
                <entry key="smith"><_link>/customers/1</_link></entry>
                <entry key="johnson"><_link>/customers/2</_link></entry>
            </customers>
        </thing>"""
        def xml = XML.parse content
        BasicXMLExtractor extractor = new BasicXMLExtractor()

        when:
        def map = extractor.extract(xml)

        then:
        [customers:['smith':['id':'1'],'johnson':['id':'2']]] == map
    }

    def "Test date parsing"() {
        setup:
        BasicXMLExtractor.metaClass.getDatePaths << {
            [['customers','date1'],['customers','date2']]
        }
        BasicXMLExtractor.metaClass.getDateFormats << {
            ["yyyy-MM-dd'T'HH:mm:ssZ", "dd.MM.yyyy HH:mm:ss"]
        }
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def base = (1000*(new Date().getTime()/1000).toLong()).toLong()
        Date d1 = new Date( base )
        Date d2 = new Date( base + 55000 )
        def text = """<?xml version="1.0" encoding="UTF-8"?>
            <object>
                <customers array="true">
                    <object>
                        <date1>${formatDate(d1,"yyyy-MM-dd'T'HH:mm:ssZ")}</date1>
                        <date2>${formatDate(d2,"dd.MM.yyyy HH:mm:ss")}</date2>
                    </object>
                    <object>
                        <date1>${formatDate(d1,"yyyy-MM-dd'T'HH:mm:ssZ")}</date1>
                        <date2>${formatDate(d2,"dd.MM.yyyy HH:mm:ss")}</date2>
                    </object>
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

    def "Test invalid date parsing"() {
        setup:
        BasicXMLExtractor.metaClass.getDatePaths << {
            [['date']]
        }
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def xml = XML.parse "<object><date>not a date</date></object>"

        when:
        extractor.extract(xml)

        then:
        DateParseException e = thrown()
        'not a date'         == e.params[0]
        400                  == e.getHttpStatusCode()
    }

    def "Test invalid short object extraction when the collection doesn't contain maps"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['foos']]
        }

        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def xml = XML.parse '<object><foos array="true"><foo>not a short-object</foo></foos></object>'

        when:
        extractor.extract(xml)

        then:
        ShortObjectExtractionException e = thrown()
        'not a short-object'             == e.params[0]
        400                              == e.getHttpStatusCode()
    }

    def "Test invalid short object extraction when the collection objects don't contain a _link attribute"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['foos']]
        }

        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def xml = XML.parse '<object><foos array="true"><foo><name>bad ref</name></foo></foos></object>'

        when:
        extractor.extract(xml)

        then:
        ShortObjectExtractionException e = thrown()
        [name:'bad ref']                 == e.params[0]
        400                              == e.getHttpStatusCode()
    }

    def "Test invalid short object extraction when a map of short-objects doesn't contain a _link attribute"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['foos']]
        }

        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def xml = XML.parse """<object>
                                    <foos map="true">
                                        <entry key="one"><name>bad ref</name></entry>
                                    </foos>
                               </object>"""

        when:
        extractor.extract(xml)

        then:
        ShortObjectExtractionException e = thrown()
        [one:[name:'bad ref']]           == e.params[0]
        400                              == e.getHttpStatusCode()
    }

    def "Test invalid short object extraction the value isn't a collection or map"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['foo']]
        }

        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def xml = XML.parse '<object><foo>not a short-object</foo></object>'

        when:
        extractor.extract(xml)

        then:
        ShortObjectExtractionException e = thrown()
        'not a short-object'             == e.params[0]
        400                              == e.getHttpStatusCode()
    }

    private String formatDate(Date d, String format) {
        new SimpleDateFormat(format).format(d)
    }
}
