/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 
package net.hedtech.restfulapi.marshallers

import grails.converters.JSON
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
import org.codehaus.groovy.grails.web.converters.marshaller.json.*
import org.codehaus.groovy.grails.web.json.JSONWriter
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.ConverterUtil

import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl



/**
 * A default domain class marshaller that provides support for versioning.
 **/
class BasicDomainClassMarshaller extends DomainClassMarshaller {

    protected static final Log log =
        LogFactory.getLog(BasicDomainClassMarshaller.class)

    protected ProxyHandler proxyHandler
    protected GrailsApplication app
 
    private static List SKIPPED_FIELDS = Arrays.asList('lastModified', 'lastModifiedBy',
                                                       'createdBy', 'password')
 

// ------------------------------- Constructors -------------------------------


    public BasicDomainClassMarshaller(app) {
        this(true, new HibernateProxyHandler(), app)
    }


    public BasicDomainClassMarshaller(boolean includeVersion, ProxyHandler proxyHandler, app) {
        super(includeVersion, proxyHandler, app)
        this.proxyHandler = proxyHandler
        this.app = app
    }


// ---------------------- DomainClassMarshaller methods -----------------------


// Seeded from: http://grails4you.com/2012/04/restful-api-for-grails-domains/
// TODO: Refactor -- very long marshalObject method with nested if statements...

    @Override
    public void marshalObject(Object value, JSON json) throws ConverterException {

        Class<?> clazz = value.getClass()
        log.trace ">>>>>>>>>>>>>>>>>>>>>>>>>> $this marshalObject() called for $clazz"
        JSONWriter writer = json.getWriter()
        value = proxyHandler.unwrapIfProxy(value)
        GrailsDomainClass domainClass = app.getDomainClass(clazz.getName())
        BeanWrapper beanWrapper = new BeanWrapperImpl(value)
 
        writer.object()
        GrailsDomainClassProperty[] properties = domainClass.getPersistentProperties()
 
        if (needToDefineId()) {
            def id
            if (domainClass.hasProperty(getObjectIdentifier())) {
                id = extractValue(value, domainClass.getPropertyByName(getObjectIdentifier()))
            } else {
                id = extractValue(value, domainClass.getPropertyByName("id"))
            }
            json.property("id", id)
        }

        if (isIncludeVersion()) {
            GrailsDomainClassProperty versionProperty = domainClass.getVersion();
            Object version = extractValue(value, versionProperty);
            json.property("version", version);
        }        

        // Add the 'href' link to 'self'
        writer.key("_href").value(getResourceUri(clazz.simpleName, value.id))
 
        processAdditionalFields(beanWrapper, json)
 
        List excludedFields = SKIPPED_FIELDS + getSkippedFields()
 
        for (GrailsDomainClassProperty property: properties) {

            if (!excludedFields.contains(property.getName())) {
 
                if (!property.isAssociation() 
                        && !processSimpleField(beanWrapper, property, json)) {

                    writeFieldName(writer, property)

                    // Write non-relation property
                    Object val = beanWrapper.getPropertyValue(property.getName())
                  
                    if (val instanceof String && !val) {
                        val = null
                    } else if (property.getType() == Boolean && val == null) {
                        val = false
                    }
 
                    json.convertAnother(val)
                } 
                else {
                    Object referenceObject = beanWrapper.getPropertyValue(property.getName())
                    GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass()
                    if (processSpecificFields(beanWrapper, property, json)) {
                        //do nothing
                    } else if (referencedDomainClass == null || property.isEmbedded() || GrailsClassUtils.isJdk5Enum(property.getType()) ||
                            property.isOneToOne() || property.isManyToOne() || property.isEmbedded()) {
                        writeFieldName(writer, property)
                        json.convertAnother(proxyHandler.unwrapIfProxy(referenceObject))
                    } else {
                        writeFieldName(writer, property)
                        GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier()

                        @SuppressWarnings("unused")
                        String refPropertyName = referencedDomainClass.getPropertyName()

                        if (referenceObject instanceof Collection) {
                            Collection o = (Collection) referenceObject
                            writer.array()

                            for (Object el: o) {
                                asShortObject(el, json, referencedIdProperty, referencedDomainClass)
                            }
                            writer.endArray()
                        } 
                        else if (referenceObject instanceof Map) {
                            Map<Object, Object> map = (Map<Object, Object>) referenceObject
                          
                            for (Map.Entry<Object, Object> entry: map.entrySet()) {
                                String key = String.valueOf(entry.getKey())
                              
                                Object o = entry.getValue()
                                writer.object()
                                writer.key(key)
                                asShortObject(o, json, referencedIdProperty, referencedDomainClass)
                                writer.endObject()
                            }
                        }
                    }
                }
            }
        }
        writer.endObject()
    }
 

