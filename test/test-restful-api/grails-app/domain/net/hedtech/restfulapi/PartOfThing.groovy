/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/ 

package net.hedtech.restfulapi

class PartOfThing {

    String  code
    String  description


    public String toString() {
        "PartOfThing[id=$id, code=$code, description=$description]"
    }


    static constraints = {
        code               ( nullable: false, maxSize: 2  )
        description        ( nullable: false, maxSize: 30 )
    }
}
