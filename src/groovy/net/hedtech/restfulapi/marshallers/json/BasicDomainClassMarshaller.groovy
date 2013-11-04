/* ****************************************************************************
 * Copyright 2013 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package net.hedtech.restfulapi.marshallers.json

import grails.converters.JSON
import grails.util.GrailsNameUtils

import net.hedtech.restfulapi.Inflector
import net.hedtech.restfulapi.marshallers.MissingFieldsException

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler as DCAH
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler
import org.codehaus.groovy.grails.web.util.WebUtils
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.converters.marshaller.json.*
import org.codehaus.groovy.grails.web.json.JSONWriter
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller

import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.factory.NoSuchBeanDefinitionException


/**
 * A default domain class marshaller.
 * By default, it will marshall all fields not in the default exclusion list.
 * Objects in associations will be rendered as 'short objects' (class and id).
 * Supports any grails domain class.
 * The class can be extended to override how an object is marshalled.
 **/
class BasicDomainClassMarshaller implements ObjectMarshaller<JSON> {

    protected static final Log log =
        LogFactory.getLog(BasicDomainClassMarshaller.class)

    GrailsApplication app
    //allow proxy handler to be explicitly set
    //this field should never be used directly,
    //use getProxyHandler() instead
    ProxyHandler proxyHandler


    private static List EXCLUDED_FIELDS = Arrays.asList('lastModified', 'lastModifiedBy',
                                                        'dataOrigin', 'createdBy', 'password')


// ------------------------------- Constructors -------------------------------


    BasicDomainClassMarshaller() {
    }


// ---------------------- DomainClassMarshaller methods -----------------------


// Seeded from: http://grails4you.com/2012/04/restful-api-for-grails-domains/

    @Override
    public void marshalObject(Object value, JSON json) throws ConverterException {

        Class<?> clazz = value.getClass()
        log.trace "$this marshalObject() called for $clazz"
        JSONWriter writer = json.getWriter()
        value = getProxyHandler().unwrapIfProxy(value)
        GrailsDomainClass domainClass = app.getDomainClass(clazz.getName())
        BeanWrapper beanWrapper = new BeanWrapperImpl(value)
        GrailsDomainClassProperty[] persistentProperties = domainClass.getPersistentProperties()

        writer.object()

        if (includeIdFor(value)) {
            def id = extractValue(value, domainClass.getIdentifier())
            json.property("id", id)
        }

        if (includeVersionFor(value)) {
            GrailsDomainClassProperty versionProperty = domainClass.getVersion()
            Object version = extractValue(value, versionProperty)
            json.property("version", version)
        }

        processAdditionalFields(beanWrapper, json)

        def propertiesToMarshall
        List includedFields = getIncludedFields( value )
        if (includedFields != null) {
            //use inclusion list
            propertiesToMarshall = persistentProperties.findAll {
                includedFields.contains( it.getName() )
            }
            if (requireIncludedFields( value )) {
                if (!(propertiesToMarshall*.name).equals(includedFields)) {
                    def missing = []
                    missing.addAll includedFields
                    missing.removeAll(propertiesToMarshall*.name)
                    throw new MissingFieldsException( missing )
                }
            }
        } else {
            //use exclusion list
            List excludedFields = getCommonExcludedFields() + getExcludedFields( value )
            propertiesToMarshall = persistentProperties.findAll {
                !excludedFields.contains( it.getName() )
            }
        }

        for (GrailsDomainClassProperty property: propertiesToMarshall) {
            log.trace( "$this marshalObject() handling field '${property.getName()}' for $clazz")
            if (processField(beanWrapper, property, json)) {
                if (property.isAssociation()) {
                    marshallAssociationField(beanWrapper, property, json)
                } else {
                    marshallSimpleField(beanWrapper,property,json)
                }
            } else {
                log.trace( "$this marshalObject() handled field '${property.getName()}' for $clazz in processField()")
            }
        }
        writer.endObject()
    }

// ------------------- Methods to override to customize behavior ---------------------

