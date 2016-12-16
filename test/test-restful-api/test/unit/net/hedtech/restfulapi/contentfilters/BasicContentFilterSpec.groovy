/* ****************************************************************************
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

import groovy.json.JsonSlurper

import grails.test.mixin.*
import grails.test.mixin.support.*

import net.hedtech.restfulapi.*
import spock.lang.*


@TestMixin(GrailsUnitTestMixin)
class BasicContentFilterSpec extends Specification {

    ContentFilter restContentFilter

    private final String APPLICATION_JSON = "application/json"
    private final String APPLICATION_XML = "application/xml"
    private final String NO_CONTENT_TYPE = ""

    private final boolean IS_PARTIAL = true

    void setup() {
        cleanup()
        restContentFilter = new BasicContentFilter()
        restContentFilter.restContentFilterFields = new BasicContentFilterFields()
        restContentFilter.restContentFilterFields.grailsApplication = grailsApplication
    }

    void cleanup() {
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap = null
    }

    def "Test unfilterable content"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description"]
                ]

        def text = "description"

        when:
        ContentFilterResult textResult = restContentFilter.applyFilter(
                "my-resource",
                text,
                NO_CONTENT_TYPE)

        then:
        compareResults(textResult, !IS_PARTIAL, text, NO_CONTENT_TYPE)
    }

    def "Test no field patterns"() {
        setup:

        def jsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
}"""

        when:
        ContentFilterResult textResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)

        then:
        compareResults(textResult, !IS_PARTIAL, jsonText, APPLICATION_JSON)
    }

    def "Test using simple filter with one change"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description"]
                ]

        def jsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
}"""
        def xmlText = """
<object>
    <code>201410</code>
    <description>Fall 2013</description>
    <status>Active</status>
</object>"""

        def filteredJsonText = """
{ "code": "201410",
  "status": "Active"
}"""
        def filteredXmlText = """
<object>
    <code>201410</code>
    <status>Active</status>
</object>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test using simple filter with no changes"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["name"]
                ]

        def jsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
}"""
        def xmlText = """
<object>
    <code>201410</code>
    <description>Fall 2013</description>
    <status>Active</status>
</object>"""

        def filteredJsonText = jsonText
        def filteredXmlText = xmlText

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, !IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, !IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, !IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, !IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test using simple filter with multiple changes"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description", "code"]
                ]

        def jsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
}"""
        def xmlText = """
<object>
    <code>201410</code>
    <description>Fall 2013</description>
    <status>Active</status>
</object>"""

        def filteredJsonText = """
{ "status": "Active"
}"""
        def filteredXmlText = """
<object>
    <status>Active</status>
</object>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test using simple filter with empty result"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description", "status", "code"]
                ]

        def jsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
}"""
        def xmlText = """
<object>
    <code>201410</code>
    <description>Fall 2013</description>
    <status>Active</status>
</object>"""

        def filteredJsonText = """{}"""
        def filteredXmlText = """<object></object>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list of unfilterable content"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description"]
                ]

        def textList = ["description"]

        when:
        ContentFilterResult textResult = restContentFilter.applyFilter(
                "my-resource",
                textList,
                NO_CONTENT_TYPE)

        then:
        compareResults(textResult, !IS_PARTIAL, textList, NO_CONTENT_TYPE)
    }

    def "Test list using simple filter with one change"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410",
  "status": "Active"
},
{ "code": "201510",
  "status": "Inactive"
},
{ "code": "201610",
  "status": "Active"
}
]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <status>Active</status>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using simple filter with no changes"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["name"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = jsonText
        def filteredXmlText = xmlText

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, !IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, !IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, !IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, !IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using simple filter with multiple changes"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description", "code"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "status": "Active"
},
{ "status": "Inactive"
},
{ "status": "Active"
}
]"""
        def filteredXmlText = """
<list>
    <object>
        <status>Active</status>
    </object>
    <object>
        <status>Inactive</status>
    </object>
    <object>
        <status>Active</status>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using simple filter with empty result"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description", "status", "code"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """[]"""
        def filteredXmlText = """<list></list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test using nested filter with one change"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["detail.description"]
                ]

        def jsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "detail": {"code": "S", "description": "Standard Semester"},
  "status": "Active"
}"""
        def xmlText = """
<object>
    <code>201410</code>
    <description>Fall 2013</description>
    <detail>
        <code>S</code>
        <description>Standard Semester</description>
    </detail>
    <status>Active</status>
</object>"""

        def filteredJsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "detail": {"code": "S"},
  "status": "Active"
}"""
        def filteredXmlText = """
<object>
    <code>201410</code>
    <description>Fall 2013</description>
    <detail>
        <code>S</code>
    </detail>
    <status>Active</status>
</object>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test using nested filter with multiple changes"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description", "detail.description"]
                ]

        def jsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "detail": {"code": "S", "description": "Standard Semester"},
  "status": "Active"
}"""
        def xmlText = """
<object>
    <code>201410</code>
    <description>Fall 2013</description>
    <detail>
        <code>S</code>
        <description>Standard Semester</description>
    </detail>
    <status>Active</status>
</object>"""

        def filteredJsonText = """
{ "code": "201410",
  "detail": {"code": "S"},
  "status": "Active"
}"""
        def filteredXmlText = """
<object>
    <code>201410</code>
    <detail>
        <code>S</code>
    </detail>
    <status>Active</status>
</object>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test using nested filter removing empty elements"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description", "status", "detail.code", "detail.description"]
                ]

        def jsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "detail": {"code": "S", "description": "Standard Semester"},
  "status": "Active"
}"""
        def xmlText = """
<object>
    <code>201410</code>
    <description>Fall 2013</description>
    <detail>
        <code>S</code>
        <description>Standard Semester</description>
    </detail>
    <status>Active</status>
</object>"""

        def filteredJsonText = """
{ "code": "201410"
}"""
        def filteredXmlText = """
<object>
    <code>201410</code>
</object>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test using simple filter removing whole element"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["detail"]
                ]

        def jsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "detail": {"code": "S", "description": "Standard Semester"},
  "status": "Active"
}"""
        def xmlText = """
<object>
    <code>201410</code>
    <description>Fall 2013</description>
    <detail>
        <code>S</code>
        <description>Standard Semester</description>
    </detail>
    <status>Active</status>
</object>"""

        def filteredJsonText = """
{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
}"""
        def filteredXmlText = """
<object>
    <code>201410</code>
    <description>Fall 2013</description>
    <status>Active</status>
</object>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using nested filter with one change"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["details.description"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S"},
             {"code": "E"}],
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "details": [{"code": "S"},
             {"code": "E"}],
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "details": [{"code": "S"},
             {"code": "E"}],
  "status": "Active"
}
]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
            </detail>
            <detail>
                <code>E</code>
            </detail>
        </details>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <details>
            <detail>
                <code>S</code>
            </detail>
            <detail>
                <code>E</code>
            </detail>
        </details>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <details>
            <detail>
                <code>S</code>
            </detail>
            <detail>
                <code>E</code>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using nested filter with multiple changes"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description", "details.description"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410",
  "details": [{"code": "S"},
             {"code": "E"}],
  "status": "Active"
},
{ "code": "201510",
  "details": [{"code": "S"},
             {"code": "E"}],
  "status": "Inactive"
},
{ "code": "201610",
  "details": [{"code": "S"},
             {"code": "E"}],
  "status": "Active"
}
]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
        <details>
            <detail>
                <code>S</code>
            </detail>
            <detail>
                <code>E</code>
            </detail>
        </details>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <details>
            <detail>
                <code>S</code>
            </detail>
            <detail>
                <code>E</code>
            </detail>
        </details>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <details>
            <detail>
                <code>S</code>
            </detail>
            <detail>
                <code>E</code>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using nested filter removing empty elements"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description", "status", "details.code", "details.description"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410"
},
{ "code": "201510"
},
{ "code": "201610"
}
]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
    </object>
    <object>
        <code>201510</code>
    </object>
    <object>
        <code>201610</code>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using simple filter removing whole element"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["details"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "status": "Active"
}
]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <status>Active</status>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using simple filter with one equality change"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description==Fall 2014"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "status": "Active"
},
{ "code": "201510",
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "status": "Active"
}
]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <status>Active</status>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using mixed filter with multiple equality changes removing whole element"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description==Fall 2014",
                                        "details.description==Extra Semester",
                                        "details.description==Standard Semester",
                                        "details.code==E"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S"}],
  "status": "Active"
},
{ "code": "201510",
  "details": [{"code": "S"}],
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "details": [{"code": "S"}],
  "status": "Active"
}
]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
            </detail>
        </details>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <details>
            <detail>
                <code>S</code>
            </detail>
        </details>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <details>
            <detail>
                <code>S</code>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using mixed filter with multiple equality changes removing all nested elements"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["description==Fall 2014",
                                        "details.description==Extra Semester",
                                        "details.description==Standard Semester",
                                        "details.code==E",
                                        "details.code==S",
                                        "description==Fall 2015",
                                        "description==Fall 2013"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410",
  "status": "Active"
},
{ "code": "201510",
  "status": "Inactive"
},
{ "code": "201610",
  "status": "Active"
}
]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <status>Inactive</status>
    </object>
    <object>
        <code>201610</code>
        <status>Active</status>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using mixed filter with multiple equality changes removing whole list entry"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["details.@type.value==secondary"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester", "type": {"value": "primary"}},
             {"code": "E", "description": "Extra Semester", "type": {"value": "secondary"}}],
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
                <type>
                    <value>primary</value>
                </type>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
                <type>
                    <value>secondary</value>
                </type>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester", "type": {"value": "primary"}}],
  "status": "Active"
}]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
                <type>
                    <value>primary</value>
                </type>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test list using mixed filter with one top-level equality change removing whole list entry"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["details.@code==E"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester"},
             {"code": "E", "description": "Extra Semester"}],
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester"}],
  "status": "Active"
}]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
            </detail>
        </details>
        <status>Active</status>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

    def "Test preservation of existing null elements"() {
        setup:
        grailsApplication.config.restfulApi.contentFilter.fieldPatternsMap =
                [
                        "my-resource": ["status", "details.code", "details.description"]
                ]

        def jsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"code": "S", "description": "Standard Semester", "language": null,},
             {"code": "E", "description": "Extra Semester"}],
  "language": null,
  "status": "Active"
},
{ "code": "201510",
  "description": "Fall 2014",
  "details": [{"code": "S", "description": "Standard Semester", "language": "English"},
             {"code": "E", "description": "Extra Semester"}],
  "language": "English",
  "status": "Inactive"
},
{ "code": "201610",
  "description": "Fall 2015",
  "details": [{"code": "S", "description": "Standard Semester", "language": null,},
             {"code": "E", "description": "Extra Semester"}],
  "language": null,
  "status": "Active"
}]"""
        def xmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
                <language nil="true"/>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <language nil="true"/>
        <status>Active</status>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
                <language>English</language>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <status>Inactive</status>
        <language>English</language>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <details>
            <detail>
                <code>S</code>
                <description>Standard Semester</description>
                <language nil="true"/>
            </detail>
            <detail>
                <code>E</code>
                <description>Extra Semester</description>
            </detail>
        </details>
        <language null="true"/>
        <status>Active</status>
    </object>
</list>"""

        def filteredJsonText = """
[{ "code": "201410",
  "description": "Fall 2013",
  "details": [{"language": null}],
  "language": null
},
{ "code": "201510",
  "description": "Fall 2014",
  "details": [{"language": "English"}],
  "language": "English"
},
{ "code": "201610",
  "description": "Fall 2015",
  "details": [{"language": null}],
  "language": null
}
]"""
        def filteredXmlText = """
<list>
    <object>
        <code>201410</code>
        <description>Fall 2013</description>
        <details>
            <detail>
                <language nil="true"/>
            </detail>
        </details>
        <language nil="true"/>
    </object>
    <object>
        <code>201510</code>
        <description>Fall 2014</description>
        <details>
            <detail>
                <language>English</language>
            </detail>
        </details>
        <language>English</language>
    </object>
    <object>
        <code>201610</code>
        <description>Fall 2015</description>
        <details>
            <detail>
                <language nil="true"/>
            </detail>
        </details>
        <language null="true"/>
    </object>
</list>"""

        when:
        ContentFilterResult jsonTextResult = restContentFilter.applyFilter(
                "my-resource",
                jsonText,
                APPLICATION_JSON)
        ContentFilterResult jsonObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToJsonObject(jsonText),
                APPLICATION_JSON)
        ContentFilterResult xmlTextResult = restContentFilter.applyFilter(
                "my-resource",
                xmlText,
                APPLICATION_XML)
        ContentFilterResult xmlObjectResult = restContentFilter.applyFilter(
                "my-resource",
                convertTextToXmlObject(xmlText),
                APPLICATION_XML)

        then:
        compareResults(jsonTextResult, IS_PARTIAL, filteredJsonText, APPLICATION_JSON)
        compareResults(jsonObjectResult, IS_PARTIAL, convertTextToJsonObject(filteredJsonText), APPLICATION_JSON)
        compareResults(xmlTextResult, IS_PARTIAL, filteredXmlText, APPLICATION_XML)
        compareResults(xmlObjectResult, IS_PARTIAL, convertTextToXmlObject(filteredXmlText), APPLICATION_XML)
    }

// PRIVATE METHODS START HERE

    def convertTextToJsonObject(String jsonText) {
        return new JsonSlurper().parseText(jsonText)
    }

    def convertTextToXmlObject(String xmlText) {
        return new XmlParser().parseText(xmlText)
    }

    def void compareResults(ContentFilterResult result, boolean expectedIsPartial, def expectedContent, String contentType) {
        // test for matching partial content indicator
        if (expectedIsPartial && expectedIsPartial != result.isPartial) {
            fail "Expected partial content"
        }
        if (!expectedIsPartial && expectedIsPartial != result.isPartial) {
            fail "Expected no changes to content"
        }

        // test for matching content class
        def actualContent = result.content
        def actualContentClass = actualContent.getClass().getName()
        def expectedContentClass = expectedContent.getClass().getName()
        if (actualContentClass != expectedContentClass) {
            fail "Expected content class $expectedContentClass does not match actual content class $actualContentClass"
        }

        // test for matching content
        compareContent(actualContent, expectedContent, contentType)
    }

    def void compareContent(def actualContent, def expectedContent, String contentType) {
        if (actualContent instanceof String) {
            compareStringResults(actualContent, expectedContent, contentType)
        } else if (actualContent instanceof Map) {
            compareMapResults(actualContent, expectedContent, contentType)
        } else if (actualContent instanceof List) {
            compareListResults(actualContent, expectedContent, contentType)
        } else if (actualContent instanceof Node) {
            compareNodeResults(actualContent, expectedContent, contentType)
        } else {
            fail "Unexpected result content is a ${actualContent.getClass().getName()}"
        }
    }

    def void compareStringResults(String actualContent, String expectedContent, String contentType) {
        if (contentType == APPLICATION_JSON) {
            compareContent(convertTextToJsonObject(actualContent), convertTextToJsonObject(expectedContent), contentType)
        } else if (contentType == APPLICATION_XML) {
            compareContent(convertTextToXmlObject(actualContent), convertTextToXmlObject(expectedContent), contentType)
        } else {
            if (actualContent != expectedContent) {
                fail "Expected content $expectedContent does not match actual content $actualContent"
            }
        }
    }

    def void compareMapResults(Map actualContent, Map expectedContent, String contentType) {
        // direct map comparison is natively supported
        if (actualContent != expectedContent) {
            fail "Expected content $expectedContent does not match actual content $actualContent"
        }
    }

    def void compareListResults(List actualContent, List expectedContent, String contentType) {
        // compare size of list
        int actualListSize = actualContent.size()
        int expectedListSize = expectedContent.size()
        if (actualListSize != expectedListSize) {
            fail "Expected content list size $expectedListSize does not match actual content list size $actualListSize"
        }

        // compare each entry in the list
        for (int i=0; i<actualListSize; i++) {
            def actualEntry = actualContent.get(i)
            def expectedEntry = expectedContent.get(i)
            def actualEntryClass = actualEntry.getClass().getName()
            def expectedEntryClass = expectedEntry.getClass().getName()
            if (actualEntryClass != expectedEntryClass) {
                fail "Expected content list entry class $expectedEntryClass does not match actual content list entry class $actualEntryClass"
            }

            // delegate to compare content for each child entry
            compareContent(actualEntry, expectedEntry, contentType)
        }
    }

    def void compareNodeResults(Node actualContent, Node expectedContent, String contentType) {
        // compare node name
        def actualNodeName = actualContent.name()
        def expectedNodeName = expectedContent.name()
        if (actualNodeName != expectedNodeName) {
            fail "Expected node name $expectedNodeName does not match actual node name $actualNodeName"
        }

        if (actualContent.name() == "list") {
            // special handling required for lists
            compareListResults(actualContent.children(), expectedContent.children(), contentType)
        } else {
            // compare node value
            def actualValue = actualContent.text()
            def expectedValue = expectedContent.text()
            if (actualValue != expectedContent.text()) {
                fail "Expected node value $expectedValue does not match actual node value $actualValue"
            }

            // compare node children
            compareListResults(actualContent.children(), expectedContent.children(), NO_CONTENT_TYPE)

            // compare node attributes
            compareMapResults(actualContent.attributes(), expectedContent.attributes(), NO_CONTENT_TYPE)
        }
    }

}
