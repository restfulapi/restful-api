/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class MarshalledThingEmbeddedPart {

    String  serialNumber
    String  description


    public String toString() {
        "MarshalledThingEmbeddedPart[id=$id, serialNumber=$serialNumber, description=$description, thing=${thing?.id}]"
    }


    static constraints = {
        serialNumber       ( nullable: false, maxSize: 20  )
        description        ( nullable: false, maxSize: 30 )
    }
}
