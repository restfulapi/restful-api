/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.marshallers.xml

import grails.converters.XML
import grails.util.GrailsNameUtils

import net.hedtech.restfulapi.Inflector

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler as DCAH
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.web.util.WebUtils
import org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.converters.marshaller.xml.*
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONArray

import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl

/**
 * Marshalls a JSONObject to xml.
 * Allows JSON content to be represented in a fixed XML format.
 **/
class JSONObjectMarshaller extends CollectionMarshaller {

    public boolean supports(Object object) {
        return object instanceof JSONObject;
    }

    public void marshalObject(Object object, XML xml) throws ConverterException {
        JSONObject json = (JSONObject) object;
        build( xml, null, json )
    }


    public String getElementName(Object o) {
        return "json";
    }

    private void build( XML xml, def current, def value ) {
        if (current) {
            xml.startNode( current )
        }
        if (value instanceof JSONObject) {
            value.keySet().each {
                def nextValue = value.get( it )
                build( xml, it, nextValue )
            }
        } else if (value instanceof JSONArray) {
            xml.startNode('array')
            value.toArray().each() { arrayElement ->
                build( xml, 'arrayElement', arrayElement )
            }
            xml.end()
        } else if (value == JSONObject.NULL) {
            //do nothing, NULL == empty element
        } else if (value instanceof Boolean) {
            xml.chars value ? 'true' : 'false'
        } else {
            //number or string
            xml.chars value.toString()
        }
        if (current) {
            xml.end()
        }
    }


}