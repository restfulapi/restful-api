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
import spock.lang.*
import grails.converters.XML
import grails.test.mixin.support.*
import grails.test.mixin.web.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.xml.*

@TestMixin([GrailsUnitTestMixin, ControllerUnitTestMixin])
class MapExtractorSpec extends Specification {

    def "Test basic extraction"() {
        setup:
        String data = """<?xml version="1.0" encoding="UTF-8"?><thing><text>text 'with single quote'</text><unicode>""" + """\u0c23""" + """</unicode><booleanTrue>true</booleanTrue><object><text>i'm an object</text></object><number1>123456789</number1><number4>123.4567</number4><number3>-0.1234</number3><number2>0.1234</number2><number6>1.34E52</number6><number5>1.34E-93</number5><booleanFalse>false</booleanFalse><horizontalTab>a""" + """\t""" + """b</horizontalTab><newLine>a
b</newLine><carriageReturn>a"""+"""\r"""+"""b</carriageReturn><anArray array="true"><arrayElement>i'm an array elt</arrayElement><arrayElement>4.5</arrayElement><arrayElement>1.34E52</arrayElement></anArray></thing>
        """
        def expected = [
            text:"text 'with single quote'",
            unicode:"\u0c23",
            booleanTrue:"true",
            object:[text:"i'm an object"],
            number1:"123456789",
            number4:"123.4567",
            number3:"-0.1234",
            number2:"0.1234",
            number6:"1.34E52",
            number5:"1.34E-93",
            booleanFalse:"false",
            horizontalTab:"a\tb",
            newLine:"a\nb",
            carriageReturn:"a\nb",
            anArray:["i'm an array elt","4.5","1.34E52"],
        ]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract( xml )

        then:
        expected == map
    }

    def "Test property that contains an object"() {
        setup:
        def data = """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customer>
                    <_link>/customers/1</_link>
            </customer>
        </thing>
        """

        def expected = [customer:[_link:'/customers/1']]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract( xml )

        then:
        expected == map
    }

    def "Test nested arrays"() {
        setup:
        def data = """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <anArray array="true">
                <elt array="true">
                    <elt>nested1</elt>
                    <elt>nested2</elt>
                </elt>
                <object>
                    <foo array="true">
                        <elt>bar</elt>
                        <elt>5.3</elt>
                    </foo>
                    <bar>bar text</bar>
                </object>
            </anArray>
        </thing>"""

        def expected = [
            anArray:[
                ['nested1','nested2'],
                [foo:['bar','5.3'], bar:'bar text']
            ]
        ]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract( xml )

        then:
        expected == map
    }

    def "Test empty array"() {
        setup:
        def data = """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <anArray array="true"/>
        </thing>"""

        def expected = [anArray:[]]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract( xml )

        then:
        expected == map
    }

    def "Test array that contains simple values"() {
        setup:
        def data = """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <anArray array="true">
                <string>abc</string>
                <number>123</number>
            </anArray>
        </thing>"""

        def expected = [anArray:['abc','123']]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract( xml )

        then:
        expected == map
    }

    def "Test array that contains objects"() {
        setup:
        def data = """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <anArray array="true">
                <person>
                    <name>smith</name>
                </person>
                <person>
                    <name>john</name>
                </person>
            </anArray>
        </thing>"""

        def expected = [
            anArray: [
                [name:'smith'],
                [name:'john']
            ]
        ]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract( xml )

        then:
        expected == map
    }

    def "Test array that contains empty objects"() {
        setup:
        def data = """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <anArray array="true">
                <person/>
                <person>
                    <name>john</name>
                </person>
            </anArray>
        </thing>"""

        def expected = [
            anArray: [
                "",
                [name:'john']
            ]
        ]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract( xml )

        then:
        expected == map

    }

    def "Test empty map"() {
        setup:
        def data = """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <aMap map="true"/>
        </thing>"""

        def expected = [aMap:[:]]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract( xml )

        then:
        expected == map
    }

    def "Test map in entry format"() {
        setup:
        def data="""<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true">
                <entry key="smith jones">
                    <lastName>Smith</lastName>
                    <firstName>John</firstName>
                </entry>
                <entry key="anderson">
                    <lastName>Anderson</lastName>
                    <firstName>Joe</firstName>
                </entry>
            </customers>
        </thing>"""

        def expected = [customers:[
            'smith jones':['lastName':'Smith','firstName':'John'],
            'anderson':['lastName':'Anderson','firstName':'Joe']
            ]
        ]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract(xml)

        then:
        expected == map
    }

    def "Test edge case in which a map contains an empty object"() {
        setup:
        def data="""<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true">
                <entry key="smith" map="true"/>
            </customers>
        </thing>"""

        def expected = [customers:[
            'smith':[:]
            ]
        ]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract(xml)

        then:
        expected == map
    }

    def "Test map that contains simple values"() {
        setup:
        def data="""<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true">
                <entry key="smith">abc</entry>
                <entry key="johnson">123</entry>
            </customers>
        </thing>"""

        def expected = [customers:[
            'smith':'abc',
            'johnson':'123'
            ]
        ]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract(xml)

        then:
        expected == map
    }

    def "Test map that contains objects"() {
        setup:
        def data="""<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true">
                <entry key="smith"><customerId>abc</customerId></entry>
                <entry key="johnson"><customerId>123</customerId></entry>
            </customers>
        </thing>"""

        def expected = [customers:[
            'smith':['customerId':'abc'],
            'johnson':['customerId':'123']
            ]
        ]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract(xml)

        then:
        expected == map
    }

    def "Test map that contains arrays"() {
        setup:
        def data="""<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true">
                <entry key="smith" array="true"><string>abc</string><number>123</number></entry>
            </customers>
        </thing>"""

        def expected = [customers:[
            'smith':['abc','123']
            ]
        ]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract(xml)

        then:
        expected == map
    }

    def "Test map that contains maps"() {
        setup:
        def data="""<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true">
                <entry key="smith" map="true"><entry key="foo">bar</entry></entry>
            </customers>
        </thing>"""

        def expected = [customers:[
            'smith':['foo':'bar']
            ]
        ]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract(xml)

        then:
        expected == map
    }

    def "Test null property"() {
        setup:
        def data="""<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customer null="true"/>
        </thing>"""

        def expected = [customer:null]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract(xml)

        then:
        expected == map
    }

    def "Test null map"() {
        setup:
        def data="""<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true" null="true"/>
        </thing>"""

        def expected = [customers:null]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract(xml)

        then:
        expected == map
    }

    def "Test null collection"() {
        setup:
        def data="""<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers array="true" null="true"/>
        </thing>"""

        def expected = [customers:null]

        when:
        def xml = XML.parse( data )
        def map = new MapExtractor().extract(xml)

        then:
        expected == map
    }

}
