/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
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
