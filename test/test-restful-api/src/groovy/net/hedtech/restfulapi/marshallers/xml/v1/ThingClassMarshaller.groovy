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
package net.hedtech.restfulapi.marshallers.xml.v1

import grails.converters.XML

import net.hedtech.restfulapi.Thing

import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.NameAwareMarshaller

class ThingClassMarshaller implements ObjectMarshaller<XML>, NameAwareMarshaller {

    public boolean supports(Object object) {
        return object instanceof Thing;
    }

    public void marshalObject(Object object, XML xml) throws ConverterException {
        Thing thing = (Thing) object
        if (thing.id)       xml.attribute( "id", thing.id.toString() )
        if (thing.version != null)   xml.attribute( "version", thing.getVersion().toString() )

        xml.startNode( "code" )
        xml.chars thing.getCode()
        xml.end()

        xml.startNode( "description" )
        xml.chars thing.getDescription()
        xml.end()

        xml.startNode( "parts" )
        thing.parts.each { part ->
            xml.startNode("part")
            xml.attribute("id", part.id.toString() )
            xml.end()
        }
        xml.end()
    }


    public String getElementName(Object o) {
        return "Thing";
    }

}
