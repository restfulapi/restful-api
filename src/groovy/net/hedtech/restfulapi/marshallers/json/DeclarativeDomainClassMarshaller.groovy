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

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler as DCAH
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.codehaus.groovy.grails.support.proxy.EntityProxyHandler
import org.codehaus.groovy.grails.web.util.WebUtils
import org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.converters.marshaller.json.*
import org.codehaus.groovy.grails.web.json.JSONWriter
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller

import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl

class DeclarativeDomainClassMarshaller extends BasicDomainClassMarshaller {

    protected static final Log log =
        LogFactory.getLog(DeclarativeDomainClassMarshaller.class)

    Class supportClass
    def fieldNames = [:]
    def includedFields = null
    boolean requireIncludedFields = false
    def excludedFields = []
    def includeId = true
    def includeVersion = true
    def additionalFieldClosures = []
    def additionalFieldsMap = [:]
    def fieldResourceNames = [:]
    //if a field is a key in the map, then represents
    //a field-level override
    def deepMarshalledFields = [:]
    //default behavior on whether to shallow or deep
    //marshall association fields
    def deepMarshallAssociations = false
    def shortObjectClosure = DEFAULT_SHORT_OBJECT

    private static DEFAULT_SHORT_OBJECT = { Map map ->
        def resource = map['resourceName']
        def id = map['resourceId']
        def json = map['json']
        def writer = json.getWriter()
        writer.object()
        writer.key("_link").value("/$resource/$id")
        writer.endObject()
    }

// ------------------- Setters ---------------------


// ------------------- End Setters ---------------------

//

    @Override
    public boolean supports(Object object) {
        if (supportClass) {
            supportClass.isInstance(object)
        } else {
            super.supports(object)
        }
    }

    /**
     * Return the name to use when marshalling the field, or
     * null if the field name should be used as-is.
     * @return the name to use when marshalling the field,
     *         or null if the domain field name should be used
     */
    @Override
    protected String getSubstitutionName(BeanWrapper beanWrapper,GrailsDomainClassProperty property) {
        return fieldNames.get( property.getName() )
    }

    /**
     * Returns the list of fields that should be marshalled
     * for the specified object.
     *<p>
     * If a null or zero-size list is returned, then
     * all fields except those specified by
     * {@link #getExcludedFields(Object) getExcludedFields} and
     * {@link #getCommonExcludedFields} will be marshalled.
     * If a non-zero sized list is returned, then only
     * the fields listed in it are marshalled.  Included fields
     * overrides any skipped fields.  That is, if a field is returned
     * by {@link getIncludedFields(Object) #getIncludedFields} then it
     * will be marshalled even if it is also returned by
     * {@link #getExcludedFields(Object) getExcludedFields} and
     * {@link #getCommonExcludedFields}
     *
     * @return list of field names to marshall
     */
    @Override
    protected List getIncludedFields(Object value) {
        return includedFields
    }

    /**
     * Override whether or not to treat an includes
     * list in a strict fashion or not.  If true then
     * an included field that is not present
     * results in a ConverterException.
     **/
    @Override
    protected boolean requireIncludedFields(Object o) {
        return this.requireIncludedFields
    }


    /**
     * Returns a list of additional fields in the
     * object that should not be marshalled.
     * The complete list of skipped fields is the
     * union of getCommonSkippedFields() and
     * the list returned by this method.
     * Does not apply if {@link #getIncludedFields(Object) getIncludedFields} returns
     * a list containing one or more field names.
     *
     * @param value the object being marshalled
     * @return list of fields that should be skipped
     */
    @Override
    protected List getExcludedFields(Object value) {
        return excludedFields
    }


    /**
     * Override processing of fields.
     * @return true if the marshaller should handle the field in
     *         the default way, false if no further action should
     *         be taken for the field.
     *
     **/
    @Override
    protected boolean processField(BeanWrapper beanWrapper,
                                   GrailsDomainClassProperty property,
                                   JSON json) {
        true
    }

    @Override
    protected void processAdditionalFields(BeanWrapper beanWrapper, JSON json) {
        Map map = [:]
        map.putAll( additionalFieldsMap )
        map.putAll(
            [
                'grailsApplication':app,
                'beanWrapper':beanWrapper,
                'json':json
            ]
        )
        if (!map['resourceName']) {
            map['resourceName'] = getDerivedResourceName(beanWrapper)
        }
        GrailsDomainClass domainClass = app.getDomainClass(beanWrapper.getWrappedInstance().getClass().getName())
        map['resourceId'] = beanWrapper.getPropertyValue(domainClass.getIdentifier().getName())
        additionalFieldClosures.each { c ->
            c.call( map )
        }
    }

    /**
     * Override whether to include an 'id' field
     * for the specified value.
     * @param o the value
     * @return true if an 'id' field should be placed in the
     *         representation
     **/
    @Override
    protected boolean includeIdFor(Object o) {
        return includeId
    }

    /**
     * Override whether to include a 'version' field
     * for the specified value.
     * @param o the value
     * @return true if a 'version' field should be placed in the
     *         representation
     **/
    @Override
    protected boolean includeVersionFor(Object o) {
        return includeVersion
    }

    /**
     * Specify whether to deep marshal a field representing
     * an association, or render it as a short-object.
     * @param beanWrapper the wrapped object containing the field
     * @param property the property to be marshalled
     * @return true if the value of the field should be deep rendered,
     *         false if a short-object representation should be used.
     **/
    @Override
    protected boolean deepMarshallAssociation(BeanWrapper beanWrapper, GrailsDomainClassProperty property) {
        if (deepMarshalledFields.containsKey(property.getName())) {
            return deepMarshalledFields[property.getName()]
        } else {
            return deepMarshallAssociations
        }
    }

    /**
     * Marshalls an object reference as a json object
     * containing a link to the referenced object as a
     * resource url.
     * @param property the property containing the reference
     * @param refObj the referenced object
     * @param json the JSON converter to marshall to
     */
    @Override
    protected void asShortObject(GrailsDomainClassProperty property, Object refObj, JSON json) throws ConverterException {
        GrailsDomainClass refDomainClass = property.getReferencedDomainClass()
        Object id = extractIdForReference( refObj, refDomainClass )
        def resource = fieldResourceNames[property.getName()]
        if (resource == null) {
            def domainName = GrailsNameUtils.getPropertyName(refDomainClass.shortName)
            resource = hyphenate(pluralize(domainName))
        }
        Map map = [
            grailsApplication:app,
            property:property,
            refObject:refObj,
            json:json,
            resourceId:id,
            resourceName:resource
        ]
        this.shortObjectClosure.call(map)
    }
}
