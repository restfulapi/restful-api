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
package net.hedtech.restfulapi.extractors.xml

import net.hedtech.restfulapi.extractors.*

class DeclarativeXMLExtractor extends BasicXMLExtractor {

    Map<String,String> dottedRenamedPaths = [:]
    Map<String,Object> dottedValuePaths = [:]
    List<String> dottedShortObjectPaths = []
    List<String> dottedFlattenedPaths = []
    List<String> dottedDatePaths = []
    Closure shortObjectClosure
    List<String> dateFormats = []

    /**
     * Returns a map of rename rules.  The keys
     * are List of String denoting paths to keys in the map which
     * should be renamed as the value the path is mapped to.
     */
    @Override
    protected Map<List<String>,String> getRenamePaths() {
        def result = [:]
        dottedRenamedPaths.entrySet().each { Map.Entry entry ->
            result.put(parse(entry.key),entry.value)
        }
        result
    }

    /**
     * Returns a map of default value rules.  The keys
     * are List of String denoting paths to keys in the map
     * which should have the default value if the key is not
     * already present.
     */
    @Override
    protected Map<List<String>,Object> getDefaultValuePaths() {
        def result = [:]
        dottedValuePaths.entrySet().each { Map.Entry entry ->
            result.put(parse(entry.key),entry.value)
        }
        result
    }

    /**
     * Returns a List of String denoting paths whose
     * values should be treated as short objects
     * and converted according the the short object closure.
     **/
    @Override
    protected List<List<String>> getShortObjectPaths() {
        def result = []
        dottedShortObjectPaths.each { String path ->
            result.add(parse(path))
        }
        result
    }

    /**
     * Returns a List of String denoting paths whose
     * values should be flattened into the containing map.
     **/
    @Override
     protected List<List<String>> getFlattenPaths() {
        def result = []
        dottedFlattenedPaths.each { String path ->
            result.add(parse(path))
        }
        result
    }

    /**
      * Returns a List of String denoting paths whose
      * values should be parsed as dates.
      * The conversion is not lenient - if the value cannot
      * be parsed as a date, an exception is thrown.
      * Parses according to {@link #getDateFormats() getDateFormats},
      * or default SimpleDateFormat if no formats are specified.
      **/
    @Override
    protected List<List<String>> getDatePaths() {
        def result = []
        dottedDatePaths.each { String path ->
            result.add(parse(path))
        }
        result
    }

    /**
     * Returns a list of date formats to parse
     * date values.
     **/
    @Override
    protected List<String> getDateFormats() {
        dateFormats.clone()
    }

    /**
     * Returns a closure that can convert a 'short object'
     * representation to a map containing the id represented
     * by the short object reference.
     */
    @Override
    protected Closure getShortObjectClosure() {
        shortObjectClosure == null ? super.getShortObjectClosure() : shortObjectClosure
    }

    protected List<String> parse(String path) {
        path.split("\\.")
    }
}
