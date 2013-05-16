/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class DomainMarshallerConfig implements MergeableConfig {

    //Named domain marshaller configurations that this config should
    //be merged with
    def includes = []

    int priority = 100
    Class supportClass
    private boolean isSupportClassSet = false
    def substitutions = [:]
    def includedFields = []
    def excludedFields = []
    def additionalFieldClosures = []
    def additionalFieldsMap = [:]
    Boolean includeId
    Boolean includeVersion

    DomainMarshallerConfig() {

    }

    DomainMarshallerConfig( DomainMarshallerConfig other ) {
        other.getClass().declaredFields.findAll { !it.synthetic }*.name.each {
            if (other."$it" instanceof Cloneable) {
                this."$it" = other."$it".clone()
            } else {
                this."$it" = other."$it"
            }
        }
    }

    def setSupportClass( Class clazz ) {
        this.isSupportClassSet = true
        this.supportClass = clazz
        this
    }

    def field(String fieldName) {
        [substitute: { String otherName ->
            substitutions[fieldName] = otherName
        }]
    }

    def include( Closure c ) {
        c.delegate = new FieldConfig( this )
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        this
    }

    def exclude( Closure c ) {
        c.delegate = new ExcludeField( this )
        c.resolveStrategy = Closure.DELEGATE_ONLY
        c.call()
        this
    }

    def additionalFields( Closure c ) {
        additionalFieldClosures.add c
        this
    }

    def additionalFieldsMap( Map m ) {
        this.additionalFieldsMap = m
        this
    }

    /**
     * Merges two DomainMarshallerConfig instances together
     * The values of the other instance augment or override
     * the settings in this instance.
     * @param other the other configuration to merge with
     */
    MergeableConfig merge( MergeableConfig other ) {
        DomainMarshallerConfig config = new DomainMarshallerConfig( this )

        if (other.isSupportClassSet) {
            config.setSupportClass( other.supportClass )
        }
        if (other.includeId != null)      config.includeId      = other.includeId
        if (other.includeVersion != null) config.includeVersion = other.includeVersion

        config.substitutions.putAll  other.substitutions
        config.includedFields.addAll other.includedFields
        config.excludedFields.addAll other.excludedFields
        config.additionalFieldClosures.addAll other.additionalFieldClosures
        config.additionalFieldsMap.putAll other.additionalFieldsMap

        config

    }


    class FieldConfig {
        DomainMarshallerConfig parent
        FieldConfig( DomainMarshallerConfig parent ) {
            this.parent = parent
        }

        def field( String name ) {
            parent.includedFields.add name
            [substitute: {String otherName ->
                parent.substitutions[name] = otherName
            }]
        }
    }

    class ExcludeField {
        DomainMarshallerConfig parent
        ExcludeField( DomainMarshallerConfig parent ) {
            this.parent = parent
        }

        def field( String name ) {
            parent.excludedFields.add name
        }
    }


}