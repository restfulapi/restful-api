/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi

/**
 * Class for testing domain class marshallers.
 **/
class MarshalledOwnerOfThing {

    String firstName
    String lastName

    public String toString() {
        "MarshalledOwnerOfThing[id=$id, firstName=$firstName, lastName=$lastName]"
    }


    static constraints = {
        firstName          ( nullable: false, maxSize: 30 )
        lastName           ( nullable: false, maxSize: 30 )
    }

}