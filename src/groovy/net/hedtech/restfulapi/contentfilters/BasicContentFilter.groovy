/* ***************************************************************************
 * Copyright 2016 Ellucian Company L.P. and its affiliates.
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

package net.hedtech.restfulapi.contentfilters

import net.hedtech.restfulapi.ContentFilter
import net.hedtech.restfulapi.ContentFilterFields
import net.hedtech.restfulapi.ContentFilterResult

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import groovy.xml.XmlUtil

/**
 * A content filter implementation for use with the 'restful-api' plugin.
 **/
class BasicContentFilter implements ContentFilter {


    // must inject restContentFilterFields into this bean
    ContentFilterFields restContentFilterFields


    /**
     * Apply filter to content.
     *
     * Content is usually either a JSON object or an XML document. It may
     * also be the string representation of one of those objects. The form
     * of the filtered content will be the same as the original content.
     **/
    def ContentFilterResult applyFilter(String resourceName, def content, String contentType) {
        assert restContentFilterFields != null
        def startTime = new Date()

        // initialize the result; return immediately if there is no content
        // or if the content type doesn't support filtering
        def result = new ContentFilterResult(content: content)
        if (!isFilterableContent(content, contentType)) {
            log.debug("Returning without filtering as content is not filterable")
            return result
        }

        // retrieve list of field patterns to be filtered; simply return with
        // the original content if there are no field patterns to be filtered
        List fieldPatterns = restContentFilterFields.retrieveFieldPatterns(resourceName)
        if (!fieldPatterns) {
            log.debug("Returning without filtering as no field patterns have been specified")
            return result
        }
        log.debug("Field patterns to be filtered=$fieldPatterns")

        // remove fields from the content; construct a JSON or XML object
        // for ease of filtering from the corresponding String representation
        // if needed - otherwise we will assume we have a Map or List of Maps
        if (content instanceof String) {
            log.debug("Filtering string $contentType content")
            switch(contentType) {
                case "application/json":
                    def structuredContent = new JsonSlurper().parseText(content)
                    result.isPartial = removeFields(fieldPatterns, structuredContent)
                    if (result.isPartial) {
                        result.content = new JsonBuilder(structuredContent).toString()
                    }
                    break
                case "application/xml":
                    def structuredContent = new XmlParser().parseText(content)
                    result.isPartial = removeFields(fieldPatterns, structuredContent)
                    if (result.isPartial) {
                        result.content = XmlUtil.serialize(structuredContent)
                    }
                    break
                default:
                    // we should never get here; throw a runtime exception if we do
                    throw new RuntimeException("Unsupported content type for filtering content: $contentType")
            }
        } else {
            log.debug("Filtering $contentType content")
            result.isPartial = removeFields(fieldPatterns, content)
        }
        log.debug("Filter applied and isPartial=${result.isPartial}")

        // return the result
        log.debug("Elapsed applyFilter time in ms: ${new Date().time - startTime.time}")
        return result
    }


    /**
     * Return true if the content and content type supports filtering.
     **/
    def boolean isFilterableContent(def content, String contentType) {
        return (content &&
                (contentType == "application/json" ||
                 contentType == "application/xml"))
    }


    /**
     * Remove fields from the content.
     *
     * Fields is a list a field names that are to be removed from
     * the content. If content is a list, each field is applied to
     * all items in the list. If content is a map, the field name
     * to be removed may be represented in dot-notation to indicate
     * that the actual field to be removed is in a nested map.
     *
     * You can conditionally remove a field using field==value notation.
     *
     * You can remove an entire list entry if anything in that entry
     * is modified by prefacing the entries first nested element with
     * an '@' character. ex: addresses.@type.addressType==billing
     *
     * Content is either a list of maps, a single map, or an XML document.
     * The value for each field in a map can either be a string or a number,
     * another map, or a list of maps. If the content is an XML document,
     * and the root node is 'list', the children of that node will be treated
     * as a list of nodes.
     **/
    def boolean removeFields(List fields, def content) {
        def isModified = false
        if (content instanceof Map) {
            isModified = removeFieldsFromMap(fields, content)
        } else if (content instanceof List) {
            isModified = removeFieldsFromList(fields, content)
        } else if (content instanceof Node) {
            if (content.name() == "list") {
                isModified = removeFieldsFromList(fields, content.children())
            } else {
                isModified = removeFieldsFromNode(fields, content)
            }
        }
        return isModified
    }


