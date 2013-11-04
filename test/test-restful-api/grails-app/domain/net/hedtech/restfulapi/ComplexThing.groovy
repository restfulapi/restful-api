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

import javax.persistence.*


class ComplexThing implements Serializable {

    static hasMany = [things: Thing]

    List<Thing>      things
    String           complexCode
    transient String transientProp = "junk"

    // We'll test Date representations for ISO8601 compliance
    Date buildDate = new Date()

    // We'll include database 'Transient' properties
    // (which are not (always) supported by DomainClassMarshaller)
    @Transient boolean      xlarge = false
    @Transient List<String> listOfStuff = ['Apple']

    // BigDecimal is used often within Banner XE domains
    BigDecimal size

    // We'll include a 'transient' property that is not normally
    // serialized but is included in converters unless excluded
    // by our marshallers. (We'll test the ability to exclude these.)
    transient String dataOrigin


    // Adds a part and returns 'this' to allow chaining.
    public ComplexThing addThing(Thing thing) {

        if (!things) things = new ArrayList()
        things.add(thing)

        // ok, we wouldn't normally persist a calculated values...
        size = (BigDecimal) things.size()
        // or set a calculated value before it is asked for...
        xlarge = (things.size() > 1) ? true : false
        this
    }


    public String toString() {
        "ComplexThing[id=$id, complexCode=$complexCode, things=${things}]"
    }


    static mapping = { }


    static constraints = {
        complexCode        ( nullable: false, maxSize: 4, unique: true  )
        size               ( nullable: true )
        dataOrigin         ( nullable: true,  maxSize: 30 )
    }
}
