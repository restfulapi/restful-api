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

package net.hedtech.restfulapi.query

import grails.plugin.spock.IntegrationSpec

import groovy.sql.Sql

import net.hedtech.restfulapi.*

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication

import java.text.*

import spock.lang.*


class HQLBuilderSpec extends IntegrationSpec {

    def grailsApplication
    def dataSource


    def setup() {
        deleteThings()
    }
    def cleanup() {
        deleteThings()
    }


    def "Test extracting filters from query parameters"() {

        setup:
        // URL:  things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[0][field]':'code',
                       'filter[1][value]':'science',
                       'filter[0][operator]':'eq',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][value]':'ZZ',
                       'max':'50'
                     ]

        when:
        def result = Filter.extractFilters( grailsApplication, params )

        then:
        result[0].field == 'code'
        result[0].operator == 'eq'
        result[0].value == 'ZZ'
        result[1].field == 'description'
        result[1].operator == 'contains'
        result[1].value == 'science'
    }


    @Unroll
    def "Test creating statements based on valid filters"( String field, String operator, def value, String filterType, String statement ) {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        // URL: things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[1][value]': value,
                       'filter[1][field]': field,
                       'filter[1][operator]': operator,
                       'max':'50'
                     ]
        if (filterType) params << [ 'filter[1][type]': filterType ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        statement == query.statement

        where:
        field              | operator   | value           | filterType | statement
        'code'             | 'eq'       | 'xx'            | null       | 'SELECT a FROM Thing a WHERE a.code = :code'
        'code'             | 'eq'       | 'xx'            | 'string'   | 'SELECT a FROM Thing a WHERE a.code = :code'
        'description'      | 'contains' | 'An xx'         | null       | 'SELECT a FROM Thing a WHERE lower(a.description) LIKE lower(:description)'
        'dateManufactured' | 'gt'       | new Date().time | 'date'     | 'SELECT a FROM Thing a WHERE a.dateManufactured > :dateManufactured'
        'weight'           | 'lt'       | 101             | 'num'      | 'SELECT a FROM Thing a WHERE a.weight < :weight'
    }


    @Unroll
    def "Test filter validity"( String field, String operator, def value, def filterType, boolean isValid ) {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        // URL: things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'max':'50'
                     ]
        if (field)      params << [ 'filter[1][field]': field ]
        if (filterType) params << [ 'filter[1][type]': filterType ]
        if (value)      params << [ 'filter[1][value]': value ]
        if (operator)   params << [ 'filter[1][operator]': operator ]


        when:
        def filter = Filter.extractFilters( grailsApplication, params )[0]

        then:
        isValid == filter.isValid()

        where:
        field              | operator   | value           | filterType | isValid
        'code'             | ''         | 'xx'            | null       | false
        'code'             | null       | 'xx'            | null       | false
        'code'             | 'eq'       | 'xx'            | null       | true
        'code'             | 'eq'       | 'xx'            | 'string'   | true
        'code'             | 'equals'   | 'xx'            | null       | true
        'code'             | '='        | 'xx'            | null       | false
        'description'      | 'contains' | 'An xx'         | null       | true
        'description'      | 'eq'       | ''              | null       | false
        'description'      | 'contains' | null            | null       | false
        'dateManufactured' | 'contains' | new Date().time | 'date'     | false
        'dateManufactured' | 'gt'       | new Date().time | 'date'     | true
        'dateManufactured' | 'lt'       | new Date().time | 'date'     | true
        'dateManufactured' | 'eq'       | new Date().time | 'date'     | true
        'weight'           | 'eq'       | 101             | 'int'      | false
        'weight'           | 'eq'       | 101             | 'long'     | false
        'weight'           | 'lt'       | 101             | 'num'      | true
        'weight'           | 'contains' | 101             | 'num'      | false
        null               | 'eq'       | 101             | null       | false
    }


    def "Test reporting query parameters using unsupported filter terms"() {

        setup:
        // URL:  things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[0][field]':'code',
                       'filter[1][value]':'science',
                       'filter[0][operator]':'eq',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][voodoo]':'ZZ',
                       'max':'50'
                     ]

        when:
        def result = Filter.extractFilters( grailsApplication, params )

        then:
        !result[0].isValid()
        result[1].isValid()

        result[0].field == 'code'
        result[0].operator == 'eq'
        result[0].value == null
        result[1].field == 'description'
        result[1].operator == 'contains'
        result[1].value == 'science'
    }


    def "Test reporting query parameters using unsupported filter operators"() {

        setup:
        // URL:  things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[0][field]':'code',
                       'filter[1][value]':'science',
                       'filter[0][operator]':'startsWith', // not supported!
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][value]':'ZZ',
                       'max':'50'
                     ]

        when:
        def result = Filter.extractFilters( grailsApplication, params )

        then:
        !result[0].isValid()
        result[1].isValid()
    }


    def "Test determining the domain class corresponding to the resource"() {

        when:
        def grailsClass = Filter.getGrailsDomainClass( grailsApplication, 'things' )

        then:
        Thing == grailsClass.clazz
    }


    def "Test creating unfiltered query"() {

        setup:
        // URL: things?max=50
        Map params = [ 'pluralizedResourceName':'things' ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        'SELECT a FROM Thing a' == query.statement
    }


    def "Test creating unfiltered query with paging"() {

        setup:
        for (n in 'A'..'K') { createThing(n) }

        // URL: things?max=50&offset=0&sort=dec&order=code
        Map params = [ 'pluralizedResourceName':'things', 'max':'5', 'offset':'0',
                       'sort':'code', 'order':'desc' ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        "SELECT a FROM Thing a ORDER BY a.code desc" == query.statement
        def things = Thing.executeQuery( query.statement, query.parameters, params )
        things?.size == 5
        things[0].code == 'K'
        things[4].code == 'G'
    }


    def "Test creating simple filtered query"() {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        // URL: things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[0][field]':'code',
                       'filter[1][value]':'An xx',
                       'filter[0][operator]':'eq',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][value]':'xx',
                       'max':'50'
                     ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        'SELECT a FROM Thing a WHERE a.code = :code AND lower(a.description) LIKE lower(:description)' == query.statement

        def things = Thing.executeQuery( query.statement, query.parameters, [:] )
        'xx' == things[0].code
        'An xx thing' == things[0].description
    }


    def "Test creating filter based on a numeric field"() {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        // URL: things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[1][value]':'An xx',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][field]':'weight',
                       'filter[0][operator]':'lt',
                       'filter[0][value]':'101',
                       'filter[0][type]':'num',
                       'max':'50'
                     ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        "SELECT a FROM Thing a WHERE a.weight < :weight AND lower(a.description) LIKE lower(:description)" == query.statement

        def things = Thing.executeQuery( query.statement, query.parameters, [:] )
        1 == things.size()
        'xx' == things[0].code
    }


    def "Test creating filter based on a date field using 'lt' operator"() {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        // URL: things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[1][value]':'An xx',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][field]':'dateManufactured',
                       'filter[0][operator]':'lt',
                       'filter[0][value]': new Long(new Date().time+10000).toString(),
                       'filter[0][type]':'date',
                       'max':'50'
                     ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        "SELECT a FROM Thing a WHERE a.dateManufactured < :dateManufactured AND lower(a.description) LIKE lower(:description)" == query.statement

        def things = Thing.executeQuery( query.statement, query.parameters, [:] )
        1 == things.size()
        'xx' == things[0].code
    }

    def "Test creating filter based on an ISO 8601 date field using 'lt' operator"() {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        Map params = [ 'pluralizedResourceName':'things',
                       'filter[1][value]':'An xx',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][field]':'dateManufactured',
                       'filter[0][operator]':'lt',
                       'filter[0][value]': getISO8601(new Date(new Date().time+10000)),
                       'filter[0][type]':'date',
                       'max':'50'
                     ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        "SELECT a FROM Thing a WHERE a.dateManufactured < :dateManufactured AND lower(a.description) LIKE lower(:description)" == query.statement

        def things = Thing.executeQuery( query.statement, query.parameters, [:] )
        1 == things.size()
        'xx' == things[0].code
    }


    def "Test creating filter based on a date field using 'gt' operator"() {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        // URL: things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[1][value]':'An xx',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][field]':'dateManufactured',
                       'filter[0][operator]':'gt',
                       'filter[0][value]': new Long(new Date().time-10000).toString(),
                       'filter[0][type]':'date',
                       'max':'50'
                     ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        "SELECT a FROM Thing a WHERE a.dateManufactured > :dateManufactured AND lower(a.description) LIKE lower(:description)" == query.statement

        def things = Thing.executeQuery( query.statement, query.parameters, [:] )
        1 == things.size()
    }


    def "Test overriding the domain class"() {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        // URL: complex-things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'complex-things',
                       'filter[0][field]':'code',
                       'filter[1][value]':'An xx',
                       'filter[0][operator]':'eq',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][value]':'XX',
                       'max':'50'
                     ]

        when:
        params << [ domainClass: Filter.getGrailsDomainClass( grailsApplication, 'things' ) ]
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        "SELECT a FROM Thing a WHERE a.code = :code AND lower(a.description) LIKE lower(:description)" == query.statement
    }


    def "Test filtering on an unknown field results in exception"() {

        setup:
        // URL: things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[0][field]':'you_will_not_find_me',
                       'filter[1][value]':'An xx',
                       'filter[0][operator]':'eq',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][value]':'xx',
                       'max':'50'
                     ]

        when:
            def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
            thrown BadFilterException
    }


    def "Test filtering on a 'many' association field results in exception"() {

        setup:
        // URL: things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[0][field]':'parts',
                       'filter[1][value]':'An xx',
                       'filter[0][operator]':'eq',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][value]':'xx',
                       'max':'50'
                     ]

        when:
            def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
            thrown BadFilterException
    }

    def "Testing filtering with an invalid date format results in an exception"() {
      setup:
      Map params = [ 'pluralizedResourceName':'things',
                       'filter[0][field]':'dateManufactured',
                       'filter[0][operator]':'lt',
                       'filter[0][value]':'not a date',
                       'filter[0][type]':'date'
                     ]
      when:
          def query = HQLBuilder.createHQL( grailsApplication, params )

      then:
          thrown BadDateFilterException
    }


    def "Test creating association filtered query"() {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        // URL: things?filter[0][field]=description&filter[1][value]=6&filter[0][operator]=contains&
        // filter[1][field]=thing&filter[1][operator]=eq&filter[0][value]=yy&max=50
        Map params = [ 'pluralizedResourceName':'part-of-things',
                       'filter[0][field]':'description',
                       'filter[1][value]':"$yyId",
                       'filter[0][operator]':'contains',
                       'filter[1][field]':'thing',
                       'filter[1][operator]':'eq',
                       'filter[0][value]':'p1',
                       'max':'50'
                     ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        "SELECT a FROM PartOfThing a INNER JOIN a.thing b WHERE lower(a.description) LIKE lower(:description) AND b.id = :thing" == query.statement

        def things = Thing.executeQuery( query.statement, query.parameters, [:] )
        things?.size == 1
        things[0].code == 'p1'
        things[0].thing.id == yyId
    }


    def "Test creating nested resource filtered query"() {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        // URL: things/6/part-of-things?filter[0][field]=description&filter[0][operator]=contains&
        // filter[0][value]=yy&max=50&offset=2
        Map params = [ 'pluralizedResourceName':'part-of-things',
                       'parentPluralizedResourceName':'things',
                       'parentId':"$yyId",
                       'filter[0][field]':'description',
                       'filter[0][operator]':'contains',
                       'filter[0][value]':'p2',
                       'max':'50',
                       'offset':'2',
                       'sort':'code'
                     ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params )

        then:
        "SELECT a FROM PartOfThing a INNER JOIN a.thing b WHERE b.id = :thing AND lower(a.description) LIKE lower(:description) ORDER BY a.code" == query.statement

        def things = Thing.executeQuery( query.statement, query.parameters )
        things?.size == 1
        things[0].code == 'p2'
        things[0].thing.id == yyId
    }


    def "Test creating simple filtered count() query"() {

        setup:
        // URL: things?filter[0][field]=code&filter[1][value]=science&filter[0][operator]=eq&
        // filter[1][field]=description&filter[1][operator]=contains&filter[0][value]=ZZ&max=50
        Map params = [ 'pluralizedResourceName':'things',
                       'filter[0][field]':'code',
                       'filter[1][value]':'science',
                       'filter[0][operator]':'eq',
                       'filter[1][field]':'description',
                       'filter[1][operator]':'contains',
                       'filter[0][value]':'AA',
                       'max':'50'
                     ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params, true /*count*/ )

        then:
        "SELECT count(a) FROM Thing a WHERE a.code = :code AND lower(a.description) LIKE lower(:description)" == query.statement
    }


    def "Test creating nested resource filtered count() query"() {

        setup:
        def xxId = createThing('xx')
        def yyId = createThing('yy')

        // URL: things/6/part-of-things?filter[0][field]=description&filter[0][operator]=contains&
        // filter[0][value]=yy&max=50&offset=2
        Map params = [ 'pluralizedResourceName':'part-of-things',
                       'parentPluralizedResourceName':'things',
                       'parentId':"$yyId",
                       'filter[0][field]':'description',
                       'filter[0][operator]':'contains',
                       'filter[0][value]':'p2',
                       'max':'50',
                       'offset':'2',
                       'sort':'code'
                     ]

        when:
        def query = HQLBuilder.createHQL( grailsApplication, params, true /*count*/ )

        then:
        "SELECT count(a) FROM PartOfThing a INNER JOIN a.thing b WHERE b.id = :thing AND lower(a.description) LIKE lower(:description)" == query.statement

        def count = Thing.executeQuery( query.statement, query.parameters )
        1 == count[0].toInteger()
    }


    // TODO: Not DRY with RestfulApiControllerFunctionalTests -- move to helper class...
    private def createThing( String code ) {
        Thing.withTransaction {
            Thing thing = new Thing( code: code, description: "An $code thing",
                      dateManufactured: new Date(), isGood: 'Y', isLarge: true )
                .addToParts( new PartOfThing( code: 'p1', description: "$code p1 part" ) )
                .addToParts( new PartOfThing( code: 'p2', description: "$code p2 part" ) )
                .save( failOnError:true, flush:true )
            thing.getId()
        }
    }

    private void deleteThings() {
        Thing.withNewSession {
            Thing.findAll().each() { it.delete( failOnError:true, flush:true ) }
        }
    }

    private String getISO8601(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC")
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        df.setTimeZone(tz)
        df.format(date)
    }
}
