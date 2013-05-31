/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.marshallers.json

import grails.converters.JSON
import groovy.lang.GroovyObject

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import net.hedtech.restfulapi.Inflector

import java.beans.PropertyDescriptor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import grails.util.GrailsNameUtils

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller
import org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.codehaus.groovy.grails.web.json.JSONWriter
import org.springframework.beans.BeanUtils

import org.springframework.beans.BeanWrapper
import org.springframework.beans.BeanWrapperImpl

/**
 * A groovy bean marshaller.
 * By default, it will marshall all properties and public (non-static/non-transient)
 * fields.
 * The class can be extended to override how an object is marshalled.
 **/
abstract
class AbstractBeanMarshaller implements ObjectMarshaller<JSON> {

    protected static final Log log =
        LogFactory.getLog(AbstractBeanMarshaller.class)

    GrailsApplication app
    ProxyHandler proxyHandler

    AbstractBeanMarshaller() {
        this.proxyHandler = new HibernateProxyHandler()
    }


    public void marshalObject(Object value, JSON json) throws ConverterException {
        Class<?> clazz = value.getClass()
        log.trace "$this marshalObject() called for $clazz"

        value = proxyHandler.unwrapIfProxy(value)
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
            if (includedFields != null && includedFields.size() > 0) {
                //use inclusion list
                propertiesToMarshall = availableProperties.findAll { PropertyDescriptor property ->
                    includedFields.contains(property.getName())
                }
                fieldsToMarshall = availableFields.findAll { Field field ->
                    includedFields.contains(field.getName())
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

            propertiesToMarshall.each() { PropertyDescriptor property ->
                if (processProperty(beanWrapper, property, json)) {
                    writeFieldName(beanWrapper, property, json)
                    Object val = beanWrapper.getPropertyValue(property.getName())
                    json.convertAnother(val)
                }
            }
            fieldsToMarshall.each() { Field field ->
                if (processField(value, field, json)) {
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
    protected List<String> getIncludedFields(Object value) {
        []
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
}
