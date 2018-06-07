/* ***************************************************************************
 * Copyright 2018 Ellucian Company L.P. and its affiliates.
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
package net.hedtech.restfulapi.config

/**
 * Exception thrown when the representationMetadata defined for
 * a representation of a resource is not a map.
 **/
class RepresentationMetadataNotMapException extends RuntimeException {
    String resourceName
    String mediaType

    String getMessage() {
        "Resource $resourceName with representation $mediaType representationMetadata does not define a map"
    }
}
