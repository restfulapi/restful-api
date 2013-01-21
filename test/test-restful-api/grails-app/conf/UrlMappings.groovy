/* ***************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

class UrlMappings {

    static mappings = {

        // Mappings supported by resource-specific controllers
        // should be added before the default mapping used for  
        // resources handled by the default RestfulApiController. 

        // name otherthingRestfulApi: "api/other-things/$id"

        // Default controller to handle RESTful API requests.
        // Place URL mappings to specific controllers BEFORE this mapping.
        //
        "/api/$pluralizedResourceName/$id" {
            controller   = 'restfulApi'
            action = [GET: "show", PUT: "update", 
                      DELETE: "delete", POST: "save"]
            parseRequest = true
            constraints {
                // to constrain the id to numeric, uncomment the following:
                // id matches: /\d+/
            }
        }
        "/api/$pluralizedResourceName" {
            controller   = 'restfulApi'
            action = [GET: "list"]
            parseRequest = true
        }


        "/"(view:"/index")
        "500"(view:'/error')
    }
}
