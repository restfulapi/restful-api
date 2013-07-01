/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.extractors.xml

import grails.converters.XML
import grails.test.mixin.*
import grails.test.mixin.web.*
import spock.lang.*
import grails.test.mixin.support.*

@TestMixin([GrailsUnitTestMixin])
class DeclarativeXMLExtractorSpec extends Specification {

    def "Test rename paths"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedRenamedPaths:['outer.inner':'renamed','name':'lastName']
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.renameRules

        then:
        [['outer','inner']:'renamed',
         ['name']:'lastName'] == rules
    }

    def "Test default value paths"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedValuePaths:['outer.inner':true, 'name':'Smith']
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.defaultValueRules

        then:
        [['outer','inner']:true,
         ['name']:'Smith'] == rules
         [:] == extractor.transformer.rules.renameRules
    }

    def "Test default short object paths"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedShortObjectPaths:['outer.inner','customer']
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:BasicXMLExtractor.DEFAULT_SHORT_OBJECT_CLOSURE,
         ['customer']:BasicXMLExtractor.DEFAULT_SHORT_OBJECT_CLOSURE] == rules
    }

    def "Test overridden short object paths"() {
        setup:
        Closure shortObject = { def value -> }
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedShortObjectPaths:['outer.inner','customer'],
            shortObjectClosure:shortObject
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:shortObject,
         ['customer']:shortObject] == rules
    }

    def "Test flatten paths"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedFlattenedPaths:['outer.inner','customer']
        )

        when:
        extractor.ready()
        def rules = extractor.transformer.rules.flattenPaths

        then:
        [['outer','inner'],['customer']] == rules
    }

    def "Test default short object closure for singletons"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedShortObjectPaths:['customer']
        )
        def xml = XML.parse """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customer>
                <_link>/customers/1234</_link>
            </customer>
        </thing>"""

        when:
        def map = extractor.extract(xml)

        then:
        [customer:[id:'1234']] == map
    }


    def "Test default short object closure for collections"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedShortObjectPaths:['customers']
        )
        def xml = XML.parse """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers array="true">
                <shortObject><_link>/customers/1</_link></shortObject>
                <shortObject><_link>/customers/2</_link></shortObject>
            </customers>
        </thing>"""

        when:
        def map = extractor.extract(xml)

        then:
        [customers:['1','2']] == map
        true                  == map['customers'].getClass().isArray()
    }

    def "Test default short object closure for maps"() {
        setup:
        DeclarativeXMLExtractor extractor = new DeclarativeXMLExtractor(
            dottedShortObjectPaths:['customers']
        )
        def xml = XML.parse """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true">
                <entry key="smith">
                    <_link>/customers/1</_link>
                </entry>
                <entry key="johnson">
                    <_link>/customers/2</_link>
                </entry>
            </customers>
        </thing>"""

        when:
        def map = extractor.extract(xml)

        then:
        [customers:['smith':['id':'1'],'johnson':['id':'2']]] == map
    }
}