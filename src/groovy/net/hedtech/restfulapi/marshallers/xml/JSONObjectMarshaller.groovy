/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.marshallers.xml

import grails.converters.XML

import org.codehaus.groovy.grails.web.converters.marshaller.xml.*
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONArray

/**
 * Marshalls a JSONObject to xml.
 * Allows JSON content to be represented in a fixed XML format.
 **/
class JSONObjectMarshaller extends CollectionMarshaller {

    public boolean supports(Object object) {
        return object instanceof JSONObject;
    }

    public void marshalObject(Object object, XML xml) throws ConverterException {
        JSONObject json = (JSONObject) object;
        build( xml, null, json )
    }


    public String getElementName(Object o) {
        return "net-hedtech-object";
    }

    private void build( XML xml, def current, def value ) {
        if (current) {
            xml.startNode( current )
        }
        if (value instanceof JSONObject) {
            value.keySet().each {
                def nextValue = value.get( it )
                build( xml, it, nextValue )
            }
        } else if (value instanceof JSONArray) {
            xml.startNode('net-hedtech-array')
            value.toArray().each() { arrayElement ->
                build( xml, 'net-hedtech-arrayElement', arrayElement )
            }
            xml.end()
        } else if (value == JSONObject.NULL) {
            //do nothing, NULL == empty element
        } else if (value instanceof Boolean) {
            xml.chars JSONObject.valueToString( value )
        } else if (value instanceof String) {
            xml.chars value
        } else if (value instanceof Number) {
            xml.chars JSONObject.valueToString( value )
        } else {
            xml.chars value.toString()
        }
        if (current) {
            xml.end()
        }
    }


}