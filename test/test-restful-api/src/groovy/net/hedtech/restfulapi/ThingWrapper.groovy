/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
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
        "ComplexWrapper[complexCode=$complexCode, things=${things}]"
    }


}