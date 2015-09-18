/* ****************************************************************************
 * Copyright 2015 Ellucian Company L.P. and its affiliates.
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
package net.hedtech.restfulapi

import grails.test.mixin.*

import groovy.transform.NotYetImplemented

import spock.lang.*

class JSONPatchSupportSpec extends Specification {

    // This is the example object used in JSON Pointer RFC 6901,
    // plus a 'complex' property containing a nested array and object
    Map cleanState() {
        return [ "foo":       [ "bar", "baz" ],
                 "":          "0",
                 "a/b":       "1",
                 "c%d":       "2",
                 "e^f":       "3",
                 "g|h":       "4",
                 "i\\j":      "5",
                 "k\"l":      "6",
                 " ":         "7",
                 "m~n":       "8",
                 "complex":   [
                                "array": [ "first",
                                           "second",
                                           [ "thirdProp": "thirdVal"],
                                           [ "fourthPropA": "fourthValA",
                                             "fourthPropB": "fourthValB"]
                                ],
                                "nested": [
                                    "propA": "propA-value",
                                    "propB": [ "nested1": "nested-again-1-value",
                                               "nested2": "nested-again-2-value" ]
                                ]
                              ]
               ]
    }

    @Unroll
    def 'Test ability to apply individual \'replace\' operations'( patch, assertion ) {
        setup:
        def currentState = cleanState()

        when:
        def patchedState = JSONPatchSupport.applyPatches( patch, currentState )

        then:
        assert assertion(patchedState)                           // assert the expected property is PATCHED,
        assert 1 == (patchedState.toString() =~ /PATCHED/).count // that no other properties were PATCHED
        assert 0 == (currentState.toString() =~ /PATCHED/).count // and that the original object wasn't mutated

        where:
        patch                                                                             | assertion
        [[ "op": "replace", "path": "/foo/0", "value": "PATCHED"  ]]                      | {o -> o.foo[0] == 'PATCHED'}
        [[ "op": "replace", "path": "/foo",   "value": "PATCHED" ]]                       | {o -> o.foo == 'PATCHED'}
        [[ "op": "replace", "path": "/",      "value": "PATCHED"  ]]                      | {o -> o == 'PATCHED'}
        [[ "op": "replace", "path": "/a~1b",      "value": "PATCHED"  ]]                  | {o -> o['a/b'] == 'PATCHED'}
        [[ "op": "replace", "path": "/c%d",      "value": "PATCHED"  ]]                   | {o -> o['c%d'] == 'PATCHED'}
        [[ "op": "replace", "path": "/e^f",      "value": "PATCHED"  ]]                   | {o -> o['e^f'] == 'PATCHED'}
        [[ "op": "replace", "path": "/g|h",      "value": "PATCHED"  ]]                   | {o -> o['g|h'] == 'PATCHED'}
        [[ "op": "replace", "path": "/i\\j",      "value": "PATCHED"  ]]                  | {o -> o['i\\j'] == 'PATCHED'}
        [[ "op": "replace", "path": "/k\"l",      "value": "PATCHED"  ]]                  | {o -> o['k\"l'] == 'PATCHED'}
        [[ "op": "replace", "path": "/ ",      "value": "PATCHED"  ]]                     | {o -> o[' '] == 'PATCHED'}
        [[ "op": "replace", "path": "/m~0n",      "value": "PATCHED"  ]]                  | {o -> o['m~n'] == 'PATCHED'}
        [[ "op": "replace", "path": "/complex/nested/propA", "value": "PATCHED"  ]]       | {o -> o.complex.nested.propA == 'PATCHED'}
        [[ "op": "replace", "path": "/complex/nested/propB/nested2", "value": "PATCHED" ]]| {o -> o.complex.nested.propB.nested2 == 'PATCHED'}
        [[ "op": "replace", "path": "/complex/array/1", "value": "PATCHED"  ]]            | {o -> o.complex.array[1] == 'PATCHED'}
        [[ "op": "replace", "path": "/complex/array/2/thirdProp", "value": "PATCHED"  ]]  | {o -> o.complex.array[2].thirdProp == 'PATCHED'}
    }


    def 'Test ability to apply multiple \'replace\' patches'() {
        // Uses the same patches as previous test, just all together in one document
        setup:
        def currentState = cleanState()
        def patches = [
                [ "op": "replace", "path": "/foo/0", "value": "PATCHED"  ],
                // We don't want to destroy our 'foo' array, as we'll assert changes to elements
                // [ "op": "replace", "path": "/foo",   "value": "PATCHED" ],
                // We also don't want to destroy our entire object, or we couldn't apply other patches
                // [ "op": "replace", "path": "/",      "value": "PATCHED"  ],
                [ "op": "replace", "path": "/a~1b", "value": "PATCHED"  ],
                [ "op": "replace", "path": "/c%d",  "value": "PATCHED"  ],
                [ "op": "replace", "path": "/e^f",  "value": "PATCHED"  ],
                [ "op": "replace", "path": "/g|h",  "value": "PATCHED"  ],
                [ "op": "replace", "path": "/i\\j", "value": "PATCHED"  ],
                [ "op": "replace", "path": "/k\"l", "value": "PATCHED"  ],
                [ "op": "replace", "path": "/ ",    "value": "PATCHED"  ],
                [ "op": "replace", "path": "/m~0n", "value": "PATCHED"  ],
                [ "op": "replace", "path": "/complex/nested/propA", "value": "PATCHED"  ],
                [ "op": "replace", "path": "/complex/nested/propB/nested2", "value": "PATCHED" ],
                [ "op": "replace", "path": "/complex/array/1", "value": "PATCHED"  ],
                [ "op": "replace", "path": "/complex/array/2/thirdProp", "value": "PATCHED"  ]
        ]

        when:
        def patchedState = JSONPatchSupport.applyPatches( patches, currentState )

        then:

        [ {o -> o.foo[0] == 'PATCHED'},
          //{o -> o.foo == 'PATCHED'}, - we can't assert destroying our array and assert contents of that array...
          //{o -> o == 'PATCHED'}, - obviously we couldn't destroy our object so cannot assert this one...
          {o -> o['a/b'] == 'PATCHED'},
          {o -> o['c%d'] == 'PATCHED'},
          {o -> o['e^f'] == 'PATCHED'},
          {o -> o['g|h'] == 'PATCHED'},
          {o -> o['i\\j'] == 'PATCHED'},
          {o -> o['k\"l'] == 'PATCHED'},
          {o -> o[' '] == 'PATCHED'},
          {o -> o['m~n'] == 'PATCHED'},
          {o -> o.complex.nested.propA == 'PATCHED'},
          {o -> o.complex.nested.propB.nested2 == 'PATCHED'},
          {o -> o.complex.array[1] == 'PATCHED'},
          {o -> o.complex.array[2].thirdProp == 'PATCHED'}
        ].each { assertion -> assert assertion(patchedState) }

        assert 13 == (patchedState.toString() =~ /PATCHED/).count // that no other properties were PATCHED
        assert 0 == (currentState.toString() =~ /PATCHED/).count // and that the original object wasn't mutated
    }

    @Unroll
    def 'Test exceptions are thrown when told to \'replace\' a non-existent property'(patch, expectedMessage) {
        setup:
        def currentState = cleanState()

        when:
        def message
        try {
            def patchedState = JSONPatchSupport.applyPatches( patch, currentState )
            fail("Expected an exception as path '${patch.path} does not exist")
        } catch (e) {
            message = e.message
        }
        then:
        assert expectedMessage == message

        where:
        patch                                                              | expectedMessage
        [[ "op": "replace", "path": "/foo/9",           "value": "NOPE" ]] | "Path [foo, 9] not found"
        [[ "op": "replace", "path": "/nope",            "value": "NOPE" ]] | "Path [nope] not found"
        [[ "op": "replace", "path": "/complex/notHere", "value": "NOPE" ]] | "Path [complex, notHere] not found"
        [[ "op": "replace", "path": "/a~2b",            "value": "NOPE" ]] | "Path [a~2b] not found"

    }


    @Unroll
    def 'Test ability to perform individual \'add\' operations'( patch, assertion ) {
        setup:
        def currentState = cleanState()

        when:
        def patchedState = JSONPatchSupport.applyPatches( patch, currentState )

        then:
        assert assertion(patchedState)                         // assert the expected property is ADDED,
        assert 1 == (patchedState.toString() =~ /ADDED/).count // that no other properties were ADDED
        assert 0 == (currentState.toString() =~ /ADDED/).count // and that the original object wasn't mutated

        where:
        // Attempting to access new top-level properties fails even though
        // iterating through the patched object does in fact find the new
        // properties. Hence use of 'exists' helper function.
        // TODO: Fix assertions to access top-level props without iteration
        patch                                                                        | assertion
        [[ "op": "add", "path": "/junk",  "value": "ADDED" ]]                        | {o -> exists(o, 'junk', 'ADDED') }
        [[ "op": "add", "path": "/foo",   "value": "ADDED" ]]                        | {o -> o.foo[2] == 'ADDED'}
        [[ "op": "add", "path": "/a~1c",  "value": "ADDED" ]]                        | {o -> exists(o, 'a/c', 'ADDED') }
        [[ "op": "add", "path": "/c%e",   "value": "ADDED" ]]                        | {o -> exists(o, 'c%e', 'ADDED') }
        [[ "op": "add", "path": "/e^g",   "value": "ADDED" ]]                        | {o -> exists(o, 'e^g', 'ADDED') }
        [[ "op": "add", "path": "/g|i",   "value": "ADDED" ]]                        | {o -> exists(o, 'g|i', 'ADDED') }
        [[ "op": "add", "path": "/i\\k",  "value": "ADDED" ]]                        | {o -> exists(o, 'i\\k', 'ADDED') }
        [[ "op": "add", "path": "/k\"m",  "value": "ADDED" ]]                        | {o -> exists(o, 'k\"m', 'ADDED') }
        [[ "op": "add", "path": "/m~0p",  "value": "ADDED" ]]                        | {o -> exists(o, 'm~p', 'ADDED') }
        [[ "op": "add", "path": "/complex/nested/propC", "value": "ADDED" ]]         | {o -> o.complex.nested.propC == 'ADDED'}
        [[ "op": "add", "path": "/complex/nested/propB/nested3", "value": "ADDED" ]] | {o -> o.complex.nested.propB.nested3 == 'ADDED'}
        [[ "op": "add", "path": "/complex/array/3/fourthPropC", "value": "ADDED" ]]  | {o -> o.complex.array[3].fourthPropC == 'ADDED'}
        [[ "op": "add", "path": "/complex/array/4", "value": "ADDED" ]]              | {o -> o.complex.array[4] == 'ADDED'}
    }


    def 'Test ability to apply multiple \'add\' patches'() {
        // Uses the same patches as previous test, just all together in one document
        setup:
        def currentState = cleanState()
        def patches = [
                [ "op": "add", "path": "/junk",  "value": "ADDED" ],
                [ "op": "add", "path": "/foo",   "value": "ADDED" ],
                [ "op": "add", "path": "/a~1c",  "value": "ADDED" ],
                [ "op": "add", "path": "/c%e",   "value": "ADDED" ],
                [ "op": "add", "path": "/e^g",   "value": "ADDED" ],
                [ "op": "add", "path": "/g|i",   "value": "ADDED" ],
                [ "op": "add", "path": "/i\\k",  "value": "ADDED" ],
                [ "op": "add", "path": "/k\"m",  "value": "ADDED" ],
                [ "op": "add", "path": "/m~0p",  "value": "ADDED" ],
                [ "op": "add", "path": "/complex/nested/propC", "value": "ADDED" ],
                [ "op": "add", "path": "/complex/nested/propB/nested3", "value": "ADDED" ],
                [ "op": "add", "path": "/complex/array/3/fourthPropC", "value": "ADDED" ],
                [ "op": "add", "path": "/complex/array/4", "value": "ADDED" ]
        ]

        when:
        def patchedState = JSONPatchSupport.applyPatches( patches, currentState )

        then:

        [ {o -> exists(o, 'junk', 'ADDED')},
          {o -> o.foo[2] == 'ADDED'},
          {o -> exists(o, 'a/c', 'ADDED')},
          {o -> exists(o, 'c%e', 'ADDED')},
          {o -> exists(o, 'e^g', 'ADDED')},
          {o -> exists(o, 'g|i', 'ADDED')},
          {o -> exists(o, 'i\\k', 'ADDED')},
          {o -> exists(o, 'k\"m', 'ADDED')},
          {o -> exists(o, 'm~p', 'ADDED')},
          {o -> o.complex.nested.propC == 'ADDED'},
          {o -> o.complex.nested.propB.nested3 == 'ADDED'},
          {o -> o.complex.array[3].fourthPropC == 'ADDED'},
          {o -> o.complex.array[4] == 'ADDED'}
        ].each { assertion -> assert assertion(patchedState) }

        assert 13 == (patchedState.toString() =~ /ADDED/).count // that no other properties were PATCHED
        assert 0 == (currentState.toString() =~ /ADDED/).count // and that the original object wasn't mutated
    }
                                // "array": [ "first",
                                //            "second",
                                //            [ "thirdProp": "thirdVal"],
                                //            [ "fourthPropA": "fourthValA",
                                //              "fourthPropB": "fourthValB"]
                                // ]


    // Note: Removing properties and delegating to a 'service.update' would only be
    //       effective is the service update method doesn't support 'partial updates'.
    @Unroll
    def 'Test ability to perform individual \'remove\' operations'( patch, assertion ) {
        setup:
        def currentState = cleanState()

        when:
        def patchedState = JSONPatchSupport.applyPatches( patch, currentState )

        then:
        assert assertion(patchedState) // assert the expected property has been removed,

        where:
        patch                                                         | assertion
        [[ "op": "remove", "path": "/foo/0"  ]]                       | {o -> o.foo[0] == 'baz'} // was 2nd element
        [[ "op": "remove", "path": "/a~1b" ]]                         | {o -> o['a/b'] == null}
        [[ "op": "remove", "path": "/c%d"  ]]                         | {o -> o['c%d'] == null}
        [[ "op": "remove", "path": "/e^f" ]]                          | {o -> o['e^f'] == null}
        [[ "op": "remove", "path": "/g|h" ]]                          | {o -> o['g|h'] == null}
        [[ "op": "remove", "path": "/i\\j" ]]                         | {o -> o['i\\j'] == null}
        [[ "op": "remove", "path": "/k\"l" ]]                         | {o -> o['k\"l'] == null}
        [[ "op": "remove", "path": "/ " ]]                            | {o -> o[' '] == null}
        [[ "op": "remove", "path": "/m~0n" ]]                         | {o -> o['m~n'] == null}
        [[ "op": "remove", "path": "/complex/nested/propA" ]]         | {o -> o.complex.nested.propA == null}
        [[ "op": "remove", "path": "/complex/nested/propB/nested2" ]] | {o -> o.complex.nested.propB.nested2 == null}
        // 'thirdProp' is a key to a map with a single entry, so we should remove the entire map...
        [[ "op": "remove", "path": "/complex/array/2/thirdProp" ]]    | {o -> o.complex.array.size() == 3}
        // 'fourthPropA' is a key to a map with a remaining entry(s), so we should only remove a property from the map...
        [[ "op": "remove", "path": "/complex/array/3/fourthPropA" ]]  | {o -> o.complex.array[3].size() == 1 &&
                                                                              o.complex.array[3].fourthPropB == "fourthValB"} // was 3rd
    }


    def 'Test ability to perform multiple \'remove\' operations'() {
        // Uses the same patches as previous test, just all together in one document
        setup:
        def currentState = cleanState()
        def patches = [
                [ "op": "remove", "path": "/foo/0" ],
                [ "op": "remove", "path": "/a~1b" ],
                [ "op": "remove", "path": "/c%d" ],
                [ "op": "remove", "path": "/e^f" ],
                [ "op": "remove", "path": "/g|h" ],
                [ "op": "remove", "path": "/i\\j" ],
                [ "op": "remove", "path": "/k\"l" ],
                [ "op": "remove", "path": "/ " ],
                [ "op": "remove", "path": "/m~0n" ],
                [ "op": "remove", "path": "/complex/nested/propA" ],
                [ "op": "remove", "path": "/complex/nested/propB/nested2" ],
                [ "op": "remove", "path": "/complex/array/1" ],              // remove 2nd
                // thirdProp is the only prop in a map, so we should remove the entire map from the parent array
                [ "op": "remove", "path": "/complex/array/1/thirdProp" ],    // was 3rd, now 2nd
                [ "op": "remove", "path": "/complex/array/1/fourthPropA" ]   // was 4th, now 2nd
        ]

        when:
        def patchedState = JSONPatchSupport.applyPatches( patches, currentState )

        then:

        [ {o -> o.foo[0] == "baz"}, // was foo[1]
          {o -> o['a/b'] == null},
          {o -> o['c%d'] == null},
          {o -> o['e^f'] == null},
          {o -> o['g|h'] == null},
          {o -> o['i\\j'] == null},
          {o -> o['k\"l'] == null},
          {o -> o[' '] == null},
          {o -> o['m~n'] == null},
          {o -> o.complex.nested.propA == null},
          {o -> o.complex.nested.propB.nested2 == null},
          {o -> o.complex.array.size() == 2 && o.complex.array[0] == 'first'},
          {o -> o.complex.array.size() == 2 && o.complex.array[1].size() == 1 &&
                o.complex.array[1].fourthPropB == 'fourthValB'}
        ].each {
            assertion -> assert assertion(patchedState)
        }
        assert 3 == patchedState.size() // that no other properties were PATCHED
        assert 10 < currentState.size() // and that the original object wasn't mutated
    }


    def 'Test ability to perform multiple operations'() {
        // Uses the same patches as previous test, just all together in one document
        setup:
        def currentState = cleanState()
        def patches = [
                [ "op": "replace", "path": "/foo/0", "value": "PATCHED"  ],
                // We don't want to destroy our 'foo' array, as we'll assert changes to elements
                // [ "op": "replace", "path": "/foo",   "value": "PATCHED" ],
                // We also don't want to destroy our entire object, or we couldn't apply other patches
                // [ "op": "replace", "path": "/",      "value": "PATCHED"  ],
                [ "op": "replace", "path": "/a~1b", "value": "PATCHED"  ],
                [ "op": "add", "path": "/complex/nested/propB/nested3", "value": "ADDED" ],
                [ "op": "replace", "path": "/c%d",  "value": "PATCHED"  ],
                [ "op": "replace", "path": "/e^f",  "value": "PATCHED"  ],
                [ "op": "remove", "path": "/g|h" ],
                [ "op": "remove", "path": "/i\\j" ],
                [ "op": "remove", "path": "/m~0n" ],
                [ "op": "add", "path": "/m~0p",  "value": "ADDED" ],
                [ "op": "add", "path": "/complex/array/3/fourthPropC", "value": "ADDED" ],
                [ "op": "remove", "path": "/complex/nested/propA" ],
                [ "op": "replace", "path": "/complex/nested/propB/nested2", "value": "PATCHED" ],
                [ "op": "remove", "path": "/k\"l" ],
                [ "op": "add", "path": "/complex/nested/propC", "value": "ADDED" ],
                [ "op": "remove", "path": "/ " ],
                [ "op": "replace", "path": "/complex/array/1", "value": "PATCHED"  ],
                [ "op": "replace", "path": "/complex/array/2/thirdProp", "value": "PATCHED"  ]
        ]

        when:
        def patchedState = JSONPatchSupport.applyPatches( patches, currentState )

        then:
        [ {o -> o.foo.size() == 2 && o.foo[0] == 'PATCHED' && o.foo[1] == 'baz'},
          {o -> o['a/b'] == 'PATCHED'},
          {o -> o['c%d'] == 'PATCHED'},
          {o -> o['e^f'] == 'PATCHED'},
          {o -> o['g|h'] == null},
          {o -> o['i\\j'] == null},
          {o -> o['k\"l'] == null},
          {o -> exists(o, 'm~p', 'ADDED')},
          {o -> o.complex.nested.propB.nested2 == 'PATCHED'},
          {o -> o.complex.nested.propC == 'ADDED'},
          {o -> o.complex.array[1] == 'PATCHED'},
          {o -> o.complex.array[2].thirdProp == 'PATCHED'},
          {o -> o.complex.array[3].fourthPropC == 'ADDED'}
        ].each {
            assertion -> assert assertion(patchedState)
        }

        assert 7 == (patchedState.toString() =~ /PATCHED/).count // that no other properties were PATCHED
        assert 0 == (currentState.toString() =~ /PATCHED/).count // and that the original object wasn't mutated
        assert 4 == (patchedState.toString() =~ /ADDED/).count // that no other properties were PATCHED
    }


    // ------------------------ Helper Methods -----------------------


    private boolean exists( obj, expectedKey, expectedValue ) {
        boolean result = false
        obj.each { k, v ->
            if (k == expectedKey && v == expectedValue) result = true
        };
        return result;
    }

}
