/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import grails.test.mixin.support.*

import net.hedtech.restfulapi.*
import net.hedtech.restfulapi.beans.*
import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.marshallers.json.*

import spock.lang.*


@TestMixin(GrailsUnitTestMixin)
class JSONGroovyBeanMarshallerConfigSpec extends Specification {

    def "Test inherits"() {
        setup:
        def src = {
            inherits = ['one','two']
        }

        when:
        def config = invoke( src )

        then:
        ['one','two'] == config.inherits

    }

    def "Test priority"() {
        setup:
        def src = {
            priority = 50
        }

        when:
        def config = invoke( src )

        then:
        50 == config.priority
    }

    def "Test supportClass"() {
        setup:
        def src = {
            supports String
        }

        when:
        def config = invoke( src )

        then:
        String == config.supportClass
        true   == config.isSupportClassSet
    }

    def "Test substitutions for field names"() {
        setup:
        def src = {
            field 'one' name 'modOne'
            field 'two' name 'modTwo'
            includesFields {
                field 'three' name 'modThree'
                field 'four' name 'modFour'
            }
        }

        when:
        def config = invoke( src )

        then:
        ['one':'modOne','two':'modTwo','three':'modThree','four':'modFour'] == config.fieldNames
    }

    def "Test included fields"() {
        setup:
        def src = {
            includesFields {
                field 'one'
                field 'two'
            }
        }

        when:
        def config = invoke( src )

        then:
        ['one','two'] == config.includedFields
    }

    def "Test requires included fields"() {
        setup:
        def src = {
            includesFields {
                requiresIncludedFields true
            }
        }

        when:
        def config = invoke( src )

        then:
        true == config.requireIncludedFields
    }

    def "Test excluded fields"() {
        setup:
        def src = {
            excludesFields {
                field 'one'
                field 'two'
            }
        }

        when:
        def config = invoke( src )

        then:
        ['one','two'] == config.excludedFields
    }

    def "Test additional field closures"() {
        setup:
        def storage = []
        def src = {
            additionalFields {
                Map m -> storage.add 'one'
            }
            additionalFields {
                Map m -> storage.add 'two'
            }
        }

        when:
        def config = invoke( src )
        config.additionalFieldClosures.each {
            it.call([:])
        }

        then:
        2             == config.additionalFieldClosures.size()
        ['one','two'] == storage
    }

    def "Test additionalFieldsMap"() {
        setup:
        def src = {
            additionalFieldsMap = ['one':'one','two':'two']
        }

        when:
        def config = invoke( src )

        then:
        [one:'one',two:'two'] == config.additionalFieldsMap
    }

    def "Test merging configurations"() {
        setup:
        def c1 = { Map m -> }
        def c2 = { Map m -> }
        JSONGroovyBeanMarshallerConfig one = new JSONGroovyBeanMarshallerConfig(
            supportClass:SimpleBean,
            fieldNames:['foo':'foo1','bar':'bar1'],
            includedFields:['foo','bar'],
            excludedFields:['e1','e2'],
            additionalFieldClosures:[{app,bean,json ->}],
            additionalFieldsMap:['one':'one','two':'two'],
            requireIncludedFields:true
        )
        JSONGroovyBeanMarshallerConfig two = new JSONGroovyBeanMarshallerConfig(
            supportClass:Thing,
            fieldNames:['foo':'foo2','baz':'baz1'],
            includedFields:['baz'],
            excludedFields:['e3'],
            additionalFieldClosures:[{app,bean,json ->}],
            additionalFieldsMap:['two':'2','three':'3'],
            requireIncludedFields:false
        )

        when:
        def config = one.merge(two)

        then:
        true                                     == config.isSupportClassSet
        Thing                                    == config.supportClass
        ['foo':'foo2','bar':'bar1','baz':'baz1'] == config.fieldNames
        ['foo','bar','baz']                      == config.includedFields
        ['e1','e2','e3']                         == config.excludedFields
        2                                        == config.additionalFieldClosures.size()
        ['one':'one',"two":'2','three':'3']      == config.additionalFieldsMap
        false                                    == config.requireIncludedFields
    }

