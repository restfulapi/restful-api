/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi

class Thing implements Serializable {

    static hasMany = [parts: PartOfThing]

    Set        parts = [] as Set
    String     code
    String     description
    Date       dateManufactured
    String     isGood
    boolean    isLarge
    Date       lastModified
    String     lastModifiedBy
    String     dataOrigin
    Float    quantity


    // Adds a part and returns 'this' to allow chaining.
    public Thing addPart(PartOfThing part) {
        parts.add(part)
        this
    }


    public String toString() {
        "Thing[id=$id, code=$code, description=$description, parts=${parts}]"
    }


    static mapping = {
    }


    static constraints = {
        code               ( nullable: false, maxSize: 2, unique: true  )
        description        ( nullable: false, maxSize: 30 )
        dateManufactured   ( nullable: true )
        isGood             ( nullable: true,  maxSize: 1, inList:['Y','N'] )
        lastModified       ( nullable: true )
        lastModifiedBy     ( nullable: true,  maxSize: 30 )
        dataOrigin         ( nullable: true,  maxSize: 30 )
        quantity           ( nullable: true )
    }
}
