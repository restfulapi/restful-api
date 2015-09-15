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

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import grails.converters.JSON

import java.beans.PropertyDescriptor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

import grails.util.GrailsClassUtils
import grails.core.support.proxy.ProxyHandler
import org.grails.web.json.JSONWriter

import org.springframework.beans.BeanWrapper

/**
 * A bean marshaller.
 * By default, it will marshall all properties and public (non-static/non-transient)
 * fields.
 * The class can be extended to override how an object is marshalled.
 **/
class BeanMarshaller extends AbstractBeanMarshaller {

    protected static final Log log =
        LogFactory.getLog(BeanMarshaller.class)

    private static List EXCLUDED_FIELDS =
        Arrays.asList('password', 'metaClass', 'class')

    @Override
    public boolean supports(Object object) {
        !(object instanceof Collection) &&
        !(object instanceof Map) &&
        !GrailsClassUtils.isJdk5Enum(object.getClass())
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