    def "Test merging configurations does not alter either object"() {
        setup:
        def c1 = { Map m -> }
        def c2 = { Map m -> }
        JSONGroovyBeanMarshallerConfig one = new JSONGroovyBeanMarshallerConfig(
            supportClass:Thing,
            fieldNames:['foo':'foo1','bar':'bar1'],
            includedFields:['foo','bar'],
            excludedFields:['e1','e2'],
            additionalFieldClosures:[{app,bean,json ->}],
            additionalFieldsMap:['one':'1'],
            requireIncludedFields:true
        )
        JSONGroovyBeanMarshallerConfig two = new JSONGroovyBeanMarshallerConfig(
            supportClass:PartOfThing,
            fieldNames:['foo':'foo2','baz':'baz1'],
            includedFields:['baz'],
            excludedFields:['e3'],
            additionalFieldClosures:[{app,bean,json ->}],
            additionalFieldsMap:['two':'2'],
            requireIncludedFields:false
        )

        when:
        one.merge(two)

        then:
        true                        == one.isSupportClassSet
        ['foo':'foo1','bar':'bar1'] == one.fieldNames
        ['foo','bar']               == one.includedFields
        ['e1','e2']                 == one.excludedFields
        1                           == one.additionalFieldClosures.size()
        ['one':'1']                 == one.additionalFieldsMap
        true                        == one.requireIncludedFields

        true                        == two.isSupportClassSet
        ['foo':'foo2','baz':'baz1'] == two.fieldNames
        ['baz']                     == two.includedFields
        ['e3']                      == two.excludedFields
        1                           == two.additionalFieldClosures.size()
        ['two':'2']                 == two.additionalFieldsMap
        false                       == two.requireIncludedFields
    }

    def "Test merging with support class set only on the left"() {
        setup:
        def c1 = { Map m -> }
        def c2 = { Map m -> }
        JSONGroovyBeanMarshallerConfig one = new JSONGroovyBeanMarshallerConfig(
            supportClass:SimpleBean
        )
        JSONGroovyBeanMarshallerConfig two = new JSONGroovyBeanMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        SimpleBean == config.supportClass
        true       == config.isSupportClassSet
    }

    def "Test merging marshaller with require included fields set only on the left"() {
        setup:
        def c1 = { Map m -> }
        JSONGroovyBeanMarshallerConfig one = new JSONGroovyBeanMarshallerConfig(
            requireIncludedFields:true
        )
        JSONGroovyBeanMarshallerConfig two = new JSONGroovyBeanMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        true == config.requireIncludedFields
    }


    def "Test resolution of marshaller configuration inherits"() {
        setup:
        JSONGroovyBeanMarshallerConfig part1 = new JSONGroovyBeanMarshallerConfig(
        )
        JSONGroovyBeanMarshallerConfig part2 = new JSONGroovyBeanMarshallerConfig(
        )
        JSONGroovyBeanMarshallerConfig part3 = new JSONGroovyBeanMarshallerConfig(
        )
        JSONGroovyBeanMarshallerConfig combined = new JSONGroovyBeanMarshallerConfig(
            inherits:['part1','part2']
        )
        JSONGroovyBeanMarshallerConfig actual = new JSONGroovyBeanMarshallerConfig(
            inherits:['combined','part3']
        )
        ConfigGroup group = new ConfigGroup()
        group.configs = ['part1':part1,'part2':part2,'part3':part3,'combined':combined]

        when:
        def resolvedList = group.resolveInherited( actual )

        then:
        [part1,part2,combined,part3,actual] == resolvedList
    }

    def "Test merge order of configuration inherits"() {
        setup:
        JSONGroovyBeanMarshallerConfig part1 = new JSONGroovyBeanMarshallerConfig(
            fieldNames:['1':'part1','2':'part1','3':'part1']
        )
        JSONGroovyBeanMarshallerConfig part2 = new JSONGroovyBeanMarshallerConfig(
            fieldNames:['2':'part2','3':'part2']

        )
        JSONGroovyBeanMarshallerConfig actual = new JSONGroovyBeanMarshallerConfig(
            inherits:['part1','part2'],
            fieldNames:['3':'actual']
        )
        ConfigGroup group = new ConfigGroup()
        group.configs = ['part1':part1,'part2':part2]

        when:
        def config = group.getMergedConfig( actual )

        then:
        ['1':'part1','2':'part2','3':'actual'] == config.fieldNames
    }

    def "Test repeated field clears previous settings"() {
        setup:
        def src = {
            field 'one' name 'modOne'
            field 'one'
            field 'two' name 'modTwo'
            includesFields {
                field 'two'
            }
        }

        when:
        def config = invoke( src )

        then:
        [:] == config.fieldNames
    }

    private JSONGroovyBeanMarshallerConfig invoke( Closure c ) {
        JSONGroovyBeanMarshallerDelegate delegate = new JSONGroovyBeanMarshallerDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        delegate.config
    }
}
