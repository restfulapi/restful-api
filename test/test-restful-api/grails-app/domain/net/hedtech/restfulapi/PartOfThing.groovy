/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

class PartOfThing {

    static belongsTo = [thing: Thing]

    String  code
    String  description


    public String getEtag() {
        "TEST-ETag-$id" // NOT A GOOD ETAG; ONLY FOR TESTING - doesn't reflect change indicator...
    }


    public String toString() {
        "PartOfThing[id=$id, code=$code, description=$description, thing=${thing?.id}]"
    }


    static constraints = {
        code               ( nullable: false, maxSize: 2  )
        description        ( nullable: false, maxSize: 30 )
    }
}
