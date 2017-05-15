import grails.util.Holders

class BootStrap {
    def restfulApiGrailsPlugin
    def restfulApiController

    def init = { servletContext ->
        //restfulApiGrailsPlugin.doWithApplicationContext(Holders.getApplicationContext())
        //restfulApiController.doWithApplicationContext(Holders.getApplicationContext())
        def applicationContext = Holders.getApplicationContext()
        def artefact = applicationContext.grailsApplication.getArtefactByLogicalPropertyName("Controller", "restfulApi")
        def restfulApiController = applicationContext.getBean(artefact.clazz.name)
        def grailsApplication = applicationContext.getBean('grailsApplication')
        restfulApiController.init(applicationContext.grailsApplication)
    }
    def destroy = {
    }
}

///* ****************************************************************************
// * Copyright 2013 Ellucian Company L.P. and its affiliates.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *  http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *****************************************************************************/
//
//import net.hedtech.restfulapi.*
//import net.hedtech.restfulapp.Thing
//import net.hedtech.restfulapp.PartOfThing
//import org.grails.commons.ApplicationAttributes
//
//
//class BootStrap {
//
//    def grailsApplication
//
//    def init = { servletContext ->
//
//        // Add custom rules to singularize resource names as needed here...
//        //
//        Inflector.addSingularize( "mice\$", "\$1mouse" )
//
//        // Add some seed data if a 'seedThings' system property is set
//        //
//        if (System.getProperty('seedThings')) {
//            ('A'..'Z').each { c1 ->
//                ('A'..'Z').each { c2 ->
//                    createThing( "${c1}${c2}" )
//                }
//            }
//        }
//    }
//
//
//    def destroy = { }
//
//
//    private void createThing( String code ) {
//        Thing.withTransaction {
//            new Thing( code: code, description: "Thing with code $code.",
//                    dateManufactured: new Date(), isGood: 'Y', isLarge: true )
//                    .addToParts( new PartOfThing(code: 'aa', description: "Part 'aa' of $code" ) )
//                    .addToParts( new PartOfThing(code: 'bb', description: "Part 'bb' of $code" ) )
//                    .save()
//        }
//
//    }
//}