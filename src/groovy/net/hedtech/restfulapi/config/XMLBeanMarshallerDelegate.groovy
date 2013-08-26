/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class XMLBeanMarshallerDelegate {

    XMLBeanMarshallerConfig config = new XMLBeanMarshallerConfig()

    XMLBeanMarshallerDelegate supports(Class clazz) {
        config.setSupportClass(clazz)
        this
    }

    XMLBeanMarshallerDelegate elementName(String name) {
        config.setElementName(name)
        this
    }

    XMLBeanMarshallerDelegate setPriority(int priority) {
        config.setPriority(priority)
        this
    }

    XMLBeanMarshallerDelegate setInherits(Collection c) {
        config.inherits = c
        this
    }


    def field(String fieldName) {
        //if a previous field definition has supplied
        //names or resource names, clear them out
        return handleField(fieldName)
    }

    XMLBeanMarshallerDelegate includesFields(Closure c) {
        c.delegate = new IncludeConfig()
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        this
    }

    XMLBeanMarshallerDelegate requiresIncludedFields(boolean b) {
        config.requireIncludedFields = b
        this
    }

    XMLBeanMarshallerDelegate excludesFields(Closure c) {
        c.delegate = new ExcludeConfig()
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        this
    }

    XMLBeanMarshallerDelegate additionalFields(Closure c) {
        config.additionalFieldClosures.add c
        this
    }

    XMLBeanMarshallerDelegate setAdditionalFieldsMap(Map m) {
        config.additionalFieldsMap = m
        this
    }

    private FieldOptions handleField(String name) {
        config.fieldNames.remove(name)
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
