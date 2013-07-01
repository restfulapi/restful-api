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
import org.codehaus.groovy.grails.web.xml.*

import org.springframework.beans.BeanWrapper

/**
 * A groovy bean marshaller.
 * By default, it will marshall all properties and public (non-static/non-transient)
 * fields.
 * The class can be extended to override how an object is marshalled.
 **/
class GroovyBeanMarshaller extends AbstractBeanMarshaller {

    protected static final Log log =
        LogFactory.getLog(GroovyBeanMarshaller.class)

    private static List EXCLUDED_FIELDS =
        Arrays.asList('password', 'metaClass', 'class')

    @Override
    public boolean supports(Object object) {
        return object instanceof GroovyObject;
    }

    @Override
    protected List<Field> getAvailableFields(Object value) {
        def fields = []
        value.getClass().getDeclaredFields().each { field ->
            int modifiers = field.getModifiers()
            if (Modifier.isPublic(modifiers) && !(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
                fields.add field
            }
        }
        return fields
    }

    @Override
    protected List getCommonExcludedFields() {
        EXCLUDED_FIELDS
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
    protected List<String> getExcludedFields(Object value) {
        super.getExcludedFields(value)
    }

}
