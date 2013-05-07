/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class MarshalledPartOfThing {

    static belongsTo = [thing: MarshalledThing]

    String  code
    String  description


    public String toString() {
        "MarshalledPartOfThing[id=$id, code=$code, description=$description, thing=${thing?.id}]"
    }


    static constraints = {
        code               ( nullable: false, maxSize: 2  )
        description        ( nullable: false, maxSize: 30 )
    }
}
