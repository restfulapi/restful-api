/* ****************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
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

package net.hedtech.restfulapi.marshallers.xml

import grails.converters.XML

import java.lang.reflect.Method

import grails.util.GrailsClassUtils
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.converters.marshaller.ObjectMarshaller
import org.springframework.beans.BeanUtils

/**
 * An enum marshaller based on the default grails behavior, but that only outputs the name
 * of the enum as a value.
 **/
public class EnumMarshaller implements ObjectMarshaller<XML> {

    public boolean supports(Object object) {
        GrailsClassUtils.isJdk5Enum(object.getClass())
    }

    public void marshalObject(Object en, XML xml) throws ConverterException {
        try {
            Class<?> enumClass = en.getClass();
            Method nameMethod = BeanUtils.findDeclaredMethod(enumClass, "name", null);
            try {
                xml.chars(nameMethod.invoke(en).toString());
            } catch (Exception e) {
                // ignored
            }
        } catch (ConverterException ce) {
            throw ce;
        } catch (Exception e) {
            throw new ConverterException("Error converting Enum with class " + en.getClass().getName(), e);
        }
    }
}