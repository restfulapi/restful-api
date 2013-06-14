/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors.json

import net.hedtech.restfulapi.extractors.*
import org.codehaus.groovy.grails.web.json.JSONObject

class BasicJSONExtractor implements JSONExtractor {

    //yes, it needs to be volatile, or the double-checked locking pattern does not work in java
    protected volatile MapTransformer transformer
    protected boolean initialized = false

    private static DEFAULT_SHORT_OBJECT_CLOSURE = { value ->
        if (value == null) return value
        if (Collection.class.isAssignableFrom(value.getClass())) {
            def result = []
            value.each() {
                if (it !=null && Map.class.isAssignableFrom(it.getClass())) {
                    def v = it['_link']
                    result.add( [id:v.substring(v.lastIndexOf('/')+1)] )
                } else {
                    throw new Exception( "Cannot convert from short object for $it" )
                }
            }
            return result
        } else {
            if (Map.class.isAssignableFrom(value.getClass())) {
                def v = value['_link']
                return [id:v.substring(v.lastIndexOf('/')+1)]
            } else {
                throw new Exception( "Cannot convert from short object for $value" )
            }
        }
    }

    BasicJSONExtractor() {
    }

    Map extract( JSONObject content ) {
        getTransformer().transform( content )
    }

    boolean ready() {
        getTransformer() != null
    }

    protected MapTransformer getTransformer() {
        if (transformer == null) {
            synchronized (this) {
                if (transformer == null) {
                    MapTransformerRules rules = new MapTransformerRules()
                    getRenamePaths().entrySet().each { Map.Entry entry ->
                        rules.addRenameRule(entry.key, entry.value)
                    }
                    getDefaultValuePaths().entrySet().each { Map.Entry entry ->
                        rules.addDefaultValueRule(entry.key, entry.value)
                    }
                    getShortObjectPaths().each { def path ->
                        rules.addModifyValueRule(path,getShortObjectClosure())
                    }
                    getFlattenPaths().each() { def path ->
                        rules.addFlattenRule(path)
                    }
                    transformer = new MapTransformer(rules)
                }
            }
        }
        transformer
    }

    /**
     * Returns a map of rename rules.  The keys
     * are List of String denoting paths to keys in the map which
     * should be renamed as the value the path is mapped to.
     */
    protected Map<List<String>,String> getRenamePaths() {
        [:]
    }

    /**
     * Returns a map of default value rules.  The keys
     * are List of String denoting paths to keys in the map
     * which should have the default value if the key is not
     * already present.
     */
    protected Map<List<String>,Object> getDefaultValuePaths() {
        [:]
    }

    /**
     * Returns a List of String denoting paths whose
     * values should be treated as short objects
     * and converted according the the short object closure.
     **/
    protected List<List<String>> getShortObjectPaths() {
        []
    }

    /**
     * Returns a List of String denoting paths whose
     * values should be flattened into the containing map.
     **/
     protected List<List<String>> getFlattenPaths() {
        []
     }

    /**
     * Returns a closure that can convert a 'short object'
     * representation to a map containing the id represented
     * by the short object reference.
     */
    protected Closure getShortObjectClosure() {
        DEFAULT_SHORT_OBJECT_CLOSURE
    }
}