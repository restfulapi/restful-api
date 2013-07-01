/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.marshallers.xml

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import grails.converters.XML
import groovy.lang.GroovyObject

import java.beans.PropertyDescriptor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.support.proxy.ProxyHandler

import org.springframework.beans.BeanWrapper

class DeclarativeGroovyBeanMarshaller extends GroovyBeanMarshaller {

    protected static final Log log =
        LogFactory.getLog(DeclarativeGroovyBeanMarshaller.class)

    Class supportClass
    def fieldNames = [:]
    def excludedFields = []
    def includedFields = []
    def additionalFieldClosures = []
    def additionalFieldsMap = [:]

    @Override
    public boolean supports(Object object) {
        if (supportClass) {
            supportClass.isInstance(object)
        } else {
            super.supports(object)
        }
    }


    @Override
    protected List<String> getExcludedFields(Object value) {
        return excludedFields
    }

    @Override
    protected List<String> getIncludedFields(Object value) {
        return includedFields
    }

    @Override
    protected String getSubstitutionName(BeanWrapper beanWrapper, PropertyDescriptor property) {
        return fieldNames.get( property.getName() )
    }

    @Override
    protected String getSubstitutionName(Object value, Field field) {
        return fieldNames.get( field.getName() )
    }

    @Override
    protected void processAdditionalFields(BeanWrapper beanWrapper, XML xml) {
        Map map = [:]
        map.putAll( additionalFieldsMap )
        map.putAll(
            [
                'grailsApplication':app,
                'beanWrapper':beanWrapper,
                'xml':xml
            ]
        )
        if (!map['resourceName']) {
            map['resourceName'] = getDerivedResourceName(beanWrapper)
        }
        //see if we have something that looks like an id property or field
        //metaprogrammed
        def id = null
        def val = beanWrapper.getWrappedInstance()
        if (val != null) {
            if (val.metaClass && val.metaClass.hasProperty(val, 'id')) {
                id = val.'id'
            } else if (val.metaClass && val.metaClass.respondsTo(val, 'getId', new Object[0])) {
                id = val.getId()
            } else if (getAvailableProperties(beanWrapper)*.name.contains('id')) {
                id = beanWrapper.getPropertyValue('id')
            } else if (getAvailableFields(beanWrapper.getWrappedInstance())*.name.contains('id')) {
                id = val.id
            }
            if (id != null) {
                map['resourceId'] = id
            }
        }
        //map['resourceId'] = beanWrapper.getPropertyValue(domainClass.getIdentifier().getName())
        additionalFieldClosures.each { c ->
            c.call( map )
        }
    }

}
