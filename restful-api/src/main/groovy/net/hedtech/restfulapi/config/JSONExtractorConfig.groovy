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

class JSONExtractorConfig implements MergeableConfig {

    //Named configurations that this config should
    //be merged with
    def inherits = []

    Map<String,String> dottedRenamedPaths = [:]
    Map<String,Object> dottedValuePaths = [:]
    List<String> dottedShortObjectPaths = []
    List<String> dottedFlattenedPaths = []
    Closure shortObjectClosure
    boolean isShortObjectClosureSet = false
    Boolean lenientDates = null
    List<String> dottedDatePaths = []
    List<String> dateFormats = []



    JSONExtractorConfig() {
    }

    JSONExtractorConfig(JSONExtractorConfig other) {
        other.getClass().declaredFields.findAll { !it.synthetic }*.name.each {
            if ((other."$it" instanceof Cloneable) && !(other."$it" instanceof Closure)) {
                this."$it" = other."$it".clone()
            } else {
                this."$it" = other."$it"
            }
        }
    }

    void setShortObjectClosure(Closure c) {
        this.shortObjectClosure = c
        this.isShortObjectClosureSet = true
    }

    /**
     * Merges two JSONExtractorConfig instances together
     * The values of the other instance augment or override
     * the settings in this instance.
     * @param other the other configuration to merge with
     */
    MergeableConfig merge(MergeableConfig other) {
        JSONExtractorConfig config = new JSONExtractorConfig(this)

        config.dottedRenamedPaths.putAll other.dottedRenamedPaths
        config.dottedValuePaths.putAll other.dottedValuePaths
        other.dottedShortObjectPaths.each {
            if (!config.dottedShortObjectPaths.contains(it)) {
                config.dottedShortObjectPaths.add(it)
            }
        }
        other.dottedFlattenedPaths.each {
            if (!config.dottedFlattenedPaths.contains(it)) {
                config.dottedFlattenedPaths.add(it)
            }
        }

        if (other.isShortObjectClosureSet) {
            config.shortObjectClosure = other.shortObjectClosure
        }

        other.dottedDatePaths.each {
            if (!config.dottedDatePaths.contains(it)) {
                config.dottedDatePaths.add(it)
            }
        }

        other.dateFormats.each {
            if (!config.dateFormats.contains(it)) {
                config.dateFormats.add(it)
            }
        }
        if (other.lenientDates != null) {
            config.lenientDates = other.lenientDates
        }

        config
    }

}
