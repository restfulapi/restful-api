/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi

import net.hedtech.restfulapi.marshallers.MarshallingService
import groovy.xml.MarkupBuilder
import net.hedtech.restfulapi.config.RepresentationConfig

/**
 * A demonstration class for a custom marshalling service,
 * using a framework other than the grails converters to marshall
 * objects.
 */
class CustomThingMarshallingService implements MarshallingService {

    @Override
    String marshalObject(Object o, RepresentationConfig config) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        if (o instanceof Collection) {
            Collection list = (Collection) o
            xml.list() {
                list.each {
                    code(it.code)
                }
            }
        } else {
            xml.thing() {
                code(o.code)
            }
        }
        return writer.toString()
    }
}