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
package net.hedtech.restfulapi.extractors.json

import java.text.ParseException
import java.text.SimpleDateFormat

import net.hedtech.restfulapi.extractors.*
import org.codehaus.groovy.grails.web.json.*

class BasicJSONExtractor implements JSONExtractor {

    //yes, it needs to be volatile, or the double-checked locking pattern does not work in java
    protected volatile MapTransformer transformer
    protected boolean initialized = false

    private static DEFAULT_SHORT_OBJECT_CLOSURE = { value ->
        if (value == null) return value
        if (Collection.class.isAssignableFrom(value.getClass())) {
            def result = []
            value.each() {
                if (it != null && Map.class.isAssignableFrom(it.getClass()) && it.containsKey('_link')) {
                    def v = it['_link']
                    result.add( v.substring(v.lastIndexOf('/') + 1) )
                } else {
                    throw new ShortObjectExtractionException( it )
                }
            }
            return result.toArray()
        } else {
            if (Map.class.isAssignableFrom(value.getClass())) {
                //two possibilities.  Either this map represents an object,
                //or it is a map of objects.  If the map has a _link property, assume it's
                //a single object
                if (value.containsKey('_link')) {
                    def v = value['_link']
                    return [id:v.substring(v.lastIndexOf('/')+1)]
                } else {
                    //assume map of short-objects
                    def result = [:]
                    value.entrySet().each { Map.Entry entry ->
                        if (!entry.value.containsKey('_link')) {
                          throw new ShortObjectExtractionException( value )
                        }
                        def v = entry.value['_link']
                        result.put(entry.key, [id:v.substring(v.lastIndexOf('/')+1)] )
                    }
                    return result
                }
            } else {
                throw new ShortObjectExtractionException( value )
            }
        }
    }

    private DEFAULT_DATE_CLOSURE = { value ->
        if (value == null) return value
        if (Date.class.isAssignableFrom(value.getClass())) {
            return value
        }
        def formats = getDateFormats().clone()
        if (formats.size() == 0) {
            formats.add(new SimpleDateFormat().toPattern())
        }
        for (String format : formats) {
            SimpleDateFormat df = new SimpleDateFormat(format)
            try {
                return df.parse(value.toString())
            } catch (ParseException e) {
                //ignore
            }
        }
        //unable to parse
        throw new DateParseException("$value")
    }

    BasicJSONExtractor() {
    }

    Map extract(JSONObject content) {
        Map map = unwrap(content)
        getTransformer().transform(map)
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
                    getDatePaths().each { def path ->
                        rules.addModifyValueRule(path,getDateClosure())
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
      * Returns a List of String denoting paths whose
      * values should be parsed as dates.
      * The conversion is not lenient - if the value cannot
      * be parsed as a date, an exception is thrown.
      * Parses according to {@link #getDateFormats() getDateFormats},
      * or default SimpleDateFormat if no formats are specified.
      **/
    protected List<List<String>> getDatePaths() {
        []
    }

    /**
     * Returns a list of date formats to parse
     * date values.
     **/
    protected def getDateFormats() {
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

    /**
     * Returns a closure that can conver date representations
     * into Dates.
     **/
    protected Closure getDateClosure() {
        DEFAULT_DATE_CLOSURE
    }

    /**
     * Unwraps JSONObjects, converting any JSONObject.NULL
     * instances to java null.
     **/
    protected def unwrap(def content) {
        if (content == null) return null
        if (content == JSONObject.NULL) return null
        if (content instanceof Map) {
          Map map = [:]
          content.entrySet().each { entry ->
            map[entry.key] = unwrap(entry.value)
          }
          return map
        }
        if (content instanceof Collection) {
          def list = []
          content.each { elt ->
            list.add unwrap(elt)
          }
          return list
        }
        return content
    }
}
