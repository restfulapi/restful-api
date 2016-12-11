/* ****************************************************************************
 * Copyright 2013-2016 Ellucian Company L.P. and its affiliates.
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

import groovy.util.slurpersupport.GPathResult

import net.hedtech.restfulapi.ContentFilterHolder
import net.hedtech.restfulapi.extractors.ProtectedFieldException
import net.hedtech.restfulapi.extractors.XMLExtractor

/**
 * Extracts content from xml into a map structure.
 **/
class MapExtractor implements XMLExtractor {

    Map extract(GPathResult xml) {
        def content = extractInternal(xml)
        if (content) {
            def contentFilterHolder = ContentFilterHolder.get()
            if (contentFilterHolder) {
                def contentFilter = contentFilterHolder.contentFilter
                def result = contentFilter.applyFilter(
                        contentFilterHolder.resourceName,
                        content,
                        contentFilterHolder.contentType)
                if (result.isPartial) {
                    if (contentFilter.allowPartialRequest) {
                        content = result.content
                    } else {
                        throw new ProtectedFieldException(contentFilterHolder.resourceName)
                    }
                }
            }
        }
        return content
    }

    protected def extractInternal(GPathResult xml) {
        if (xml.@null.text() == 'true') {
            return null
        } else if (xml.@array.text() == 'true') {
            //have an array.
            //all children are elements of the array.
            //they may contain complex content themselves
            def array = []
            xml.children().each() {
                array.add( extractInternal( it ) )
            }
            return array
        } else if (xml.@map.text() == 'true') {
            extractMap(xml)
        } else if (xml.children().size() == 0) {
            return xml.text()
        } else {
            extractMap(xml)
        }
    }

    protected Map extractMap(GPathResult xml) {
        //treat as map, children are name/value pairs
        def map = [:]
        //we have two representations for maps.
        //The first is 'object-style', where the children node-names
        //represent key names, and their content is values.  This is
        //used for representations of objects.
        //The other is a map where the keys may contain strings with spaces
        //and the children are <entry> nodes.
        if (xml.entry.size() > 0) {
            xml.entry.each() { def entry ->
                map.put(entry.@key.text(), extractInternal(entry))
            }
        } else {
            xml.children().each() {
                map.put(it.name(), extractInternal(it))
            }
        }
        return map
    }
}
