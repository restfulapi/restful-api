/*****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi.extractors

import grails.test.mixin.*
import spock.lang.*

import net.hedtech.restfulapi.extractors.configuration.*
import net.hedtech.restfulapi.extractors.json.*

class MapTransformerSpec extends Specification {

    def "Test top-level rename"() {
        setup:
        def map = ['one':'v1','two':'v2','three':'v3']
        def transformer = new MapTransformer()
        transformer.addRenameRule(['one'], 'fieldOne')
        transformer.addRenameRule(['three'], 'fieldThree')

        when:
        map = transformer.transform(map)

        then:
        ['fieldOne':'v1','two':'v2','fieldThree':'v3'] == map
    }

    def "Test nested rename"() {
        setup:
        def map = ['outer1':['inner1':'v1','inner2':'v2'],
                   'outer2':['inner1':'v1'] ]
        def transformer = new MapTransformer()
        transformer.addRenameRule(['outer1','inner1'],'renamed')

        when:
        map = transformer.transform(map)

        then:
        ['outer1':['renamed':'v1','inner2':'v2'],
         'outer2':['inner1':'v1'] ] == map
    }

    def "Test rename across collection"() {
        setup:
        def map = [collection:[
            ['customer':'123'],
            ['customer':'456'],
            ['customer':'789']
        ]]
        def transformer = new MapTransformer()
        transformer.addRenameRule(['collection','customer'],'customerId')

        when:
        map = transformer.transform(map)

        then:
        [collection:[
            ['customerId':'123'],
            ['customerId':'456'],
            ['customerId':'789']
        ]] == map
    }

    def "Test depth-first transformation"() {
        setup:
        def map = [outer:[inner:'123']]
        def transformer = new MapTransformer()
        transformer.addRenameRule(['outer'], 'newOuter')
        transformer.addRenameRule(['outer','inner'], 'newInner')

        when:
        map = transformer.transform(map)

        then:
        [newOuter:[newInner:'123']] == map
    }

    def "Test simple default"() {
        setup:
        def map = [field1:'123']
        def transformer = new MapTransformer()
        transformer.addDefaultValueRule(['field2'],true)

        when:
        map = transformer.transform(map)

        then:
        [field1:'123','field2':true]
    }

    def "Test nested default"() {
        setup:
        def map = ['outer1':['inner1':'v1'],
                   'outer2':['inner1':'v1'] ]
        def transformer = new MapTransformer()
        transformer.addDefaultValueRule(['outer1','default'],true)

        when:
        map = transformer.transform(map)

        then:
        ['outer1':['inner1':'v1','default':true],
         'outer2':['inner1':'v1'] ] == map
    }

    def "Test default across collection"() {
        setup:
        def map = [collection:[
            ['customer':'123'],
            ['customer':'456'],
            ['customer':'789']
        ]]
        def transformer = new MapTransformer()
        transformer.addDefaultValueRule(['collection','boolean'],true)

        when:
        map = transformer.transform(map)

        then:
        [collection:[
            ['customer':'123','boolean':true],
            ['customer':'456','boolean':true],
            ['customer':'789','boolean':true]
        ]] == map
    }

    def "Test default does not overwrite non-null value"() {
        setup:
        def map = [name:'foo']
        def transformer = new MapTransformer()
        transformer.addDefaultValueRule(['name'],'bar')

        when:
        map = transformer.transform(map)

        then:
        [name:'foo'] == map
    }

    def "Test default does not overwrite null value"() {
        setup:
        def map = [name:null]
        def transformer = new MapTransformer()
        transformer.addDefaultValueRule(['name'],'bar')

        when:
        map = transformer.transform(map)

        then:
        [name:null] == map
    }

    def "Test renaming and defaulting same optional property"() {
        setup:
        def map = [collection:[['req':123,'optional':456],['req':234]]]
        def transformer = new MapTransformer()
        transformer.addRenameRule(['collection','optional'],'req2')
        transformer.addDefaultValueRule(['collection','optional'],0)

        when:
        map = transformer.transform(map)

        then:
        [collection:[['req':123,'req2':456],['req':234,'req2':0]]] == map
    }

    def "Test intermediate objects are not created"() {
        setup:
        def map = [collection:[],map:null]
        def transformer = new MapTransformer()
        transformer.addDefaultValueRule(['collection','req'],0)
        transformer.addDefaultValueRule(['map','req'],0)

        when:
        map = transformer.transform(map)

        then:
        [collection:[],map:null] == map
    }

    def "Test modify value"() {
        setup:
        def map = [_link:"/customers/12"]
        def transformer = new MapTransformer()
        transformer.addModifyValueRule(['_link'],{def v -> v.substring(v.lastIndexOf('/')+1)})

        when:
        map = transformer.transform(map)

        then:
        ['_link':'12'] == map
    }

    def "Test modify nested value"() {
        setup:
        def map = [inner:[name:'foo','customer':['_link':'/customers/15']]]
        def transformer = new MapTransformer()
        transformer.addModifyValueRule(['inner','customer'],{def m -> def v = m['_link']; v.substring(v.lastIndexOf('/')+1)})

        when:
        map = transformer.transform(map)

        then:
        [inner:[name:'foo', 'customer':'15']] == map
    }

    def "Test modify collection"() {
        setup:
        def map  = [customers:[['_link':'/customers/1'],['_link':'/customers/2']]]
        def transformer = new MapTransformer()
        transformer.addModifyValueRule(['customers'],
            { value ->
                def result = []
                value.each {
                    if (it instanceof Map) {
                        def v = it['_link']
                        result.add(v.substring(v.lastIndexOf('/')+1))
                    }
                }
                return result
            }
        )

        when:
        map = transformer.transform(map)

        then:
        [customers:['1','2']] == map

    }

    def "Test flatten map"() {
        setup:
        def map = [name:'foo',customer:[name:'widgetco',preferred:true]]
        def transformer = new MapTransformer()
        transformer.addFlattenRule(['customer'])

        when:
        map = transformer.transform(map)

        then:
        [name:'foo','customer.name':'widgetco', 'customer.preferred':true] == map
    }

    def "Test flatten maps in a collection"() {
        setup:
        def map = [name:'foo',customers:[[name:'w1',preferred:true],[name:'w2',preferred:false]]]
        def transformer = new MapTransformer()
        transformer.addFlattenRule(['customers'])

        when:
        map = transformer.transform(map)

        then:
        [name:'foo','customers[0].name':'w1','customers[0].preferred':true, 'customers[1].name':'w2', 'customers[1].preferred':false] == map
    }

    def "Test flatten nested maps in collections"() {
        setup:
        def map = [name:'foo',
            customers:[
                [name:'w1', address:[street:'street1', zip:'11111']],
                [name:'w2', address:[street:'street2', zip:'22222']]
            ],
            product:[name:'p1', parts:[ [partNo:123, desc:'part1'], [partNo:456, desc:'part2'] ]]
        ]
        def transformer = new MapTransformer()
        transformer.addFlattenRule(['customers'])
        transformer.addFlattenRule(['customers','address'])
        transformer.addFlattenRule(['product','parts'])
        transformer.addFlattenRule(['product'])

        def expected = [name:'foo',
        'customers[0].name':'w1', 'customers[0].address.street':'street1', 'customers[0].address.zip':'11111',
        'customers[1].name':'w2', 'customers[1].address.street':'street2', 'customers[1].address.zip':'22222',
        'product.name':'p1',
        'product.parts[0].partNo':123, 'product.parts[0].desc':'part1',
        'product.parts[1].partNo':456, 'product.parts[1].desc':'part2'
         ]

        when:
        map = transformer.transform(map)

        then:
        expected == map
    }

    def "Test flatten after rename"() {
        setup:
        def map = [name:'foo',customer:[name:'widgetco',preferred:true]]
        def transformer = new MapTransformer()
        transformer.addFlattenRule(['customer'])
        transformer.addRenameRule(['customer'],'myCustomer')

        when:
        map = transformer.transform(map)

        then:
        [name:'foo','myCustomer.name':'widgetco', 'myCustomer.preferred':true] == map

    }

    def "Test flatten maps in a collection after rename"() {
        setup:
        def map = [name:'foo',customers:[[name:'w1',preferred:true],[name:'w2',preferred:false]]]
        def transformer = new MapTransformer()
        transformer.addFlattenRule(['customers'])
        transformer.addRenameRule(['customers'],'myCustomers')
        transformer.addRenameRule(['customers','name'],'companyName')

        when:
        map = transformer.transform(map)

        then:
        [name:'foo','myCustomers[0].companyName':'w1','myCustomers[0].preferred':true, 'myCustomers[1].companyName':'w2', 'myCustomers[1].preferred':false] == map
    }

    def "Test flatten nested maps in collections after rename"() {
        setup:
        def map = [name:'foo',
            customers:[
                [name:'w1', address:[street:'street1', zip:'11111']],
                [name:'w2', address:[street:'street2', zip:'22222']]
            ],
            product:[name:'p1', parts:[ [partNo:123, desc:'part1'], [partNo:456, desc:'part2'] ]]
        ]
        def transformer = new MapTransformer()
        transformer.addFlattenRule(['customers'])
        transformer.addFlattenRule(['customers','address'])
        transformer.addFlattenRule(['product','parts'])
        transformer.addFlattenRule(['product'])
        transformer.addRenameRule(['customers'], 'myCustomers')
        transformer.addRenameRule(['customers','address'],'companyAddress')
        transformer.addRenameRule(['product','parts'],'myParts')
        transformer.addRenameRule(['product'],'myProduct')

        def expected = [name:'foo',
        'myCustomers[0].name':'w1', 'myCustomers[0].companyAddress.street':'street1', 'myCustomers[0].companyAddress.zip':'11111',
        'myCustomers[1].name':'w2', 'myCustomers[1].companyAddress.street':'street2', 'myCustomers[1].companyAddress.zip':'22222',
        'myProduct.name':'p1',
        'myProduct.myParts[0].partNo':123, 'myProduct.myParts[0].desc':'part1',
        'myProduct.myParts[1].partNo':456, 'myProduct.myParts[1].desc':'part2'
         ]

        when:
        map = transformer.transform(map)

        then:
        expected == map
    }

}