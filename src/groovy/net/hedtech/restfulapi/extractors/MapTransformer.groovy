/* ***************************************************************************
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
package net.hedtech.restfulapi.extractors

/**
 * Class to provide abilities to transform maps by renaming keys and
 * providing default values.
 * The MapTransformer operates on nested maps that may also contain collections.
 * The keys of the map(s) are assumed to be strings.
 * A path is a sequence of keys denoting a key within the map or a sub-map.
 * If any part of a path denotes a key whose value is a collection, then the rest of
 * the path is applied to all elements of the collection.
 * For example, if the transformer is configured to rename the path ['books','title'] to
 * 'bookTitle' and the value of 'books' is a Set of maps representing the books, then each
 * map in the books set that contains the key 'title' will have it renamed to 'myTitle'.
 *
 * All rules should be specified in terms of paths within the original map.
 * Rules are applied depth-first.
 **/
class MapTransformer {

    //map of paths (lists of string) to arrays of closures
    //that define transformations to apply to that path
    protected def actions = [:]
    protected MapTransformerRules rules

    MapTransformer(MapTransformerRules rules) {
        this.rules = rules
        init()
    }

    /**
     * Transforms the specified map.
     * @param map the map to transform
     **/
    def transform(Map map) {
        def keys = actions.keySet()
        //sort longest to shortest paths
        keys = keys.sort { a,b -> b.size() <=> a.size() }

        keys.each { path ->
            path = path.clone()
            transform(map, [], path)
        }
        map
    }

    /**
     * Transforms a map by applying closures to the path.
     **/
    protected transform(Map currentObject, def objectPath, def remainingPath) {
        if (remainingPath.size() == 0) {
            def closureHolder = actions.get(objectPath)

            closureHolder.defaultValueClosures.each { Closure c ->
                c.call(currentObject)
            }
            closureHolder.modifyValueClosures.each { Closure c ->
                c.call(currentObject)
            }
            closureHolder.renameClosures.each { Closure c ->
                c.call(currentObject)
            }
            closureHolder.flattenClosures.each { Closure c ->
                c.call(currentObject)
            }
        } else {
            def key = remainingPath.remove(0)
            objectPath.add key
            if (currentObject.containsKey(key)) {
                def nextObject = currentObject[key]
                if (nextObject instanceof Map) {
                    transform(nextObject, objectPath, remainingPath)
                } else if (nextObject instanceof List) {
                    nextObject.each {
                        if (it instanceof Map) {
                            transform(it, objectPath, remainingPath)
                        }
                    }
                }
            }
            remainingPath.add objectPath.pop()
        }

    }

    protected void init() {
        addRenameActions()
        addDefaultValueActions()
        addModifyValueActions()
        addFlattenActions()
    }

    private void addRenameActions() {
        rules.renameRules.entrySet().each { Map.Entry entry ->
            def path = entry.key
            def newKey = entry.value
            path = path.clone()
            def oldKey = path.pop()
            Closure c = {Map m ->
                if (m.containsKey(oldKey)) {
                    def val = m.remove(oldKey)
                    m.put(newKey, val)
                }
            }
            addRenameClosure(path, c)
        }
    }

    private void addDefaultValueActions() {
        rules.defaultValueRules.entrySet().each { Map.Entry entry ->
            def path = entry.key.clone()
            def localKey = path.pop()
            def defaultValue = entry.value
            Closure c = {Map m ->
                if (!m.containsKey(localKey)) {
                    m.put(localKey, defaultValue)
                }
            }
            addDefaultValueClosure(path, c)
        }
    }

    private void addModifyValueActions() {
        rules.modifyValueRules.each() { Map.Entry entry ->
            def path = entry.key.clone()
            def localKey = path.pop()
            def closure = entry.value
            Closure c = {Map m ->
                if (m.containsKey(localKey)) {
                    def val = m.get(localKey)
                    val = closure.call(val)
                    m.put(localKey, val)
                }
            }
            addModifyValueClosure(path, c)
        }
    }

    private void addFlattenActions() {
        rules.flattenPaths.each { def path ->
            def newKey = rules.renameRules.get(path)
            if (newKey != null) {
                path.pop()
                path.add newKey
            }
            path = path.clone()
            def localKey = path.pop()
            Closure c = {Map m ->
                if (m.containsKey(localKey)) {
                    def val = m.get(localKey)
                    if (val instanceof Collection) {
                        m.remove(localKey)
                        int index = 0
                        val.each {
                            if (it instanceof Map) {
                                flattenInto(m, it, "$localKey[$index]")
                                index++
                            }
                        }
                    } else {
                        if (val instanceof Map) {
                            m.remove(localKey)
                            flattenInto(m, val, localKey)
                        }
                    }
                }
            }
            addFlattenClosure(path, c)
        }
    }

    private void addRenameClosure(List<String> path, Closure c) {
        getClosureHolder(path).renameClosures.add c
    }

    private void addDefaultValueClosure(List<String> path, Closure c) {
        getClosureHolder(path).defaultValueClosures.add c
    }

    private void addModifyValueClosure(List<String> path, Closure c) {
        getClosureHolder(path).modifyValueClosures.add c
    }

    private void addFlattenClosure(List<String> path, Closure c) {
        getClosureHolder(path).flattenClosures.add c
    }

    private ClosureHolder getClosureHolder(List<String> path) {
        if (!actions.containsKey(path)) {
            actions[path] = new ClosureHolder()
        }
        actions[path]
    }

    /**
     * Flattens the source map into the target map using the specified prefix.
     * All key/value pairs in the source map are added to the target as
     * "prefix.key"/value pairs.
     **/
    private void flattenInto(Map target, Map source, String prefix) {
        source.entrySet().each { Map.Entry entry ->
            target.put(prefix + "." + entry.key, entry.value)
        }
        target
    }

    class ClosureHolder {
        def renameClosures = []
        def defaultValueClosures = []
        def modifyValueClosures = []
        def flattenClosures = []
    }
}
