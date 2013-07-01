/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.extractors.xml

import grails.test.mixin.*
import grails.test.mixin.web.*
import spock.lang.*
import grails.test.mixin.support.*
import grails.converters.XML

@TestMixin([GrailsUnitTestMixin])
class BasicXMLExtractorSpec extends Specification {

    def "Test rename paths"() {
        setup:
        BasicXMLExtractor.metaClass.getRenamePaths << {
            [['outer','inner']:'renamed',
             ['name']:'lastName']
        }

        when:
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def rules = extractor.transformer.rules.renameRules

        then:
        [['outer','inner']:'renamed',
         ['name']:'lastName'] == rules
    }

    def "Test default value paths"() {
        setup:
        BasicXMLExtractor.metaClass.getDefaultValuePaths << {
            [['outer','inner']:true,
             ['name']:'Smith']
        }

        when:
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def rules = extractor.transformer.rules.defaultValueRules

        then:
        [['outer','inner']:true,
         ['name']:'Smith'] == rules
         [:] == extractor.transformer.rules.renameRules
    }

    def "Test default short object paths"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['outer','inner'],['customer']]
        }

        when:
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:BasicXMLExtractor.DEFAULT_SHORT_OBJECT_CLOSURE,
         ['customer']:BasicXMLExtractor.DEFAULT_SHORT_OBJECT_CLOSURE] == rules
    }

    def "Test overridden short object paths"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['outer','inner'],['customer']]
        }
        Closure shortObject = { def value -> }
        BasicXMLExtractor.metaClass.getShortObjectClosure << {
            shortObject
        }

        when:
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def rules = extractor.transformer.rules.modifyValueRules

        then:
        [['outer','inner']:shortObject,
         ['customer']:shortObject] == rules
    }

    def "Test flatten paths"() {
        setup:
        BasicXMLExtractor.metaClass.getFlattenPaths << {
            [['outer','inner'],['customer']]
        }

        when:
        BasicXMLExtractor extractor = new BasicXMLExtractor()
        def rules = extractor.transformer.rules.flattenPaths

        then:
        [['outer','inner'],['customer']] == rules
    }

    def "Test default short object closure for singletons"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['customer']]
        }
        def content = '<?xml version="1.0" encoding="UTF-8"?><thing><customer><_link>/customers/1234</_link></customer></thing>'
        def xml = XML.parse content
        BasicXMLExtractor extractor = new BasicXMLExtractor()

        when:
        def map = extractor.extract(xml)

        then:
        [customer:[id:'1234']] == map
    }


    def "Test default short object closure for collections"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['customers']]
        }
        def content = '<?xml version="1.0" encoding="UTF-8"?><thing><customers array="true"><shortObject><_link>/customers/1</_link></shortObject><shortObject><_link>/customers/2</_link></shortObject></customers></thing>'
        def xml = XML.parse content
        BasicXMLExtractor extractor = new BasicXMLExtractor()

        when:
        def map = extractor.extract(xml)

        then:
        [customers:['1','2']] == map
    }

    def "Test default short object closure for maps"() {
        setup:
        BasicXMLExtractor.metaClass.getShortObjectPaths << {
            [['customers']]
        }
        def content = """<?xml version="1.0" encoding="UTF-8"?>
        <thing>
            <customers map="true">
                <entry key="smith"><_link>/customers/1</_link></entry>
                <entry key="johnson"><_link>/customers/2</_link></entry>
            </customers>
        </thing>"""
        def xml = XML.parse content
        BasicXMLExtractor extractor = new BasicXMLExtractor()

        when:
        def map = extractor.extract(xml)

        then:
        [customers:['smith':['id':'1'],'johnson':['id':'2']]] == map
    }
}