    @Override
    public boolean supports(Object object) {
        DCAH.isDomainClass(object.getClass())
    }

    /**
     * Return the name to use when marshalling the field, or
     * null if the field name should be used as-is.
     * @return the name to use when marshalling the field,
     *         or null if the domain field name should be used
     */
    protected String getSubstitutionName(BeanWrapper beanWrapper,GrailsDomainClassProperty property) {
        null
    }

    /**
     * Returns the list of fields that should be marshalled
     * for the specified object.
     *<p>
     * If a null is returned, then
     * all fields except those specified by
     * {@link #getSkippedField(Object) getSkippedFields} and
     * {@link #getCommonSkippedFields} will be marshalled.
     * If a non-null list is returned, then only
     * the fields listed in it are marshalled.  Included fields
     * overrides any skipped fields.  That is, if a non-null list is returned
     * by {@link getIncludedFields(Object) #getIncludedFields} then
     * {@link #getSkippedField(Object) getSkippedFields} and
     * {@link #getCommonSkippedFields} are ignored.
     *
     * @return list of field names to marshall
     */
    protected List<String> getIncludedFields(Object value) {
        null
    }


    /**
     * Override whether or not to treat an includes
     * list in a strict fashion or not.  If true then
     * an included field that is not present
     * results in a ConverterException.
     **/
    protected boolean requireIncludedFields(Object o) {
        return false
    }


    /**
     * Returns a list of additional fields in the
     * object that should not be marshalled.
     * The complete list of skipped fields is the
     * union of getCommonSkippedFields() and
     * the list returned by this method.
     * Does not apply if {@link #getIncludedFields(Object) getIncludedFields} returns
     * a non-null list of fields to include.
     *
     * @param value the object being marshalled
     * @return list of fields that should be skipped
     */
    protected List<String> getExcludedFields(Object value) {
        []
    }


    /**
     * Fields that are always skipped.
     * Does not apply if {@link #getIncludedFields() getIncludedFields}
     * returns a list containing one or more field names.
     * @return list of fields that should be skipped in all
     *          objects this marshaller supports
     */
    protected List<String> getCommonExcludedFields() {
        EXCLUDED_FIELDS
    }


    /**
     * Override processing of fields.
     * @return true if the marshaller should handle the field in
     *         the default way, false if no further action should
     *         be taken for the field.
     *
     **/
    protected boolean processField(BeanWrapper beanWrapper,
                                   GrailsDomainClassProperty property,
                                   JSON json) {
        true
    }


    protected void processAdditionalFields(BeanWrapper beanWrapper, JSON json) {
    }

    /**
     * Override whether to include an 'id' field
     * for the specified value.
     * @param o the value
     * @return true if an 'id' field should be placed in the
     *         representation
     **/
    protected boolean includeIdFor(Object o) {
        return true
    }

    /**
     * Override whether to include a 'version' field
     * for the specified value.
     * @param o the value
     * @return true if a 'version' field should be placed in the
     *         representation
     **/
    protected boolean includeVersionFor(Object o) {
        return true
    }

    /**
     * Specify whether to deep marshal a field representing
     * an association, or render it as a short-object.
     * @param beanWrapper the wrapped object containing the field
     * @param property the property to be marshalled
     * @return true if the value of the field should be deep rendered,
     *         false if a short-object representation should be used.
     **/
    protected boolean deepMarshallAssociation(BeanWrapper beanWrapper, GrailsDomainClassProperty property) {
        return false
    }


// ------------------- End methods to override to customize behavior ---------------------

// ------------------- Methods to support marshalling ---------------------

    protected def writeFieldName(BeanWrapper beanWrapper,
                                 GrailsDomainClassProperty property,
                                 JSON json) {
        JSONWriter writer = json.getWriter()
        def propertyName = getSubstitutionName(beanWrapper,property)
        if (propertyName == null) {
            propertyName = property.getName()
        }
        writer.key(propertyName)
    }

