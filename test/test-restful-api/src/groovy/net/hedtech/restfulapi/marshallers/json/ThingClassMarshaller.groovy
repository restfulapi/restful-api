/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.marshallers.json

import grails.converters.JSON

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

import org.springframework.beans.BeanWrapper


/**
 * A JSON marshaller for the Thing domain class.
 **/
class ThingClassMarshaller extends BasicDomainClassMarshaller {


    protected static final Log log =
        LogFactory.getLog(ThingClassMarshaller.class)


    public ThingClassMarshaller(app) {
        super()
        setApp( app )
    }


    @Override
    protected boolean processField(BeanWrapper beanWrapper,
                                            GrailsDomainClassProperty property, JSON json) {
        Object referenceObject = beanWrapper.getPropertyValue(property.getName())

        // As an example, we'll add a 'numParts' property to the representation
        if ("parts" == property.getName()) {
            json.property("numParts", referenceObject.size());
        }
        return true // and we'll allow the superclass to process parts normally
    }

    @Override
    protected void processAdditionalFields(BeanWrapper beanWrapper, JSON json) {
        super.processAdditionalFields(beanWrapper, json)
        json.property("sha1", beanWrapper.getWrappedInstance().getSupplementalRestProperties()['sha1'])
    }



}
