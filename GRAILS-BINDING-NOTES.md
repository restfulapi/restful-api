<!-- ***************************************************************************
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
 *************************************************************************** -->

#Observations on grails data binding

The work for associations occurs in GrailsDataBinder in the preProcessMutablePropertyValues; specifically, in the bindAssociations() call.  The value of the property that holds id's for associated objects either needs to be a java.lang.String representing the id (not a GString), or a java array of id's - an ArrayList will not work.

##Many-to-one
Set the association property to a string representing a single id (e.g author.id='1'; the map would be ['nose.id':1]).
Can modify properties of associated object: ['nose.id':1,'nose.name':'bar']
if the many side has a belongsTo, that field will not have a value until save is called on the containing object.

##One-to-one
Works the same as many-to-one

##One-to-many
The data binding will clear the collection for the association, and invoke the "addTo" method for the property to add each associated object.

If you only want to associate (not change properties of associated objects), you can just use a java array:

    ['books':[1,2].toArray()]

Can also use index form:

    ['books[0].id':1,'books[1].id':2]

With index form, can both associate and bind values:

    ['books[0].id':1,'books[1].id':2,'books[1].isbn':'1234']

If the many side has a belongsTo, it is populated during binding when the many side is created.

In general, depending on the grails data binding and the indexed form can be dangerous, as it requires the sub-object representations in the inbound message to be in a specific order.  Grails data binding works on the index, not the id.  For example, suppose we have a one-to-many relationship between Authors and Book, where an author has many books.  We have an existing author with 3 books; the IDs of these books are 1, 2, and 3, titles 'book1', 'book2', 'book3' and they are stored within the author in the same order.  So the books array within author looks like:

    [[book: id=1 title:book1], [book: id=2 title:book2], [book: id=3 title:book3]]

    Suppose we use grails data binding to bind the following map to author:

    ['books[0].id':2,'books[0].title':'changed']

What this is telling grails is the associated object at index 0 (which might be random if the association is stored in a set) should be changed to reference a Book with id 2, and then that book should have its title changed.  So the end result of the bind is a collection of books for the author that looks like:

    [[book: id=2 title:changed], [book: id=2 title:changed], [book: id=3 title:book3]]

This demonstrates that if you do deep-binding of sub-objects, your representations become 'brittle' in that your implementation and the client must agree on an implicit ordering or sub-objects.

##Many-to-many
Whether the owned side is updated or not depends on the input map.  In a many-to-many association where Book belongsTo Author, if a book exists, and you update the collection on the author side as:

    [books:[1].toArray()]

then the bidirectional nature of the association is set correctly; that is, the book with id 1 will have the author added to its collection of authors.
However, if you use index notation to add to the collection:

    ['books[0].id':1]

and bind this to an Author instance, then the Author instance will have the book in its collection, but the Book with id 1 **will not** have the author in its author collection.  This appears to be a limitation of the GrailsDataBinder.

##POGOs (plain old groovy objects)
Can use the grails databinder class directly, or depend on the groovy map constructor.
For POGOs that aggregate domain objects, the service can break the map into pieces, one for each domain class, and bind separately.

##General data binding strategies
This is not meant to be comprehensive or required - the product architect and technical leads on each product will have to decide for themselves the best data-binding strategy for their application.  However, some general recommendations can be made.

* Try to keep things simple.  Most of the complicatins in databinding occur if you try to use representations from a 'include the kitchen sink approach' where create/update methods have to accept, bind, and process representations that have many levels of fully represented sub-objects.
* If you are doing sub-objects, you will have to decide whether to try to manipulate the incoming representation into a dot-notation format that can instanstiate and bind to sub-objects, or manually instantiate the sub-objects themselves.  You can use the declarative extractor support in the restfulapi plugin to flatten maps for you, but you must be careful - grails data binding selects the sub-object to bind values to based on the index, not the primary key.  This means that if you use the flattening facility, the caller **must** send back representations with sub-objects in the correct order.  For this reason, it is recommended that for associated objects, you either use short-objects, so that the operation can associate a resource with other resources, but does not set properties on the associated resource (make additional calls to modify them).  If this is not desirable, then it is recommended that you perform custom data binding on the sub-objects, in order to remove the need for users of the API to worry about sub-object ordering.