    protected Object extractValue(Object domainObject, GrailsDomainClassProperty property) {
        BeanWrapper beanWrapper = new BeanWrapperImpl(domainObject)
        return beanWrapper.getPropertyValue(property.getName())
    }

    protected Object extractIdForReference( Object refObj, GrailsDomainClass refDomainClass ) {
        Object idValue
        if (getProxyHandler() instanceof EntityProxyHandler) {
            idValue = ((EntityProxyHandler) getProxyHandler()).getProxyIdentifier(refObj)
            if (idValue == null) {
                idValue = extractValue(refObj, refDomainClass.getIdentifier())
            }
        }
        else {
            idValue = extractValue(refObj, refDomainClass.getIdentifier())
        }
        idValue
    }

    /**
     * Marshalls an object reference as a json object
     * containing a link to the referenced object as a
     * resource url.
     * @param property the property containing the reference
     * @param refObj the referenced object
     * @param json the JSON converter to marshall to
     */
    protected void asShortObject(GrailsDomainClassProperty property, Object refObj, JSON json) throws ConverterException {
        GrailsDomainClass refDomainClass = property.getReferencedDomainClass()
        Object id = extractIdForReference( refObj, refDomainClass )
        def domainName = GrailsNameUtils.getPropertyName(refDomainClass.shortName)
        def resource = hyphenate(pluralize(domainName))
        JSONWriter writer = json.getWriter()
        writer.object()
        writer.key("_link").value("/$resource/$id")
        writer.endObject()
    }


    protected void marshallSimpleField(BeanWrapper beanWrapper, GrailsDomainClassProperty property, JSON json) {
        log.trace "$this marshalObject() handling field '${property.getName()}' for ${beanWrapper.getWrappedInstance().getClass().getName()} as a simple field"
        //simple property
        writeFieldName(beanWrapper, property, json)
        // Write non-relation property
        Object val = beanWrapper.getPropertyValue(property.getName())
        json.convertAnother(val)
    }

    protected void marshallAssociationField(BeanWrapper beanWrapper, GrailsDomainClassProperty property, JSON json) {
        if (deepMarshallAssociation(beanWrapper,property)) {
            deepMarshallAssociationField(beanWrapper, property, json)
        } else {
            shallowMarshallAssociationField(beanWrapper, property, json)
        }
    }

    protected void deepMarshallAssociationField(BeanWrapper beanWrapper, GrailsDomainClassProperty property, JSON json) {
        log.trace "$this deepMarshallAssociationField handling field '${property.getName()}' for ${beanWrapper.getWrappedInstance().getClass().getName()} as a deep-marshalled association"
        writeFieldName(beanWrapper, property, json)
        Object referenceObject = beanWrapper.getPropertyValue(property.getName())
        if (referenceObject == null) {
            json.getWriter().value(null)
        } else {
            referenceObject = getProxyHandler().unwrapIfProxy(referenceObject)
            if (referenceObject instanceof SortedMap) {
                referenceObject = new TreeMap((SortedMap) referenceObject)
            } else if (referenceObject instanceof SortedSet) {
                referenceObject = new TreeSet((SortedSet) referenceObject)
            } else if (referenceObject instanceof Set) {
                referenceObject = new HashSet((Set) referenceObject)
            } else if (referenceObject instanceof Map) {
                referenceObject = new HashMap((Map) referenceObject)
            } else if (referenceObject instanceof Collection) {
                referenceObject = new ArrayList((Collection) referenceObject)
            }
            json.convertAnother(referenceObject)
        }
    }

