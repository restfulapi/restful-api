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

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import net.hedtech.restfulapi.Inflector

import net.hedtech.restfulapi.marshallers.MissingFieldsException

import java.beans.PropertyDescriptor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import grails.util.GrailsNameUtils

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.codehaus.groovy.grails.support.proxy.DefaultProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.json.JSONWriter
import org.springframework.beans.BeanUtils

import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl
import org.springframework.beans.factory.NoSuchBeanDefinitionException


/**
 * A bean marshaller.
 * By default, it will marshall all properties and public (non-static/non-transient)
 * fields.
 * The class can be extended to override how an object is marshalled.
 **/
abstract
class AbstractBeanMarshaller implements ObjectMarshaller<JSON> {

    protected static final Log log =
        LogFactory.getLog(AbstractBeanMarshaller.class)

    GrailsApplication app
    //allow proxy handler to be explicitly set
    //this field should never be used directly,
    //use getProxyHandler() instead
    ProxyHandler proxyHandler

    AbstractBeanMarshaller() {
    }


    public void marshalObject(Object value, JSON json) throws ConverterException {
        Class<?> clazz = value.getClass()
        log.trace "$this marshalObject() called for $clazz"

        value = getProxyHandler().unwrapIfProxy(value)
        BeanWrapper beanWrapper = new BeanWrapperImpl(value)

        JSONWriter writer = json.getWriter()
        try {
            writer.object()
            processAdditionalFields(beanWrapper, json)

            List<PropertyDescriptor> availableProperties = getAvailableProperties(beanWrapper)
            List<Field> availableFields = getAvailableFields(value)

            def propertiesToMarshall = []
            def fieldsToMarshall = []
            List<String> includedFields = getIncludedFields( value )
            if (includedFields != null) {
                //use inclusion list
                propertiesToMarshall = availableProperties.findAll { PropertyDescriptor property ->
                    includedFields.contains(property.getName())
                }
                fieldsToMarshall = availableFields.findAll { Field field ->
                    includedFields.contains(field.getName())
                }
                if (requireIncludedFields( value )) {
                    def toBeMarshalled = []
                    toBeMarshalled.addAll propertiesToMarshall*.name
                    toBeMarshalled.addAll fieldsToMarshall*.name
                    if (!toBeMarshalled.equals(includedFields)) {
                        def missing = []
                        missing.addAll includedFields
                        missing.removeAll(toBeMarshalled)
                        throw new MissingFieldsException( missing )
                    }
                }
            } else {
                //use exclusion list
                List excludedFields = getCommonExcludedFields() + getExcludedFields(value)
                propertiesToMarshall = availableProperties.findAll { PropertyDescriptor property ->
                    !excludedFields.contains(property.getName())
                }
                fieldsToMarshall = availableFields.findAll { Field field ->
                    !excludedFields.contains(field.getName())
                }
            }

            def propertyNames = propertiesToMarshall*.name

            propertiesToMarshall.each() { PropertyDescriptor property ->
                if (processProperty(beanWrapper, property, json)) {
                    writeFieldName(beanWrapper, property, json)
                    Object val = beanWrapper.getPropertyValue(property.getName())
                    json.convertAnother(val)
                }
            }
            fieldsToMarshall.each() { Field field ->
                if (!propertyNames.contains(field.name) && processField(value, field, json)) {
                    writeFieldName(beanWrapper, field, json)
                    Object val = field.get(value)
                    json.convertAnother(val)
                }
            }

            writer.endObject()
        }
        catch (ConverterException ce) {
            throw ce
        }
        catch (Exception e) {
            throw new ConverterException("Error converting Bean with class " + value.getClass().getName(), e)
        }
    }



// ------------------- Methods to override to customize behavior ---------------------

    /**
     * Returns a List of PropertyDescriptor of properties available to be marshalled.
     * Properties returned must have an available read method.
     **/
    protected List<PropertyDescriptor> getAvailableProperties(BeanWrapper beanWrapper) {
        beanWrapper.getPropertyDescriptors().findAll {
            it.getReadMethod() != null
        }
    }

    abstract
    protected List<Field> getAvailableFields(Object value)

    /**
     * Return the name to use when marshalling the property, or
     * null if the field name should be used as-is.
     * @return the name to use when marshalling the field,
     *         or null if the domain field name should be used
     */
    protected String getSubstitutionName(BeanWrapper beanWrapper, PropertyDescriptor property) {
        null
    }

    /**
     * Return the name to use when marshalling the field, or
     * null if the field name should be used as-is.
     * @return the name to use when marshalling the field,
     *         or null if the domain field name should be used
     */
    protected String getSubstitutionName(Object value, Field field) {
        null
    }

    /**
     * Returns the list of fields/properties that should be marshalled
     * for the specified object.
     *<p>
     * If a null list is returned, then
     * all fields except those specified by
     * {@link #getExcludedFields(Object) getExcludedFields} and
     * {@link #getCommonExcludedFields} will be marshalled.
     * If a non-null list is returned, then only
     * the fields listed in it are marshalled.  Included fields
     * overrides any skipped fields.  That is, if
     * {@link getIncludedFields(Object) #getIncludedFields} returns a
     * non-null list, then
     * {@link #getExcludedFields(Object) getExcludedFields} and
     * {@link #getCommonExcludedFields} are ignored.
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
     * Returns a list of additional properties/fields in the
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
    abstract
    protected List getCommonExcludedFields()


    /**
     * Override processing of properties.
     * @return true if the marshaller should handle the field in
     *         the default way, false if no further action should
     *         be taken for the field.
     *
     **/
    protected boolean processProperty(BeanWrapper beanWrapper,
                                      PropertyDescriptor property,
                                      JSON json) {
        true
    }

    /**
     * Override processing of fields.
     * @return true if the marshaller should handle the field in
     *         the default way, false if no further action should
     *         be taken for the field.
     *
     **/
    protected boolean processField(Object obj,
                                   Field field,
                                   JSON json) {
        true
    }


    protected void processAdditionalFields(BeanWrapper beanWrapper, JSON json) {
    }

// ------------------- Methods to support marshalling ---------------------

    protected def writeFieldName(BeanWrapper beanWrapper,
                                 def propertyOrField,
                                 JSON json) {
        JSONWriter writer = json.getWriter()
        def name = getSubstitutionName(beanWrapper,propertyOrField)
        if (name == null) {
            name = propertyOrField.getName()
        }
        writer.key(name)
    }

   protected String getDerivedResourceName(Object o) {
        def domainName = GrailsNameUtils.getPropertyName(o.getClass().simpleName)
        hyphenate(pluralize(domainName))
    }

    protected String getDerivedResourceName(BeanWrapper wrapper) {
        getDerivedResourceName(wrapper.getWrappedInstance())
    }

    private String pluralize(String str) {
        Inflector.pluralize(str)
    }

    private String hyphenate(String str) {
        Inflector.hyphenate(str)
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
}
