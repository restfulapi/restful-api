/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi

/**
 * Class for testing domain class marshallers.
 **/
class MarshalledThing implements Serializable {

    Map contributors
    MarshalledThingEmbeddedPart embeddedPart
    MarshalledOwnerOfThing owner //many-to-one

    static hasMany = [
        parts: MarshalledPartOfThing, //one-to-many (Collection)
        contributors:MarshalledThingContributor //one-to-many (Map)
    ]
    static hasOne = [subPart:MarshalledSubPartOfThing] //1-to-1
    static embedded = ['embeddedPart']




    String     code
    String     description
    boolean    isLarge
    Date       lastModified
    String     lastModifiedBy
    String     dataOrigin


    public String toString() {
        "MarshalledThing[id=$id, code=$code, description=$description, parts=${parts}]"
    }


    static mapping = {
        parts lazy: false
    }

    static constraints = {
        parts              bindable: false
        subPart            bindable: false, unique: true
        code               ( nullable: false, maxSize: 2, unique: true  )
        description        ( nullable: false, maxSize: 30 )
        lastModified       ( nullable: true )
        lastModifiedBy     ( nullable: true,  maxSize: 30 )
        dataOrigin         ( nullable: true,  maxSize: 30 )

    }

}