    protected void shallowMarshallAssociationField(BeanWrapper beanWrapper, GrailsDomainClassProperty property, JSON json) {
        Class<?> clazz = beanWrapper.getWrappedInstance().getClass()
        log.trace( "$this shallowMarshallowAssociationField() handling field '${property.getName()}' for $clazz as an shallow association")

        JSONWriter writer = json.getWriter()
        Object referenceObject = beanWrapper.getPropertyValue(property.getName())
        GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass()

        if (referencedDomainClass == null || property.isEmbedded() || GrailsClassUtils.isJdk5Enum(property.getType())) {
            //hand off to marshaller chain
            log.trace( "$this marshalObject() handling field '${property.getName()}' for $clazz as a fully rendered object")
            writeFieldName(beanWrapper, property, json)
            json.convertAnother(getProxyHandler().unwrapIfProxy(referenceObject))
        } else if (property.isOneToOne() || property.isManyToOne()) {
            log.trace( "$this marshalObject() handling field '${property.getName()}' for $clazz as a short object")
            writeFieldName(beanWrapper, property, json)
            if (referenceObject == null) {
                writer.value(null)
            } else {
                asShortObject(property, referenceObject, json)
            }
        } else {
            writeFieldName(beanWrapper, property, json)
            if (referenceObject == null) {
                writer.value(null)
            } else {
                if (referenceObject instanceof Collection) {
                    log.trace( "$this marshalObject() handling field '${property.getName()}' for $clazz as a Collection")
                    marshallAssociationCollection(property, referenceObject, json)
                } else if (referenceObject instanceof Map) {
                    log.trace( "$this marshalObject() handling field ${property.getName()} for $clazz as a Map")
                    marshallAssociationMap(property, referenceObject, json)
                }
            }
        }
    }

    protected void marshallAssociationCollection(GrailsDomainClassProperty property, Object referenceObject, JSON json) {
        Collection o = (Collection) referenceObject
        GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass()
        GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier()
        @SuppressWarnings("unused")
        String refPropertyName = referencedDomainClass.getPropertyName()

        JSONWriter writer = json.getWriter()
        writer.array()
        for (Object el: o) {
            asShortObject(property, el, json)
        }
        writer.endArray()
    }

    protected void marshallAssociationMap(GrailsDomainClassProperty property, Object referenceObject, JSON json) {
        Map<Object, Object> map = (Map<Object, Object>) referenceObject
        GrailsDomainClass referencedDomainClass = property.getReferencedDomainClass()
        GrailsDomainClassProperty referencedIdProperty = referencedDomainClass.getIdentifier()
        @SuppressWarnings("unused")
        String refPropertyName = referencedDomainClass.getPropertyName()

        JSONWriter writer = json.getWriter()
        writer.object()
        for (Map.Entry<Object, Object> entry: map.entrySet()) {
            String key = String.valueOf(entry.getKey())
            Object o = entry.getValue()
            writer.key(key)
            asShortObject(property, o, json)
        }
        writer.endObject()
    }

    protected String getDerivedResourceName(Object o) {
        def domainName = GrailsNameUtils.getPropertyName(o.getClass().simpleName)
        hyphenate(pluralize(domainName))
    }

    protected String getDerivedResourceName(BeanWrapper wrapper) {
        getDerivedResourceName(wrapper.getWrappedInstance())
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

    protected ProxyHandler getProxyHandler() {
        //this should be thread-safe.  It proxyHandler is not
        //set, then two concurrent threads could try to set it together.
        //the worst case, one thread uses a (temporary) DefaultProxyHander,
        //which is then discarded.
        if (proxyHandler == null) {
            def tmp
            try {
                tmp = app.getMainContext().getBean('proxyHandler')
            } catch (NoSuchBeanDefinitionException e) {
                tmp = new DefaultProxyHandler()
            }
            proxyHandler = tmp
        }
        return proxyHandler
    }

    private String pluralize(String str) {
        Inflector.pluralize(str)
    }


    private String hyphenate(String str) {
        Inflector.hyphenate(str)
    }
}
