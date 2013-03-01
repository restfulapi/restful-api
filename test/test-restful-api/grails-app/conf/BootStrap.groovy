/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

import grails.converters.JSON
import grails.converters.XML

import net.hedtech.restfulapi.*
import net.hedtech.restfulapi.marshallers.json.*
import net.hedtech.restfulapi.marshallers.xml.*
import net.hedtech.restfulapi.extractors.json.*
import net.hedtech.restfulapi.extractors.xml.*
import net.hedtech.restfulapi.extractors.configuration.*


class BootStrap {

    def grailsApplication

    def init = { servletContext ->

        // Add rules to singularize resource names as needed here...
        //
        Inflector.addSingularize("mice\$", "\$1mouse")


        // Our simple seed data
        createThing('AA')
        createThing('BB')
    }


    def destroy = { }


    private void createThing(String code) {
        Thing.withTransaction {
            new Thing(code: code, description: "An $code thing",
                      dateManufactured: new Date(), isGood: 'Y', isLarge: true)
                .addPart(new PartOfThing(code: 'aa', description: 'aa part').save())
                .addPart(new PartOfThing(code: 'bb', description: 'bb part').save())
                .save()
        }

    }
}
