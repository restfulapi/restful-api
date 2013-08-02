/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class JSONDomainMarshallerDelegate {

    JSONDomainMarshallerConfig config = new JSONDomainMarshallerConfig()

    JSONDomainMarshallerDelegate supports( Class clazz ) {
        config.setSupportClass( clazz )
        this
    }

    JSONDomainMarshallerDelegate setPriority(int priority) {
        config.setPriority(priority)
        this
    }

    JSONDomainMarshallerDelegate includesId(boolean b) {
        config.includeId = b
        this
    }

    JSONDomainMarshallerDelegate includesVersion(boolean b) {
        config.includeVersion = b
        this
    }

    JSONDomainMarshallerDelegate setInherits(Collection c) {
        config.inherits = c
        this
    }

    JSONDomainMarshallerDelegate deepMarshallsAssociations(boolean b) {
        config.deepMarshallAssociations = b
        this
    }


    def field(String fieldName) {
        //if a previous field definition has supplied
        //names or resource names, clear them out
        return handleField(fieldName)
    }

    JSONDomainMarshallerDelegate includesFields(Closure c) {
        c.delegate = new IncludeConfig()
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        this
    }

    JSONDomainMarshallerDelegate requiresIncludedFields(boolean b) {
        config.requireIncludedFields = b
        this
    }

    JSONDomainMarshallerDelegate excludesFields(Closure c) {
        c.delegate = new ExcludeConfig()
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        this
    }

    JSONDomainMarshallerDelegate additionalFields(Closure c) {
        config.additionalFieldClosures.add c
        this
    }

    JSONDomainMarshallerDelegate setAdditionalFieldsMap(Map m) {
        config.additionalFieldsMap = m
        this
    }

    JSONDomainMarshallerDelegate setShortObjectClosure(Closure c) {
        config.setShortObjectClosure(c)
        this
    }

    JSONDomainMarshallerDelegate shortObject(Closure c) {
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
