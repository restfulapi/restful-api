/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.extractors.xml

import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONArray

import groovy.util.slurpersupport.GPathResult

import net.hedtech.restfulapi.extractors.XMLExtractor

/**
 * Extracts content from json-as-xml by
 * extracting the json format, then
 **/
class JSONObjectExtractor implements XMLExtractor {

    Map extract( GPathResult xml ) {
        return extractInternal( xml )

    }

    private def extractInternal( GPathResult xml ) {
        //Element with no children represents a final value
        if (xml.children().size() == 0) {
            if (xml.text() == null || xml.text() == "") {
                return JSONObject.NULL
            } else {
                return xml.text()
            }
        } else if (xml.children().size() == 1 && xml.children()[0].name() == 'net-hedtech-array') {
            //have an array.
            //all children will be named 'ArrayElement',
            //and may themelves have complex content.
            JSONArray array = new JSONArray()
            xml.children()[0].children().each() {
                array.add( extractInternal( it ) )
            }
            return array
        } else {
            //have a JSON entity.
            JSONObject json = new JSONObject()
            xml.children().each() {
                json.put( it.name(), extractInternal( it ) )
            }
            return json
        }
    }

}