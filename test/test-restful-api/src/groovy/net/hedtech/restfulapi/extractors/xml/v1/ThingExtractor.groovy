/* ****************************************************************************
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
