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

import grails.core.GrailsApplication

class XMLBeanMarshallerConfig implements MergeableConfig {

    //Named domain marshaller configurations that this config should
    //be merged with
    def inherits = []

    int priority = 100
    Class supportClass
    private boolean isSupportClassSet = false
    String elementName
    private boolean isElementNameSet = false
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

    //Whether to marshall fields that are null or not
    //Applies as a default to all fields not explicitly configured
    //inthe includeNullField map.
    Boolean marshallNullFields
    //Map of field names to boolean.  If true, that field will be
    //marshalled, even if null.  If false, the field will only be
    //marshalled if it has a non-null value.
    def marshalledNullFields = [:]


    XMLBeanMarshallerConfig() {
    }

    XMLBeanMarshallerConfig( XMLBeanMarshallerConfig other ) {
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

    def setElementName( String name ) {
        this.isElementNameSet = true
        this.elementName = name
        this
    }

    /**
     * Merges two DomainMarshallerConfig instances together
     * The values of the other instance augment or override
     * the settings in this instance.
     * @param other the other configuration to merge with
     */
    MergeableConfig merge( MergeableConfig other ) {
        XMLBeanMarshallerConfig config = new XMLBeanMarshallerConfig( this )

        if (other.isSupportClassSet) {
            config.setSupportClass( other.supportClass )
        }
        if (other.isElementNameSet) {
            config.setElementName( other.elementName )
        }
        config.fieldNames.putAll  other.fieldNames
        config.includedFields.addAll other.includedFields
        config.useIncludedFields = config.useIncludedFields || other.useIncludedFields
        config.excludedFields.addAll other.excludedFields
        config.additionalFieldClosures.addAll other.additionalFieldClosures
        config.additionalFieldsMap.putAll other.additionalFieldsMap
        config.marshalledNullFields.putAll other.marshalledNullFields
        if (other.requireIncludedFields != null) config.requireIncludedFields = other.requireIncludedFields
        if (other.marshallNullFields != null) config.marshallNullFields = other.marshallNullFields

        config

    }
}
