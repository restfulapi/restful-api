/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.extractors.xml

import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONArray

import groovy.util.slurpersupport.GPathResult

import net.hedtech.restfulapi.extractors.XMLExtractor

/**
 * Extracts content from xml into a map structure.
 **/
class MapExtractor implements XMLExtractor {

    Map extract( GPathResult xml ) {
        return extractInternal( xml )

    }

    private def extractInternal( GPathResult xml ) {
        if ('true' == xml.'@array'.text()) {
            //this element represents an array
            def array = []
            xml.children().each {
                array.add extractInternal(it)
            }
            return array
        } else if ('true' == xml.'@map'.text()) {
            //this element represents a map
            //expect sub-elements to be <entry key='name'>value</entry>
            def map = [:]
            xml.entry.each {
                def key = it.@key.text()
                def val
                if (it.children().size() == 0) {
                    //simple value
                    val = it.text()
                } else if (it.children().size() == 1) {
                    val = extractInternal(it.children().getAt(0))
                    //if an entry has a child, then it must be either an array
                    //or an object.
                    //if it is a simple value, then it will be in the text of the entry
                    //directly, and therefore the node has zero children.
                    //so if the val is an empty string or null, it mean an empty object
                    if (val == null || val == '') {
                        val = [:]
                    }
                }
                map.put(key, val)
            }
            return map
        } else if (0 == xml.children().size()) {
            //Element with no children represents a final (text) value
            return xml.text()
        } else {
            //a node without an explicit map or array attribute that has children
            //is by default an object, where each child represents a field
            //so extract as a map
            Map map = [:]
            xml.children().each() {
                map.put( it.name(), extractInternal( it ) )
            }
            return map
        }
    }

    private boolean isArray(GPathResult xml) {
        if (xml.children().size() == 0) return false
        Set names = new HashSet()
        xml.children.each() {
            names.add it.getName()
        }
        return names.size() == 1
    }

}