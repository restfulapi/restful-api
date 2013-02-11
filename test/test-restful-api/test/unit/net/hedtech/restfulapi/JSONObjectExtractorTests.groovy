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

    void testME() {
        def namedConfig = this.getClass().getName() + "_textJSONAsXMLExtraction"
        XML.createNamedConfig( namedConfig  ) {
            it.registerObjectMarshaller(new JSONObjectMarshaller(), 999)
        }
        String data = """<?xml version="1.0" encoding="UTF-8"?><json><text>text 'with single quote'</text><unicode>""" + """\u0c23""" + """</unicode><booleanTrue>true</booleanTrue><object><text>i'm an object</text></object><aNull /><number1>123456789</number1><number4>123.4567</number4><number3>-0.1234</number3><number2>0.1234</number2><number6>1.34E52</number6><number5>1.34E-93</number5><booleanFalse>false</booleanFalse><horizontalTab>a""" + """\t""" + """b</horizontalTab><newLine>a
b</newLine><carriageReturn>a"""+"""\r"""+"""b</carriageReturn><anArray><array><arrayElement>i'm an array elt</arrayElement><arrayElement>4.5</arrayElement><arrayElement>1.34E52</arrayElement></array></anArray></json>
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

}