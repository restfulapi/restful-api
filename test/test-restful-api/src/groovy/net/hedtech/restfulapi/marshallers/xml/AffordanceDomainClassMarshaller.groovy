/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.marshallers.xml

import grails.converters.XML
import grails.util.GrailsNameUtils

import net.hedtech.restfulapi.Inflector

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.web.converters.marshaller.NameAwareMarshaller;
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler as DCAH
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler;
import org.codehaus.groovy.grails.web.util.WebUtils
import org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.converters.marshaller.xml.*
import org.codehaus.groovy.grails.web.xml.XMLStreamWriter
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller

import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl



class AffordanceDomainClassMarshaller extends BasicDomainClassMarshaller {


    AffordanceDomainClassMarshaller() {
        super()
    }


    protected void processAdditionalFields(BeanWrapper beanWrapper, XML xml) {
        GrailsDomainClass domainClass = app.getDomainClass(beanWrapper.getWrappedInstance().getClass().getName())
        def resourceId = beanWrapper.getPropertyValue(domainClass.getIdentifier().getName())
        def resourceName = getDerivedResourceName(beanWrapper)
        xml.startNode('_href')
        xml.convertAnother("/$resourceName/$resourceId")
        xml.end()
    }
}
