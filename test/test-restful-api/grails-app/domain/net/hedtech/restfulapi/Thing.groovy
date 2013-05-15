/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi

class Thing implements Serializable {

    static hasMany = [parts: PartOfThing]

    String     code
    String     description
    Date       dateManufactured
    long       weight = 100
    boolean    isLarge
    Date       lastModified
    String     lastModifiedBy
    String     dataOrigin


    public String toString() {
        "Thing[id=$id, code=$code, description=$description, parts=${parts}]"
    }


    static mapping = {
        parts lazy: false
    }


    static constraints = {
        parts              bindable: false
        code               ( nullable: false, maxSize: 2, unique: true  )
        description        ( nullable: false, maxSize: 30 )
        dateManufactured   ( nullable: true )
        weight             ( nullable: true )
        lastModified       ( nullable: true )
        lastModifiedBy     ( nullable: true,  maxSize: 30 )
        dataOrigin         ( nullable: true,  maxSize: 30 )

    }
}
