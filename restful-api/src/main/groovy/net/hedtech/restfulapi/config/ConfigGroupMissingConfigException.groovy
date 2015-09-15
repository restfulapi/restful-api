/* ***************************************************************************
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
package net.hedtech.restfulapi.config


/**
 * Exception thrown when a domain class marshaller definition includes
 * an unknown template.
 **/
class MissingDomainMarshallerTemplateException extends RuntimeException {
    String name

    String getMessage() {
        "Domain Class Marshaller template $name not defined.  Marshaller templates must be defined before use."
    }
}
