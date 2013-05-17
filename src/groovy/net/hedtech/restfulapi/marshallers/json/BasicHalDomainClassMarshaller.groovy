/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.marshallers.json

import grails.converters.JSON
import grails.util.GrailsNameUtils

import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException

import org.springframework.beans.BeanWrapper


/**
 * A JSON marshaller that supports HAL (http://stateless.co/hal_specification.html).
 **/
class BasicHalDomainClassMarshaller extends BasicDomainClassMarshaller {



    // @Override
    // public void marshalObject(Object value, JSON json) throws ConverterException {

    //     log.trace ">>>>>>>>>>>>>>>>>>>>>>>>>> $this marshalObject() called for $clazz"
    //     super.marshalObject(value, json)
    // }



    @Override
    protected boolean processField(BeanWrapper beanWrapper,
                                            GrailsDomainClassProperty property, JSON json) {
        // Object referenceObject = beanWrapper.getPropertyValue(property.getName())
        true // continue to process these fields
    }


    @Override
    protected void processAdditionalFields(BeanWrapper beanWrapper, JSON json) {
        GrailsDomainClass domainClass = app.getDomainClass(beanWrapper.getWrappedInstance().getClass().getName())
        def id = beanWrapper.getPropertyValue(domainClass.getIdentifier().getName())
        def resourceName = getDerivedResourceName(beanWrapper)
        json.property("_href", "/${resourceName}/${id}")
    }


    @Override
    public boolean supports(Object object) {
        super.supports(object)
        // Note: To 'support' a particular domain class, use something like:
        // proxyHandler.unwrapIfProxy(object).getClass().isAssignableFrom(Xyz.class)
    }

}