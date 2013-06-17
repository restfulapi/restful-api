/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import spock.lang.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.*
import grails.test.mixin.support.*
import net.hedtech.restfulapi.marshallers.json.*

@TestMixin(GrailsUnitTestMixin)
class JSONExtractorConfigSpec extends Specification {

    def "Test inherits"() {
        setup:
        def src = {
            inherits = ['one','two']
        }

        when:
        def config = invoke(src)

        then:
        ['one','two'] == config.inherits
    }

    def "Test rename"() {
        setup:
        def src = {
            property 'person.name' name 'lastName'
        }

        when:
        def config = invoke(src)

        then:
        ['person.name':'lastName'] == config.dottedRenamedPaths
    }

    def "Test short object"() {
        setup:
        def src = {
            property 'person.name' shortObject()
        }

        when:
        def config = invoke(src)

        then:
        ['person.name'] == config.dottedShortObjectPaths
    }

    def "Test short object explicit true"() {
        setup:
        def src = {
            property 'person.name' shortObject true
        }

        when:
        def config = invoke(src)

        then:
        ['person.name'] == config.dottedShortObjectPaths
    }

    def "Test flattened path"() {
        setup:
        def src = {
            property 'person.name' flatObject()
        }

        when:
        def config = invoke(src)

        then:
        ['person.name'] == config.dottedFlattenedPaths
    }

    def "Test short object explicit true"() {
        setup:
        def src = {
            property 'person.name' flatObject true
        }

        when:
        def config = invoke(src)

        then:
        ['person.name'] == config.dottedFlattenedPaths
    }

    def "Test default value"() {
        setup:
        def src = {
            property "person.name" defaultValue 'Smith'
        }

        when:
        def config = invoke(src)

        then:
        ['person.name':'Smith'] == config.dottedValuePaths
    }

    def "Test short object closure"() {
        setup:
        Closure closure = { def val ->
            return 'foo'
        }
        def src = {
            shortObject closure
        }

        when:
        def config = invoke(src)

        then:
        closure == config.shortObjectClosure
    }

    def "Test repeated property clears previous settings"() {
        setup:
        def src = {
            property 'person.name' name 'lastName' shortObject true flatObject true defaultValue 'Smith'
            property 'person.name'
        }

        when:
        def config = invoke(src)

        then:
        [:] == config.dottedRenamedPaths
        [:] == config.dottedValuePaths
        []  == config.dottedShortObjectPaths
        []  == config.dottedFlattenedPaths
    }

    def "Test merging configurations"() {
        setup:
        def c1 = { def v -> }
        def c2 = { def v -> }
        def one = new JSONExtractorConfig(
            dottedRenamedPaths:['one':'newOne','two':'myTwo'],
            dottedFlattenedPaths:['flat1'],
            dottedValuePaths:['one':'Smith','two':'shoe'],
            dottedShortObjectPaths:['short1'],
            shortObjectClosure:c1
        )
        def two = new JSONExtractorConfig(
            dottedRenamedPaths:['two':'newTwo','three':'newThree'],
            dottedFlattenedPaths:['flat2'],
            dottedValuePaths:['two':'John','three':'Johnson'],
            dottedShortObjectPaths:['short2'],
            shortObjectClosure:c2
        )

        when:
        def config = one.merge(two)

        then:
        ['one':'newOne','two':'newTwo','three':'newThree'] == config.dottedRenamedPaths
        ['flat1','flat2']                                  == config.dottedFlattenedPaths
        ['one':'Smith','two':'John','three':'Johnson']     == config.dottedValuePaths
        ['short1','short2']                                == config.dottedShortObjectPaths
        c2                                                 == config.shortObjectClosure
    }

    def "Test merging configurations does not alter either object"() {
        setup:
        def c1 = { def v -> }
        def c2 = { def v -> }
        def one = new JSONExtractorConfig(
            dottedRenamedPaths:['one':'newOne','two':'myTwo'],
            dottedFlattenedPaths:['flat1'],
            dottedValuePaths:['one':'Smith','two':'shoe'],
            dottedShortObjectPaths:['short1'],
            shortObjectClosure:c1
        )
        def two = new JSONExtractorConfig(
            dottedRenamedPaths:['two':'newTwo','three':'newThree'],
            dottedFlattenedPaths:['flat2'],
            dottedValuePaths:['two':'John','three':'Johnson'],
            dottedShortObjectPaths:['short2'],
            shortObjectClosure:c2
        )

        when:
        def config = one.merge(two)

        then:
        ['one':'newOne','two':'myTwo'] == one.dottedRenamedPaths
        ['flat1']                      == one.dottedFlattenedPaths
        ['one':'Smith','two':'shoe']   == one.dottedValuePaths
        ['short1']                     == one.dottedShortObjectPaths
        c1                             == one.shortObjectClosure

        ['two':'newTwo','three':'newThree'] == two.dottedRenamedPaths
        ['flat2']                           == two.dottedFlattenedPaths
        ['two':'John','three':'Johnson']    == two.dottedValuePaths
        ['short2']                          == two.dottedShortObjectPaths
        c2                                  == two.shortObjectClosure
    }

    def "Test merging with short object closure set only on the left"() {
        setup:
        def c1 = { def v -> }
        def one = new JSONExtractorConfig(shortObjectClosure:c1)
        def two = new JSONExtractorConfig()

        when:
        def config = one.merge(two)

        then:
        c1   == config.shortObjectClosure
        true == config.isShortObjectClosureSet
    }

    private JSONExtractorConfig invoke( Closure c ) {
        JSONExtractorDelegate delegate = new JSONExtractorDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        delegate.config
    }
}