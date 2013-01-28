<!-- ******************************************************************** 
     Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************** --> 

# TODO list for work

* Define strategy and guidelines for retaining version-specific representations. Can the general marshallers be used at all?  If so, are tests sufficient to drive develpment of version-specific marshallers?  Need discussion, and Shane input...

* Demonstrate use with complex objects
* Demonstrate use of resource-specific marshallers (versus generic ones).
* Demonstrate use of custom XML marshallers
* Implement create/update/delete actions
* Design/implement support for reading JSON and XML (if needed because Grails cannot do this)

* Add to the generic RestfulApiController support for handling ApplicationException (but avoid ApplicationException dependency - if an exception responds to AE methods we can pass in a localizer to the 'returnMap' method otherwise we should just use 'e.message')
* Support localization of label/title affordances (just use 'message' injected into the controller)
* Add a groovy bean class marshaler to support non-domain objects
* Investigate why the test application needed to install the 'inflector' plugin? (this should be transitive) 
* Investigate why 2nd run of tests in grails console causes failures of the inflector plugin
* Encapsulate the Inflector plugin within our own 'Inflector' class that exposes static methods (versus letting marshallers use the Inflector plugin directly)

* Use a filter to calculate and add the ETag header SHA1 to the response
* Implement 'unit' tests of UrlMapping and Controller actions (in addition to functional tests)
* Demonstrate use of a custom controller
* Add affordance support to marshalers, and demonstrate usage in a custom controller
* Implement support for conditional requests (TBD)

* Investigate returning absolute URI in Location header on create (how to if behind load balancer?)
* Firm up contract with service save() method (what is returned in the result, optional id only, etc)
* Firm up contract with service save, update, and delete methods.  Do they get the full request params, or only the extracted resource representation?
* Make decision on exposure of id in a URI in Location header.  Is it always an internal key, or do we need resource keys that may differ from internal keys
* Is the format of an error response to be configurable via marshallers?
* Is the format of an error response considered to be versioned?  
* Determine mapping of Exceptions to errorMap.  
* Does errorMap production need to take the action into account?  e.g., is producing an error response w/ classname and id appropriate for a list action?

* Support for Date binding

# Code cleanup tasks
* setup/teardown methods for functional tests to setup/teardown persistent data?

# Current Status
* Created an initial plugin project and test-app (that uses an in-memory database versus a Banner dependency)
* Partially implemented a generic controller with content negotiation that selects a registered marshaller. (Only show/list are implemented.)
* Implemented a generic 'basic' marhsaller, and a placeholder for a generic 'HAL' marshaller
* Implemented a trivial resource-specific marshaller 
* Implemented initial functional test to prove use of various marshallers
* Implemented support for converting urls to resource names (e.g., part-of-things ==> PartOfThing) that leverages the 'inflector' plugin




