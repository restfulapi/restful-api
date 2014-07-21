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

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.*
import net.hedtech.restfulapi.marshallers.json.*

import spock.lang.*


@TestMixin(GrailsUnitTestMixin)
class JSONDomainMarshallerConfigSpec extends Specification {

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
        true          == config.useIncludedFields
    }

    def "Test including no fields"() {
        setup:
        def src = {
            includesFields {
            }
        }

        when:
        def config = invoke( src )

        then:
        []   == config.includedFields
        true == config.useIncludedFields
    }

    def "Test requires included fields"() {
        setup:
        def src = {
            requiresIncludedFields true
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


    def "Test exclude id and version"() {
       setup:
        def src = {
            includesId false
            includesVersion false
        }

        when:
        def config = invoke( src )

        then:
        false == config.includeId
        false == config.includeVersion
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

    def "Test field resource names"() {
        setup:
        def src = {
            field 'owner' resource 'thing-owners'
            field 'manager' name 'mgr' resource 'thing-managers'
            field 'accountant' resource 'thing-accountants' name 'acct'
        }

        when:
        def config = invoke(src)

        then:
        ['owner':'thing-owners','manager':'thing-managers','accountant':'thing-accountants'] == config.fieldResourceNames
    }

    def "Test field resource names in includes"() {
        setup:
        def src = {
            includesFields {
                field 'owner' resource 'thing-owners'
                field 'manager' name 'mgr' resource 'thing-managers'
                field 'accountant' resource 'thing-accountants' name 'acct'
            }
        }

        when:
        def config = invoke(src)

        then:
        ['owner':'thing-owners','manager':'thing-managers','accountant':'thing-accountants'] == config.fieldResourceNames
    }

    def "Test deep marshalling associations"() {
        setup:
        def src = {
            deepMarshallsAssociations true
        }

        when:
        def config = invoke(src)

        then:
        true == config.deepMarshallAssociations
    }

    def "Test deep marshalling fields"() {
        setup:
        def src = {
            field 'owner' deep true
            field 'manager' deep false name 'mgr' resource 'thing-managers'
        }

        when:
        def config = invoke(src)
        then:
        ['owner':true, 'manager':false] == config.deepMarshalledFields
    }

    def "Test custom short object closure"() {
        setup:
        def invoked = false
        def src = {
            shortObject { Map m ->
                invoked = true
            }
        }

        when:
        def config = invoke(src)
        config.shortObjectClosure.call([:])

        then:
        true == invoked
        true == config.isShortObjectClosureSet
    }

    def "Test default marshalling null fields"() {
        setup:
        def src = {
            marshallsNullFields false
        }

        when:
        def config = invoke(src)

        then:
        false == config.marshallNullFields
    }

    def "Test marshalling null fields"() {
        setup:
        def src = {
            field 'owner' marshallsNull true
            field 'manager' marshallsNull false name 'mgr' resource 'thing-managers'
        }

        when:
        def config = invoke(src)
        then:
        ['owner':true, 'manager':false] == config.marshalledNullFields
    }

    def "Test merging domain marshaller configurations"() {
        setup:
        def c1 = { Map m -> }
        def c2 = { Map m -> }
        JSONDomainMarshallerConfig one = new JSONDomainMarshallerConfig(
            supportClass:Thing,
            fieldNames:['foo':'foo1','bar':'bar1'],
            includedFields:['foo','bar'],
            useIncludedFields:true,
            excludedFields:['e1','e2'],
            additionalFieldClosures:[{app,bean,json ->}],
            additionalFieldsMap:['one':'one','two':'two'],
            fieldResourceNames:['f1':'r1','f2':'r2'],
            shortObjectClosure:c1,
            includeId:true,
            includeVersion:true,
            requireIncludedFields:true,
            deepMarshalledFields:['one':true,'two':false],
            deepMarshallAssociations:false,
            marshallNullFields:true,
            marshalledNullFields:['one':true,'two':false],
        )
        JSONDomainMarshallerConfig two = new JSONDomainMarshallerConfig(
            supportClass:PartOfThing,
            fieldNames:['foo':'foo2','baz':'baz1'],
            includedFields:['baz'],
            useIncludedFields:false,
            excludedFields:['e3'],
            additionalFieldClosures:[{app,bean,json ->}],
            additionalFieldsMap:['two':'2','three':'3'],
            fieldResourceNames:['f2':'name3','f3':'r3'],
            shortObjectClosure:c2,
            includeId:false,
            includeVersion:false,
            requireIncludedFields:false,
            deepMarshalledFields:['two':true,'three':false],
            deepMarshallAssociations:true,
            marshallNullFields:false,
            marshalledNullFields:['two':true,'three':false]
        )

        when:
        def config = one.merge(two)

        then:
        true                                     == config.isSupportClassSet
        PartOfThing                              == config.supportClass
        ['foo':'foo2','bar':'bar1','baz':'baz1'] == config.fieldNames
        ['foo','bar','baz']                      == config.includedFields
        true                                     == config.useIncludedFields
        ['e1','e2','e3']                         == config.excludedFields
        2                                        == config.additionalFieldClosures.size()
        ['one':'one',"two":'2','three':'3']      == config.additionalFieldsMap
        ['f1':'r1','f2':'name3','f3':'r3']       == config.fieldResourceNames
        c2                                       == config.shortObjectClosure
        false                                    == config.includeId
        false                                    == config.includeVersion
        false                                    == config.requireIncludedFields
        true                                     == config.deepMarshallAssociations
        ['one':true,'two':true,'three':false]    == config.deepMarshalledFields
        false                                    == config.marshallNullFields
        ['one':true,'two':true,'three':false]    == config.marshalledNullFields
    }

    def "Test merging domain marshaller configurations does not alter either object"() {
        setup:
        def c1 = { Map m -> }
        def c2 = { Map m -> }
        JSONDomainMarshallerConfig one = new JSONDomainMarshallerConfig(
            supportClass:Thing,
            fieldNames:['foo':'foo1','bar':'bar1'],
            includedFields:['foo','bar'],
            useIncludedFields:true,
            excludedFields:['e1','e2'],
            additionalFieldClosures:[{app,bean,json ->}],
            additionalFieldsMap:['one':'1'],
            fieldResourceNames:['f1':'r1','f2':'r2'],
            shortObjectClosure:c1,
            includeId:true,
            includeVersion:true,
            requireIncludedFields:true,
            deepMarshalledFields:['one':true,'two':false],
            deepMarshallAssociations:false,
            marshallNullFields:true,
            marshalledNullFields:['one':true,'two':false],
        )
        JSONDomainMarshallerConfig two = new JSONDomainMarshallerConfig(
            supportClass:PartOfThing,
            fieldNames:['foo':'foo2','baz':'baz1'],
            includedFields:['baz'],
            useIncludedFields:false,
            excludedFields:['e3'],
            additionalFieldClosures:[{app,bean,json ->}],
            additionalFieldsMap:['two':'2'],
            fieldResourceNames:['f2':'name3','f3':'r3'],
            shortObjectClosure:c2,
            includeId:false,
            includeVersion:false,
            requireIncludedFields:false,
            deepMarshalledFields:['two':true,'three':false],
            deepMarshallAssociations:true,
            marshallNullFields:false,
            marshalledNullFields:['two':true,'three':false]
        )

        when:
        one.merge(two)

        then:
        true                        == one.isSupportClassSet
        ['foo':'foo1','bar':'bar1'] == one.fieldNames
        ['foo','bar']               == one.includedFields
        true                        == one.useIncludedFields
        ['e1','e2']                 == one.excludedFields
        1                           == one.additionalFieldClosures.size()
        ['one':'1']                 == one.additionalFieldsMap
        ['f1':'r1','f2':'r2']       == one.fieldResourceNames
        c1                          == one.shortObjectClosure
        true                        == one.includeId
        true                        == one.includeVersion
        true                        == one.requireIncludedFields
        false                       == one.deepMarshallAssociations
        ['one':true,'two':false]    == one.deepMarshalledFields
        true                        == one.marshallNullFields
        ['one':true,'two':false]    == one.marshalledNullFields

        true                        == two.isSupportClassSet
        ['foo':'foo2','baz':'baz1'] == two.fieldNames
        ['baz']                     == two.includedFields
        false                       == two.useIncludedFields
        ['e3']                      == two.excludedFields
        1                           == two.additionalFieldClosures.size()
        ['two':'2']                 == two.additionalFieldsMap
        ['f2':'name3','f3':'r3']    == two.fieldResourceNames
        c2                          == two.shortObjectClosure
        false                       == two.includeId
        false                       == two.includeVersion
        false                       == two.requireIncludedFields
        true                        == two.deepMarshallAssociations
        ['two':true,'three':false]  == two.deepMarshalledFields
        false                       == two.marshallNullFields
        ['two':true, 'three':false] == two.marshalledNullFields
    }

    def "Test merging domain marshaller with support class set only on the left"() {
        setup:
        JSONDomainMarshallerConfig one = new JSONDomainMarshallerConfig(
            supportClass:Thing
        )
        JSONDomainMarshallerConfig two = new JSONDomainMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        Thing == config.supportClass
        true  == config.isSupportClassSet
    }

    def "Test merging domain marshaller with short object closure set only on the left"() {
        setup:
        def c1 = { Map m -> }
        JSONDomainMarshallerConfig one = new JSONDomainMarshallerConfig(
            shortObjectClosure:c1
        )
        JSONDomainMarshallerConfig two = new JSONDomainMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        c1   == config.shortObjectClosure
        true == config.isShortObjectClosureSet
    }

    def "Test merging domain marshaller with use included fields set only on the left"() {
        setup:
        def c1 = { Map m -> }
        JSONDomainMarshallerConfig one = new JSONDomainMarshallerConfig(
            useIncludedFields:true
        )
        JSONDomainMarshallerConfig two = new JSONDomainMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        true == config.useIncludedFields
    }

    def "Test merging domain marshaller with require included fields set only on the left"() {
        setup:
        def c1 = { Map m -> }
        JSONDomainMarshallerConfig one = new JSONDomainMarshallerConfig(
            requireIncludedFields:true
        )
        JSONDomainMarshallerConfig two = new JSONDomainMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        true == config.requireIncludedFields
    }

    def "Test merging domain marshaller with deep marshalling associations set only on the left"() {
        setup:
        def c1 = { Map m -> }
        JSONDomainMarshallerConfig one = new JSONDomainMarshallerConfig(
            deepMarshallAssociations:true
        )
        JSONDomainMarshallerConfig two = new JSONDomainMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        true == config.deepMarshallAssociations
    }

    def "Test merging domain marshaller with marshalling null fields set only on the left"() {
        setup:
        def c1 = { Map m -> }
        JSONDomainMarshallerConfig one = new JSONDomainMarshallerConfig(
            marshallNullFields:false
        )
        JSONDomainMarshallerConfig two = new JSONDomainMarshallerConfig(
        )

        when:
        def config = one.merge(two)

        then:
        false == config.marshallNullFields
    }

    def "Test resolution of domain marshaller configuration inherits"() {
        setup:
        JSONDomainMarshallerConfig part1 = new JSONDomainMarshallerConfig(
        )
        JSONDomainMarshallerConfig part2 = new JSONDomainMarshallerConfig(
        )
        JSONDomainMarshallerConfig part3 = new JSONDomainMarshallerConfig(
        )
        JSONDomainMarshallerConfig combined = new JSONDomainMarshallerConfig(
            inherits:['part1','part2']
        )
        JSONDomainMarshallerConfig actual = new JSONDomainMarshallerConfig(
            inherits:['combined','part3']
        )
        ConfigGroup group = new ConfigGroup()
        group.configs = ['part1':part1,'part2':part2,'part3':part3,'combined':combined]

        when:
        def resolvedList = group.resolveInherited( actual )

        then:
        [part1,part2,combined,part3,actual] == resolvedList
    }

    def "Test merge order of domain marshaller configuration inherits"() {
        setup:
        JSONDomainMarshallerConfig part1 = new JSONDomainMarshallerConfig(
            fieldNames:['1':'part1','2':'part1','3':'part1']
        )
        JSONDomainMarshallerConfig part2 = new JSONDomainMarshallerConfig(
            fieldNames:['2':'part2','3':'part2']

        )
        JSONDomainMarshallerConfig actual = new JSONDomainMarshallerConfig(
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
            field 'one' name 'modOne' resource 'resource-ones' deep true marshallsNull false
            field 'one'
            field 'two' name 'modTwo' resource 'resource-twos' deep true marshallsNull true
            includesFields {
                field 'two'
            }
        }

        when:
        def config = invoke( src )

        then:
        [:] == config.fieldNames
        [:] == config.fieldResourceNames
        [:] == config.deepMarshalledFields
        [:] == config.marshalledNullFields
    }

    private JSONDomainMarshallerConfig invoke( Closure c ) {
        JSONDomainMarshallerDelegate delegate = new JSONDomainMarshallerDelegate()
        c.delegate = delegate
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.call()
        delegate.config
    }
}
