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

package net.hedtech.restfulapi.config

import grails.test.mixin.*
import grails.test.mixin.support.*

import net.hedtech.restfulapi.*
import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.marshallers.json.*

import spock.lang.*


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

    def "Test flat object explicit true"() {
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

    def "Test lenient dates"() {
        setup:
        def src = {
            lenientDates = true
        }

        when:
        def config = invoke(src)

        then:
        true == config.lenientDates
    }

    def "Test repeated property clears previous settings"() {
        setup:
        def src = {
            property 'person.name' name 'lastName' shortObject true flatObject true defaultValue 'Smith'
            property 'person.birthday' date true
            property 'person.name'
            property 'person.birthday'
        }

        when:
        def config = invoke(src)

        then:
        [:] == config.dottedRenamedPaths
        [:] == config.dottedValuePaths
        []  == config.dottedShortObjectPaths
        []  == config.dottedFlattenedPaths
        []  == config.dottedDatePaths
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
            shortObjectClosure:c1,
            dottedDatePaths:['date1'],
            dateFormats:['yyyy.MM.dd'],
            lenientDates: false

        )
        def two = new JSONExtractorConfig(
            dottedRenamedPaths:['two':'newTwo','three':'newThree'],
            dottedFlattenedPaths:['flat2'],
            dottedValuePaths:['two':'John','three':'Johnson'],
            dottedShortObjectPaths:['short2'],
            shortObjectClosure:c2,
            dottedDatePaths:['date2'],
            dateFormats:['yyyy/MM/dd'],
            lenientDates: true
        )

        when:
        def config = one.merge(two)

        then:
        ['one':'newOne','two':'newTwo','three':'newThree'] == config.dottedRenamedPaths
        ['flat1','flat2']                                  == config.dottedFlattenedPaths
        ['one':'Smith','two':'John','three':'Johnson']     == config.dottedValuePaths
        ['short1','short2']                                == config.dottedShortObjectPaths
        c2                                                 == config.shortObjectClosure
        ['date1','date2']                                  == config.dottedDatePaths
        ['yyyy.MM.dd','yyyy/MM/dd']                        == config.dateFormats
        true                                               == config.lenientDates
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
            shortObjectClosure:c1,
            dottedDatePaths:['date1'],
            dateFormats:['yyyy.MM.dd'],
            lenientDates:false
        )
        def two = new JSONExtractorConfig(
            dottedRenamedPaths:['two':'newTwo','three':'newThree'],
            dottedFlattenedPaths:['flat2'],
            dottedValuePaths:['two':'John','three':'Johnson'],
            dottedShortObjectPaths:['short2'],
            shortObjectClosure:c2,
            dottedDatePaths:['date2'],
            dateFormats:['yyyy/MM/dd'],
            lenientDates:true
        )

        when:
        def config = one.merge(two)

        then:
        ['one':'newOne','two':'myTwo']      == one.dottedRenamedPaths
        ['flat1']                           == one.dottedFlattenedPaths
        ['one':'Smith','two':'shoe']        == one.dottedValuePaths
        ['short1']                          == one.dottedShortObjectPaths
        c1                                  == one.shortObjectClosure
        ['date1']                           == one.dottedDatePaths
        ['yyyy.MM.dd']                      == one.dateFormats
        false                               == one.lenientDates

        ['two':'newTwo','three':'newThree'] == two.dottedRenamedPaths
        ['flat2']                           == two.dottedFlattenedPaths
        ['two':'John','three':'Johnson']    == two.dottedValuePaths
        ['short2']                          == two.dottedShortObjectPaths
        c2                                  == two.shortObjectClosure
        ['date2']                           == two.dottedDatePaths
        ['yyyy/MM/dd']                      == two.dateFormats
        true                                == two.lenientDates
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

    def "Test merging with lenient dates set only on the left"() {
        setup:
        def one = new JSONExtractorConfig(lenientDates:true)
        def two = new JSONExtractorConfig()

        when:
        def config = one.merge(two)

        then:
        true   == config.lenientDates
    }

    private JSONExtractorConfig invoke( Closure c ) {
        JSONExtractorDelegate delegate = new JSONExtractorDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        delegate.config
    }
}
