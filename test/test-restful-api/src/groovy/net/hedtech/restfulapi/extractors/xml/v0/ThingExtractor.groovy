/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors.xml.v0

import groovy.util.slurpersupport.GPathResult

import net.hedtech.restfulapi.extractors.XMLExtractor
class ThingExtractor implements XMLExtractor {

    Map extract( GPathResult content ) {
        def map = [:]

        if (content.code?.text())   map['code']             = content.code?.text()
        if (content.description?.text()) map['description'] = content.description?.text()

        return map
    }
}