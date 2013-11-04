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
package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class XMLDomainMarshallerDelegate {

    XMLDomainMarshallerConfig config = new XMLDomainMarshallerConfig()

    XMLDomainMarshallerDelegate supports(Class clazz) {
        config.setSupportClass(clazz)
        this
    }

    XMLDomainMarshallerDelegate elementName(String name) {
        config.setElementName(name)
        this
    }

    XMLDomainMarshallerDelegate setPriority(int priority) {
        config.setPriority(priority)
        this
    }

    XMLDomainMarshallerDelegate includesId(boolean b) {
        config.includeId = b
        this
    }

    XMLDomainMarshallerDelegate includesVersion(boolean b) {
        config.includeVersion = b
        this
    }

    XMLDomainMarshallerDelegate setInherits(Collection c) {
        config.inherits = c
        this
    }

    XMLDomainMarshallerDelegate deepMarshallsAssociations(boolean b) {
        config.deepMarshallAssociations = b
        this
    }

    def field(String fieldName) {
        //if a previous field definition has supplied
        //names or resource names, clear them out
        return handleField(fieldName)
    }

    XMLDomainMarshallerDelegate includesFields(Closure c) {
        config.useIncludedFields = true
        c.delegate = new IncludeConfig()
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        this
    }

    XMLDomainMarshallerDelegate requiresIncludedFields(boolean b) {
        config.requireIncludedFields = b
        this
    }

    XMLDomainMarshallerDelegate excludesFields(Closure c) {
        c.delegate = new ExcludeConfig()
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        this
    }

    XMLDomainMarshallerDelegate additionalFields(Closure c) {
        config.additionalFieldClosures.add c
        this
    }

    XMLDomainMarshallerDelegate setAdditionalFieldsMap(Map m) {
        config.additionalFieldsMap = m
        this
    }

    XMLDomainMarshallerDelegate setShortObjectClosure(Closure c) {
        config.setShortObjectClosure(c)
        this
    }

    XMLDomainMarshallerDelegate shortObject(Closure c) {
        setShortObjectClosure( c )
        this
    }

    private FieldOptions handleField(String name) {
        config.fieldNames.remove(name)
        config.fieldResourceNames.remove(name)
        config.deepMarshalledFields.remove(name)
        return new FieldOptions(name)
    }

    class FieldOptions {
        String fieldName
        FieldOptions(String fieldName) {
            this.fieldName = fieldName
        }

        FieldOptions name(String name) {
            config.fieldNames[fieldName] = name
            this
        }

        FieldOptions resource(String name) {
            config.fieldResourceNames[fieldName] = name
            return this
        }

        FieldOptions deep(boolean b) {
            config.deepMarshalledFields[fieldName] = b
            return this
        }
    }

    class IncludeConfig {
        FieldOptions field( String name ) {
            config.includedFields.add name
            handleField(name)
        }
    }

    class ExcludeConfig {
        def field( String name ) {
            config.excludedFields.add name
        }
    }


}
