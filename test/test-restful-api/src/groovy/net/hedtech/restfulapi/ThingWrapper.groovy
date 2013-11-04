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
package net.hedtech.restfulapi

/**
 * A simple 'non-domain' object corresponding to a resource.
 **/
class ThingWrapper implements Serializable {


    List<Thing>      things
    String           complexCode
    transient String transientProp = "junk"

    // We'll test Date representations for ISO8601 compliance
    Date buildDate = new Date()

    boolean      xlarge = { return (things.size() > 1) ? true : false }
    List<String> listOfStuff = ['Apple']

    // BigDecimal is used often within Banner XE domains
    BigDecimal size


    // Adds a part and returns 'this' to allow chaining.
    public ThingWrapper addThing(Thing thing) {

        if (!things) things = new ArrayList()
        things.add(thing)

        // ok, we wouldn't normally persist a calculated values...
        size = (BigDecimal) things.size()
        // or set a calculated value before it is asked for...
        xlarge = (things.size() > 1) ? true : false
        this
    }


    public String toString() {
        "ThingWrapper[complexCode=$complexCode, things=${things}]"
    }


}
