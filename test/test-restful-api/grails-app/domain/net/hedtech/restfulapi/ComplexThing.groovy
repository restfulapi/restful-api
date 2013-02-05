/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
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

    // We'll include 'transient' properties that are not normally
    // serialized but are included in converters unless excluded
    // by our marshallers. (We'll test the ability to exclude these.)
    transient Date   lastModified
    transient String lastModifiedBy
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
        lastModified       ( nullable: true )
        lastModifiedBy     ( nullable: true,  maxSize: 30 )
        dataOrigin         ( nullable: true,  maxSize: 30 )
    }
}