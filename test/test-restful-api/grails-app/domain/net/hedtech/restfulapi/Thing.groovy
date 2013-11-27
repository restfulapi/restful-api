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

class Thing implements Serializable {

    static hasMany = [parts: PartOfThing]

    String     code
    String     description
    Date       dateManufactured
    long       weight = 100
    boolean    isLarge
    Date       lastUpdated
    String     lastModifiedBy
    String     dataOrigin


    public String toString() {
        "Thing[id=$id, code=$code, description=$description, parts=${parts}]"
    }


    static mapping = {
        autoTimestamp true
        parts lazy: false
    }


    static constraints = {
        parts              bindable: false
        code               ( nullable: false, maxSize: 2, unique: true  )
        description        ( nullable: false, maxSize: 50 )
        dateManufactured   ( nullable: true )
        weight             ( nullable: true )
        lastUpdated        ( nullable: true )
        lastModifiedBy     ( nullable: true,  maxSize: 30 )
        dataOrigin         ( nullable: true,  maxSize: 30 )

    }
}
