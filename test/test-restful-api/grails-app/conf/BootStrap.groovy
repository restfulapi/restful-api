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
        Inflector.addSingularize( "mice\$", "\$1mouse" )

        // Add some seed data if a 'seedThings' system property is set
        //
        if (System.getProperty('seedThings')) {
            ('A'..'Z').each { c1 ->
                ('A'..'Z').each { c2 ->
                    createThing( "${c1}${c2}" )
                }
            }
        }
    }


    def destroy = { }


    private void createThing( String code ) {
        Thing.withTransaction {
            new Thing( code: code, description: "Thing with code $code.",
                       dateManufactured: new Date(), isGood: 'Y', isLarge: true )
                .addToParts( new PartOfThing(code: 'aa', description: "Part 'aa' of $code" ) )
                .addToParts( new PartOfThing(code: 'bb', description: "Part 'bb' of $code" ) )
                .save()
        }

    }
}
