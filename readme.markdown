<!-- ******************************************************************** 
     Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************** --> 

# TODO list for work (turn into Jira)

# Things to consider (may need user stories)
* Define strategy and guidelines for retaining version-specific representations. Can the general marshallers be used at all?  If so, are tests sufficient to drive develpment of version-specific marshallers?  Need discussion, and Shane input...
* Firm up contract with service save() method (what is returned in the result, optional id only, etc)
* Firm up contract with service save, update, and delete methods.  Do they get the full request params, or only the extracted resource representation?
* Support for Date binding

# Code cleanup tasks


# Current Status
* Created an initial plugin project and test-app (that uses an in-memory database versus a Banner dependency)
* Partially implemented a generic controller with content negotiation that selects a registered marshaller. (Only show/list are implemented.)
* Implemented a generic 'basic' marhsaller, and a placeholder for a generic 'HAL' marshaller
* Implemented a trivial resource-specific marshaller 
* Implemented initial functional test to prove use of various marshallers
* Implemented support for converting urls to resource names (e.g., part-of-things ==> PartOfThing) that leverages the 'inflector' plugin
* Demonstrate use of resource-specific marshallers (versus generic ones).
* Implement create/update/delete actions
* Support localization of label/title affordances (just use 'message' injected into the controller)
* Encapsulate the Inflector plugin within our own 'Inflector' class that exposes static methods (versus letting marshallers use the Inflector plugin directly)




