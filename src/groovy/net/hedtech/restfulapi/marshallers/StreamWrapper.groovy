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
package net.hedtech.restfulapi.marshallers

import net.hedtech.restfulapi.config.RepresentationConfig

/**
 * A wrapper for InputStreams that can convey the total number
 * of bytes that will be returned by the stream.
 * Can be returned by a marshalling service in order to set
 * the Content-Length header.
 **/
class StreamWrapper {

    InputStream stream
    int totalSize
}
