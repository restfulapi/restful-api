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

class JSONDomainMarshallerConfig implements MergeableConfig {

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
    //map of field names (that are associations)
    //to the resource names the associated object
    //represents.  Used to override the default
    //behavior of pluralizing the simple domain name
    def fieldResourceNames = [:]
    //Contains a closure that will override the default
    //rendering behavior for short objects.
    def shortObjectClosure = null
    Boolean deepMarshallAssociations
    def deepMarshalledFields = [:]
    boolean isShortObjectClosureSet = false
    Boolean includeId
    Boolean includeVersion


    JSONDomainMarshallerConfig() {

    }

    JSONDomainMarshallerConfig( JSONDomainMarshallerConfig other ) {
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

    void setShortObjectClosure(Closure c) {
        this.shortObjectClosure = c
        this.isShortObjectClosureSet = true
    }


    /**
     * Merges two DomainMarshallerConfig instances together
     * The values of the other instance augment or override
     * the settings in this instance.
     * @param other the other configuration to merge with
     */
    MergeableConfig merge( MergeableConfig other ) {
        JSONDomainMarshallerConfig config = new JSONDomainMarshallerConfig( this )

        if (other.isSupportClassSet) {
            config.setSupportClass( other.supportClass )
        }
        if (other.includeId != null)      config.includeId      = other.includeId
        if (other.includeVersion != null) config.includeVersion = other.includeVersion
        if (other.requireIncludedFields != null) config.requireIncludedFields = other.requireIncludedFields
        if (other.deepMarshallAssociations != null) config.deepMarshallAssociations = other.deepMarshallAssociations

        config.fieldNames.putAll  other.fieldNames
        config.includedFields.addAll other.includedFields
        config.useIncludedFields = config.useIncludedFields || other.useIncludedFields
        config.excludedFields.addAll other.excludedFields
        config.additionalFieldClosures.addAll other.additionalFieldClosures
        config.additionalFieldsMap.putAll other.additionalFieldsMap
        config.fieldResourceNames.putAll other.fieldResourceNames
        config.deepMarshalledFields.putAll other.deepMarshalledFields
        if (other.isShortObjectClosureSet) {
            config.shortObjectClosure = other.shortObjectClosure
        }

        config
    }
}