    /**
     * Helper method to remove fields from a map.
     **/
    def boolean removeFieldsFromMap(List fields, Map content) {
        def isModified = false
        log.trace("Examining map with ${content.size()} entries")

        // find all fields that are null before filtering
        def nullFields = []
        content.each { entry ->
            def value = entry.value
            if (value == null ||
                    (value instanceof Map && isEmptyMap(value)) ||
                    (value instanceof List && isEmptyList(value))) {
                def key = entry.key
                log.trace("Null value will be preserved for $key")
                nullFields.add(key)
            }
        }

        // remove fields from each entry in the map
        fields.each { field ->
            def levels = field.tokenize('.')
            def firstLevel = levels[0]
            log.trace("Field levels=${levels.size()} with first level=$firstLevel")

            // test if complete map is to be removed if modified
            def isRemoveIfModified = firstLevel.startsWith('@')
            if (isRemoveIfModified) {
                firstLevel = firstLevel.substring(1)
                log.trace("Will remove complete map if nested match is removed")
            }

            // test for top-level; else we have nested levels
            if (levels.size() == 1) {

                // test if field is to be removed
                // conditional removal is available using field==value notation
                int equalsIndex = firstLevel.indexOf('==')
                if (equalsIndex == -1) {

                    // just check for simple key equality
                    log.trace("Checking simple match for map key=$firstLevel")
                    if (content.containsKey(firstLevel)) {
                        content.remove(firstLevel)
                        log.trace("Removed matching field")
                        isModified = true
                    }
                } else {

                    // test for equality on the value before removal
                    def equalsKey = firstLevel.substring(0,equalsIndex)
                    def equalsValue = firstLevel.substring(equalsIndex+2)
                    log.trace("Checking complex match for map key=$equalsKey with value=$equalsValue")
                    if (equalsKey && equalsValue) {
                        if (equalsValue == content.get(equalsKey)) {
                            content.remove(equalsKey)
                            log.trace("Removed matching field")
                            isModified = true
                        }
                    }
                }
            } else {

                // remove fields from nested content
                def nestedContent = content.get(firstLevel)
                if (nestedContent) {
                    log.trace("Examining match on nested content for $firstLevel")
                    if (removeFields([levels.tail().join('.')], nestedContent)) {
                        isModified = true
                    }
                }
            }

            // test if complete map is to be removed if modified
            if (isRemoveIfModified && isModified) {
                content.clear()
                log.trace("Complete map for nested match is removed")
            }
        }

        // remove any empty map entries
        if (isModified) {
            content.iterator().with { iterator ->
                iterator.each { entry ->
                    def value = entry.value
                    if (value == null ||
                            (value instanceof Map && isEmptyMap(value)) ||
                            (value instanceof List && isEmptyList(value))) {
                        if (!nullFields.contains(entry.key)) {
                            log.trace("Removing empty map entry for ${entry.key}")
                            iterator.remove()
                        }
                    }
                }
            }
        }

        // return true only if map was modified
        log.trace("Map isModified=$isModified")
        return isModified
    }


    /**
     * Helper method to remove fields from a list.
     **/
    def boolean removeFieldsFromList(List fields, List content) {
        def isModified = false
        log.trace("Examining list with ${content.size()} items")

        // remove fields from each item in the list
        content.each { item ->
            if (removeFields(fields, item)) {
                isModified = true
            }
        }

        // remove any empty list items
        if (isModified) {
            content.removeAll { item ->
                if (item == null ||
                        (item instanceof Map && isEmptyMap(item)) ||
                        (item instanceof Node && isEmptyNode(item))) {
                    log.trace("Removing empty list item")
                    true
                }
            }
        }

        // return true only if list was modified
        log.trace("List isModified=$isModified")
        return isModified
    }


