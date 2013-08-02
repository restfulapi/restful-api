/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

class XMLExtractorConfig implements MergeableConfig {

    //Named configurations that this config should
    //be merged with
    def inherits = []

    Map<String,String> dottedRenamedPaths = [:]
    Map<String,Object> dottedValuePaths = [:]
    List<String> dottedShortObjectPaths = []
    List<String> dottedFlattenedPaths = []
    Closure shortObjectClosure
    boolean isShortObjectClosureSet = false


    XMLExtractorConfig() {
    }

    XMLExtractorConfig(XMLExtractorConfig other) {
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
     * Merges two XMLExtractorConfig instances together
     * The values of the other instance augment or override
     * the settings in this instance.
     * @param other the other configuration to merge with
     */
    MergeableConfig merge(MergeableConfig other) {
        XMLExtractorConfig config = new XMLExtractorConfig(this)

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

        config
    }

}
