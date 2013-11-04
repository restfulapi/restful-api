/* ****************************************************************************
 * Copyright 2013 Ellucian Company L.P. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package net.hedtech.restfulapi

/**
 * Class for testing domain class marshallers.
 **/
class MarshalledThing implements Serializable {

    static hasMany = [
        parts: MarshalledPartOfThing, //one-to-many (Collection)
        contributors:MarshalledThingContributor, //one-to-many (Map)
        simpleArray:String
    ]
    static hasOne = [subPart:MarshalledSubPartOfThing] //1-to-1
    static embedded = ['embeddedPart']

    Map contributors = [:]
    MarshalledThingEmbeddedPart embeddedPart
    MarshalledOwnerOfThing owner //many-to-one
    Map simpleMap = [:]
    Collection simpleArray = []
    Collection parts = []

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
        subPart            nullable: true, bindable: false, unique: true
        embeddedPart       nullable: true
        owner              nullable: true
        code               ( nullable: false, maxSize: 2, unique: true  )
        description        ( nullable: true, maxSize: 30 )
        lastModified       ( nullable: true )
        lastModifiedBy     ( nullable: true,  maxSize: 30 )
        dataOrigin         ( nullable: true,  maxSize: 30 )

    }

}
