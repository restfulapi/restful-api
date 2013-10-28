/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

import org.codehaus.groovy.grails.commons.GrailsApplication

class JSONBeanMarshallerConfig implements MergeableConfig {

    //Named domain marshaller configurations that this config should
    //be merged with
    def inherits = []

    int priority = 100
    Class supportClass
    private boolean isSupportClassSet = false
    //map of field name to the name the field should be marshalled as.
    //allows fields to be renamed in the output
    def fieldNames = [:]
    //list of field names to explicity include
    //the array is empty, instead of null, to make
    //merging configs easier.  However, the includedFields
    //is ignored when creating a marshaller unless
    //useIncludedFields is true.
    def includedFields = []
    boolean useIncludedFields = false
    Boolean requireIncludedFields
    //list of additional fields to ignore
    def excludedFields = []
    //closures which will be invoked to add additional fields
    //used for affordances, computed fields, etc
    def additionalFieldClosures = []
    //map of additional input that will be passed into the
    //additional field closures
    def additionalFieldsMap = [:]


    JSONBeanMarshallerConfig() {
    }

    JSONBeanMarshallerConfig( JSONBeanMarshallerConfig other ) {
        other.getClass().declaredFields.findAll { !it.synthetic }*.name.each {
            if ((other."$it" instanceof Cloneable) && !(other."$it" instanceof Closure)) {
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

    /**
     * Merges two DomainMarshallerConfig instances together
     * The values of the other instance augment or override
     * the settings in this instance.
     * @param other the other configuration to merge with
     */
    MergeableConfig merge( MergeableConfig other ) {
        JSONBeanMarshallerConfig config = new JSONBeanMarshallerConfig( this )

        if (other.isSupportClassSet) {
            config.setSupportClass( other.supportClass )
        }
        config.fieldNames.putAll  other.fieldNames
        config.includedFields.addAll other.includedFields
        config.useIncludedFields = config.useIncludedFields || other.useIncludedFields
        config.excludedFields.addAll other.excludedFields
        config.additionalFieldClosures.addAll other.additionalFieldClosures
        config.additionalFieldsMap.putAll other.additionalFieldsMap
        if (other.requireIncludedFields != null) config.requireIncludedFields = other.requireIncludedFields

        config

    }
}
