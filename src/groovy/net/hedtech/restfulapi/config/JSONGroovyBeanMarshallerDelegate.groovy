/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class JSONGroovyBeanMarshallerDelegate {

    JSONGroovyBeanMarshallerConfig config = new JSONGroovyBeanMarshallerConfig()

    JSONGroovyBeanMarshallerDelegate supports( Class clazz ) {
        config.setSupportClass( clazz )
        this
    }

    JSONGroovyBeanMarshallerDelegate setPriority(int priority) {
        config.setPriority(priority)
        this
    }

    JSONGroovyBeanMarshallerDelegate setInherits(Collection c) {
        config.inherits = c
        this
    }


    def field(String fieldName) {
        //if a previous field definition has supplied
        //names or resource names, clear them out
        return handleField(fieldName)
    }

    JSONGroovyBeanMarshallerDelegate includesFields(Closure c) {
        c.delegate = new IncludeConfig()
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        this
    }

    JSONGroovyBeanMarshallerDelegate requiresIncludedFields(boolean b) {
        config.requireIncludedFields = b
        this
    }

    JSONGroovyBeanMarshallerDelegate excludesFields(Closure c) {
        c.delegate = new ExcludeConfig()
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        this
    }

    JSONGroovyBeanMarshallerDelegate additionalFields(Closure c) {
        config.additionalFieldClosures.add c
        this
    }

    JSONGroovyBeanMarshallerDelegate setAdditionalFieldsMap(Map m) {
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
