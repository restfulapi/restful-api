/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

import net.hedtech.restfulapi.*

import org.codehaus.groovy.grails.commons.ApplicationAttributes


class BootStrap {

    def grailsApplication

    def init = { servletContext ->

        // Add custom rules to singularize resource names as needed here...
        //
        Inflector.addSingularize("mice\$", "\$1mouse")

        // Add some simple seed data
        //
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