    /**
     * Helper method to remove fields from a node.
     **/
    def boolean removeFieldsFromNode(List fields, Node content) {
        def isModified = false
        log.trace("Examining node ${content.name()} with ${content.children().size()} children")

        // find all fields that are null before filtering
        def nullFields = []
        content.each { child ->
            if (child instanceof Node && isEmptyNode(child)) {
                def name = child.name()
                log.trace("Null value will be preserved for $name")
                nullFields.add(name)
            }
        }

        // remove fields from each child in the node
        fields.each { field ->
            def levels = field.tokenize('.')
            def firstLevel = levels[0]
            log.trace("Field levels=${levels.size()} with first level=$firstLevel")

            // test if complete node is to be removed if modified
            def isRemoveIfModified = firstLevel.startsWith('@')
            if (isRemoveIfModified) {
                firstLevel = firstLevel.substring(1)
            }

            // test for top-level; else we have nested levels
            if (levels.size() == 1) {

                // test if field is to be removed
                // conditional removal is available using field==value notation
                int equalsIndex = firstLevel.indexOf('==')
                if (equalsIndex == -1) {

                    // just check for simple node name equality
                    log.trace("Checking simple match for child node name=$firstLevel")
                    def removeNodes = []
                    content.children().each { child ->
                        if (child.name() == firstLevel) {
                            removeNodes.add(child)
                            log.trace("Removed matching field")
                            isModified = true
                        }
                    }
                    removeNodes.each { child ->
                        content.remove(child)
                    }
                } else {

                    // test for equality on the value before removal
                    def equalsKey = firstLevel.substring(0,equalsIndex)
                    def equalsValue = firstLevel.substring(equalsIndex+2)
                    log.trace("Checking complex match for child node name=$equalsKey with value=$equalsValue")
                    if (equalsKey && equalsValue) {
                        def removeNodes = []
                        content.children().each { child ->
                            if (!(child instanceof String) &&
                                    child.name() == equalsKey &&
                                    child.text() == equalsValue) {
                                removeNodes.add(child)
                                log.trace("Removed matching field")
                                isModified = true
                            }
                        }
                        removeNodes.each { child ->
                            content.remove(child)
                        }
                    }
                }
            } else {

                // remove fields from nested content
                content.children().each { child ->
                    if (child.name() == firstLevel) {
                        log.trace("Examining match on nested content for $firstLevel")
                        def nestedFields = [levels.tail().join('.')]
                        if (removeFieldsFromNode(nestedFields, child)) {
                            isModified = true
                        } else {
                            // try removing fields from children list in case this child is a nested list
                            if (removeFieldsFromList(nestedFields, child.children())) {
                                isModified = true
                            }
                        }
                    }
                }
            }

            // test if complete map is to be removed if modified
            if (isRemoveIfModified && isModified) {
                def removeNodes = []
                content.children().each { child ->
                    removeNodes.add(child)
                }
                removeNodes.each { child ->
                    content.remove(child)
                }
                log.trace("Complete node for nested node match is removed")
            }
        }

        // remove any empty child nodes
        if (isModified) {
            content.iterator().with { iterator ->
                iterator.each { child ->
                    if (child instanceof Node && isEmptyNode(child)) {
                        def name = child.name()
                        if (!nullFields.contains(name)) {
                            log.trace("Removing empty child node with name=$name")
                            iterator.remove()
                        }
                    }
                }
            }
        }

        // return true only if node was modified
        log.trace("Node isModified=$isModified")
        return isModified
    }


    /**
     * Return true if a Map is empty.
     **/
    def boolean isEmptyMap(Map map) {
        return (map.size() == 0)
    }


    /**
     * Return true if a List is empty.
     **/
    def boolean isEmptyList(List list) {
        return (list.size() == 0)
    }


    /**
     * Return true if a Node is empty.
     **/
    def boolean isEmptyNode(Node node) {
        return ((node.text() == null || node.text() == "") &&
                node.children().size() == 0 &&
                (node.attributes().size() == 0 ||
                        (node.attributes().size() == 1 &&
                                (node.attributes().get("nill") == "true" ||
                                 node.attributes().get("null") == "true"))))
    }
}
