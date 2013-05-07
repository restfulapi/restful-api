/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class MarshalledSubPartOfThing {

    String  code
    String  description
    MarshalledThing thing


    public String toString() {
        "MarshalledSubPartOfThing[id=$id, code=$code, description=$description, thing=${thing?.id}]"
    }


    static constraints = {
        code               ( nullable: false, maxSize: 2  )
        description        ( nullable: true, maxSize: 30 )
    }
}
