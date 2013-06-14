/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.extractors

/**
 * All rules should be specified in terms of paths within the original map.
 **/
class MapTransformerRules {

    def renameRules = [:]
    def defaultValueRules = [:]
    def modifyValueRules = [:]
    def flattenPaths = []

    /**
     * Renames the final element of the path to a new
     * value.
     * @param path a list of strings denoting the path to rename
     * @param newKey the new name for the last element of the path
     */
    void addRenameRule(List<String> path, String newKey) {
        renameRules.put(path.clone(), newKey)
    }

    /**
     * Adds a default value for the specified path
     * if the key denoted by the path is not already present.
     * @param path a list of strings denoting the path to rename
     * @param newKey the new name for the last element of the path
     **/
    void addDefaultValueRule(List<String> path, def defaultValue) {
        defaultValueRules.put(path.clone(), defaultValue)
    }

    void addModifyValueRule(List<String> path, Closure closure) {
        modifyValueRules.put(path.clone(),closure)
    }

    void addFlattenRule(List<String> path) {
        if (!flattenPaths.contains(path)) {
            flattenPaths.add path.clone()
        }
    }
}