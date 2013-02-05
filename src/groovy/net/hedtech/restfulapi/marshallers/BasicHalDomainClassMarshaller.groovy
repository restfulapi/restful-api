/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.marshallers

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


    public BasicHalDomainClassMarshaller(app) {
        super(true, new HibernateProxyHandler(), app)
    }


    // @Override
    // public void marshalObject(Object value, JSON json) throws ConverterException {

    //     log.trace ">>>>>>>>>>>>>>>>>>>>>>>>>> $this marshalObject() called for $clazz"
    //     super.marshalObject(value, json)
    // }


    @Override
    protected List getSkippedFields() { }


    @Override
    protected String getAlternativeName(String originalName){
        return null
    }


    @Override
    protected boolean processSpecificFields(BeanWrapper beanWrapper,
                                            GrailsDomainClassProperty property, JSON json) {
        // Object referenceObject = beanWrapper.getPropertyValue(property.getName())
        false // continue to process these fields
    }


    @Override
    protected void processAdditionalFields(BeanWrapper beanWrapper, JSON json) {
    }


    @Override
    public boolean supports(Object object) {
        super.supports(object)
        // Note: To 'support' a particular domain class, use something like:
        // proxyHandler.unwrapIfProxy(object).getClass().isAssignableFrom(Xyz.class)
    }


    @Override
    protected void asShortObject(java.lang.Object refObj, JSON json,
                                 GrailsDomainClassProperty idProperty,
                                 GrailsDomainClass referencedDomainClass) {

        log.trace "asShortObject is processing ${referencedDomainClass.shortName}"
        def domainObj = proxyHandler.unwrapIfProxy(refObj)

        if (referencedDomainClass.shortName == 'PartOfThing') {
            json.writer.object()
            json.writer.key("class").value(referencedDomainClass.name)
            json.writer.key("id").value(extractValue(refObj, idProperty))

            // TODO: Use format like "_links": { "self": { "href":"xxx", "rel":"xxx" } }
            json.writer.key("_href").value(getResourceUri(refObj, idProperty, referencedDomainClass))
            json.writer.endObject()

        }
    }

}