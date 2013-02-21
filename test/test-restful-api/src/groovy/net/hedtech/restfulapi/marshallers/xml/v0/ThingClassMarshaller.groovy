/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.marshallers.xml.v0

import grails.converters.XML
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.codehaus.groovy.grails.web.converters.marshaller.NameAwareMarshaller
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException

import net.hedtech.restfulapi.Thing

class ThingClassMarshaller implements ObjectMarshaller<XML>, NameAwareMarshaller {

    public boolean supports(Object object) {
        return object instanceof Thing;
    }

    public void marshalObject(Object object, XML xml) throws ConverterException {
        Thing thing = (Thing) object
        if (thing.id)       xml.attribute( "id", thing.id.toString() )
        if (thing.version)   xml.attribute( "version", thing.version.toString() )

        xml.startNode( "code" )
        xml.chars thing.getCode()
        xml.end()

        xml.startNode( "description" )
        xml.chars thing.getDescription()
        xml.end()
    }


    public String getElementName(Object o) {
        return "Thing";
    }

}