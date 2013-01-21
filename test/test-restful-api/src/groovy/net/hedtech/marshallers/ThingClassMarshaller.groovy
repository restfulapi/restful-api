/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 

package net.hedtech.marshallers

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import grails.converters.JSON

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

import org.springframework.beans.BeanWrapper


/**
 * A JSON marshaller that supports HAL (http://stateless.co/hal_specification.html).
 **/
class ThingClassMarshaller extends BasicHalDomainClassMarshaller {


    protected static final Log log =
        LogFactory.getLog(ThingClassMarshaller.class)


    public ThingClassMarshaller(app) {
        super(app)
    }


    @Override 
    protected boolean processSpecificFields(BeanWrapper beanWrapper,
                                            GrailsDomainClassProperty property, JSON json) {
        Object referenceObject = beanWrapper.getPropertyValue(property.getName())

        // As an example, we'll add a 'numParts' property to the representation
        if ("parts" == property.getName()) {
            json.property("numParts", referenceObject.size());
        }
        return false // and we'll allow further processing of 'parts' by returning false
    }



}