    @Override
    public boolean supports(Object object) {
        def response = WebUtils.retrieveGrailsWebRequest().getCurrentResponse()
        log.trace "\n$this supports(${object.getClass()}) invoked (format is ${response.format})"

        def result = DCAH.isDomainClass(object.getClass()) && (response.format == 'jsonv0' || response.format == 'json')
        log.trace "$this supports(${object.getClass()}) returning $result"
        log.debug "   (Based on: isDomainClass=${app.isDomainClass(object.getClass())} \
                       && response.format=${response.format})"
        result
    }


    @Override
    public boolean isRenderDomainClassRelations() {
        true
    }


    // TODO: Implement changes to how associated domain objects are rendered
    @Override
    protected void asShortObject(java.lang.Object refObj, JSON json, 
                                 GrailsDomainClassProperty idProperty, 
                                 GrailsDomainClass referencedDomainClass) {

        super.asShortObject refObj, json, idProperty, referencedDomainClass
    }
 

// ------------------- Additional Methods, Helper Methods ---------------------


    // TODO: Determine whether to use IRI or full absolute URI


    protected String getResourceUri(String simpleDomainClassName, id) {
        def domainName = GrailsNameUtils.getPropertyName(simpleDomainClassName)
        def resourceName = hyphenate(pluralize(domainName))
        "/${resourceName}/${id}"
    }


    /**
     * Returns the URI for the resource.
     * This method should be called from a 'asShortObject' method implementation  
     * (it uses a subset of the argments from that method to facilitate usage).
     **/
    protected String getResourceUri(java.lang.Object refObj, 
                                    GrailsDomainClassProperty idProperty,
                                    GrailsDomainClass referencedDomainClass) {

        def domainName = GrailsNameUtils.getPropertyName(referencedDomainClass.shortName)
        def resourceName = hyphenate(pluralize(domainName))
        "/${resourceName}/${extractValue(refObj, idProperty)}"
    }


    protected String getBaseUrl(String val) {
        if (val) {
            if (val.startsWith("http://") || val.startsWith("https://")
                    || val.startsWith("ftp://") || val.startsWith("file://")) {
                return val
            } else {
                return ConfigurationHolder.config.grails.contentURL + "/" + val
            }
        }
        null
    }
 

    protected def writeFieldName(JSONWriter writer, GrailsDomainClassProperty property) {
        def propertyName = property.getName()
        if (getAlternativeName(propertyName)) {
            propertyName = getAlternativeName(propertyName)
        }
        writer.key(propertyName)
    }
 

    protected String getAlternativeName(String originalName) {
        null
    }


    protected List getSkippedFields() {
        []
    }
 

    protected List getCommonSkippedFields() {
        SKIPPED_FIELDS
    }
 

    protected boolean processSpecificFields(BeanWrapper beanWrapper,
                                            GrailsDomainClassProperty property, JSON json) {
        false
    }
 

    protected boolean processSimpleField(BeanWrapper beanWrapper,
                                         GrailsDomainClassProperty property, 
                                         JSON json) {
        false
    }
 

    protected void processAdditionalFields(BeanWrapper beanWrapper, JSON json) {
    }
 

    protected String getObjectIdentifier() {
        'id'
    }
 

    protected boolean needToDefineId() {
        true 
    }


    private String pluralize(String str) {
        Inflector.pluralize(str)
    }


    private String hyphenate(String str) {
        Inflector.hyphenate(str)
    }

}