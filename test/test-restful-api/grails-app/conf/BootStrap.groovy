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

        // Example of how to Register a marshaller into the default configuration,
        // that overrides the marshallers added by the 'restful-api' plugin.
        // Note the priority is above 100 (so that it will be used instead of the
        // marshaller registered by the plugin.)

        // 'json' (application/json) configuration
        //
        //JSON.registerObjectMarshaller(new BasicDomainClassMarshaller(grailsApplication), 101)

        // ------ Named configuration(s) that corresponds to a custom media type ------

        // 'jsonv0' (application/vnd.hedtech.v0+json) configuration
        //
        JSON.createNamedConfig('jsonv0') {
            it.registerObjectMarshaller(new BasicHalDomainClassMarshaller(grailsApplication), 100)
            it.registerObjectMarshaller(new ThingClassMarshaller(grailsApplication), 101)
        }

        XML.registerObjectMarshaller(new JSONObjectMarshaller(), 200)

        XML.createNamedConfig('xmlv0') {
            it.registerObjectMarshaller(new JSONObjectMarshaller(), 200)
        }

        XML.createNamedConfig('thing-xmlv0') {
            it.registerObjectMarshaller(new net.hedtech.restfulapi.marshallers.xml.v0.ThingClassMarshaller(), 101)
        }

        XML.createNamedConfig('thing-xmlv1') {
            it.registerObjectMarshaller(new net.hedtech.restfulapi.marshallers.xml.v1.ThingClassMarshaller(), 101)
        }

        JSONExtractorConfigurationHolder.registerExtractor( "things", "json", new DefaultJSONExtractor() )
        JSONExtractorConfigurationHolder.registerExtractor( "things", "jsonv1", new ThingDefaultDescriptionExtractor() )
        JSONExtractorConfigurationHolder.registerExtractor( "thing-wrapper", "json", new DefaultJSONExtractor() )

        XMLExtractorConfigurationHolder.registerExtractor( "things", "xml", new JSONObjectExtractor() )
        XMLExtractorConfigurationHolder.registerExtractor( "things", "xmlv1", new JSONObjectExtractor() )

        XMLExtractorConfigurationHolder.registerExtractor( "things", "thing-xmlv0",
            new net.hedtech.restfulapi.extractors.xml.v0.ThingExtractor() )

        XMLExtractorConfigurationHolder.registerExtractor( "things", "thing-xmlv1",
            new net.hedtech.restfulapi.extractors.xml.v1.ThingExtractor() )



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
