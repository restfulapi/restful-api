/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors.xml.v1

import groovy.util.slurpersupport.GPathResult

import net.hedtech.restfulapi.extractors.XMLExtractor
class ThingExtractor implements XMLExtractor {

    Map extract( GPathResult content ) {
        def map = [:]

        if (content.code?.text())   map['code']             = content.code?.text()
        if (content.description?.text()) map['description'] = content.description?.text()
        if (content.parts?.part?.size() > 0) {
            map['parts'] = []
            content.parts[0].part.each { part ->
                map['parts'].add( [ code: part.code?.text(), description: part.description?.text() ] )
            }
        }
        return map
    }
}