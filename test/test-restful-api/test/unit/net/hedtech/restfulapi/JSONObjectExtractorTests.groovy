/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

import net.hedtech.restfulapi.marshallers.xml.JSONObjectMarshaller
import net.hedtech.restfulapi.extractors.xml.JSONObjectExtractor
import grails.converters.*

import grails.test.mixin.*
import org.junit.*

class JSONObjectExtractorTests {

    void testBasic() {
        String data = """<?xml version="1.0" encoding="UTF-8"?><json><text>text 'with single quote'</text><unicode>""" + """\u0c23""" + """</unicode><booleanTrue>true</booleanTrue><object><text>i'm an object</text></object><aNull /><number1>123456789</number1><number4>123.4567</number4><number3>-0.1234</number3><number2>0.1234</number2><number6>1.34E52</number6><number5>1.34E-93</number5><booleanFalse>false</booleanFalse><horizontalTab>a""" + """\t""" + """b</horizontalTab><newLine>a
b</newLine><carriageReturn>a"""+"""\r"""+"""b</carriageReturn><anArray><net-hedtech-array><net-hedtech-arrayElement>i'm an array elt</net-hedtech-arrayElement><net-hedtech-arrayElement>4.5</net-hedtech-arrayElement><net-hedtech-arrayElement>1.34E52</net-hedtech-arrayElement></net-hedtech-array></anArray></json>
        """

        def xml = XML.parse( data )
        def json = new JSONObjectExtractor().extract( xml )

        assert "text 'with single quote'" == json.text
        assert "a\nb" == json.newLine
        //XML 1.0 processor rules will cause a carriage return to be parsed as a newline
        assert "a\nb" == json.carriageReturn
        assert "a\tb" == json.horizontalTab
        assert "\u0c23" == json.unicode
        assert "123456789" == json.number1
        assert "0.1234" == json.number2
        assert "-0.1234" == json.number3
        assert "123.4567" == json.number4
        assert "1.34E-93" == json.number5
        assert "1.34E52" == json.number6
        assert "true" == json.booleanTrue
        assert "false" == json.booleanFalse
        assert "i'm an object" == json.object.text
        assert "i'm an array elt" == json.anArray[0]
        assert "4.5" == json.anArray[1]
        assert "1.34E52" == json.anArray[2]
    }

    void testNested() {
        def data = """<?xml version="1.0" encoding="UTF-8"?>
        <json>
            <anArray>
                <net-hedtech-array>
                    <net-hedtech-arrayElement>
                        <net-hedtech-array>
                            <net-hedtech-arrayElement>nested1</net-hedtech-arrayElement>
                            <net-hedtech-arrayElement>nested2</net-hedtech-arrayElement>
                        </net-hedtech-array>
                    </net-hedtech-arrayElement>
                    <net-hedtech-arrayElement>
                        <foo>
                            <net-hedtech-array>
                                <net-hedtech-arrayElement>bar</net-hedtech-arrayElement>
                                <net-hedtech-arrayElement>5.3</net-hedtech-arrayElement>
                            </net-hedtech-array>
                        </foo>
                        <bar>bar text</bar>
                    </net-hedtech-arrayElement>
                </net-hedtech-array>
            </anArray>
        </json>"""
        /*
        String data = """<?xml version="1.0" encoding="UTF-8"?>
        <json>
            <anArray>
                <net-hedtech-array>
                    <net-hedtech-arrayElement>
                        <foo>
                            <text>some text for foo</text>
                            <bar>
                                <net-hedtech-array>
                                    <net-hedtech-arrayElement>
                                        <net-hedtech-array>
                                            <net-hedtech-arrayElement>nested array</net-hedtech-arrayElement>
                                        </net-hedtech-array>
                                    </net-hedtech-arrayElement>
                                    <net-hedtech-arrayElement>
                                        <foobar>
                                            <text>foobar foobar</text>
                                            <array>
                                                <net-hedtech-array>
                                                    <net-hedtech-arrayElement>7</net-hedtech-arrayElement>
                                                    <net-hedtech-arrayElement>13</net-hedtech-arrayElement>
                                                </net-hedtech-array>
                                            </array>
                                        </foobar>
                                    </net-hedtech-arrayElement>
                                </net-hedtech-array>
                                <barText>barText with some text</barText>
                            </bar>
                        </foo>
                    </net-hedtech-arrayElement>
                </net-hedtech-array>
            </anArray>
            <foo>
                <text>I am the outer foo</text>
            </foo>
        </json>
        """*/

        def xml = XML.parse( data )
        def json = new JSONObjectExtractor().extract( xml )
        assert "nested1" == json.anArray[0][0]
        assert "nested2" == json.anArray[0][1]
        assert "bar" == json.anArray[1].foo[0]
        assert "5.3" == json.anArray[1].foo[1]
        assert "bar text" == json.anArray[1].bar

/*
        assert "I am the outer foo" == json.foo.text
        assert "some text for foo" == json.anArray[0].foo.text
        println json.anArray[0].foo.bar
        println "______________________________"
        assert "nested array" == json.anArray[0].foo.bar[0][0]*/


    }

}