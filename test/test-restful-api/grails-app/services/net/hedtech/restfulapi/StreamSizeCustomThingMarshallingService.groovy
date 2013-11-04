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
package net.hedtech.restfulapi

import groovy.xml.MarkupBuilder

import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

import net.hedtech.restfulapi.config.RepresentationConfig
import net.hedtech.restfulapi.marshallers.MarshallingService
import net.hedtech.restfulapi.marshallers.StreamWrapper

/**
 * A demonstration class for a custom marshalling service.
 * Uses a framework other than the grails converters to marshall
 * objects.
 */
class StreamSizeCustomThingMarshallingService {

    static transactional = false


    StreamWrapper marshalObject(Object o, RepresentationConfig config) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)

        if (o instanceof Collection) {
            Collection list = (Collection) o
            xml.list() {
                list.each {
                    code(it.code)
                }
            }
        }
        else {
            xml.thing() {
                code(o.code)
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ZipOutputStream zipFile = new ZipOutputStream(baos)
        zipFile.putNextEntry(new ZipEntry('content'))
        zipFile.write(writer.toString().getBytes('UTF-8'))
        zipFile.closeEntry()
        zipFile.flush()
        zipFile.close()
        byte[] bytes = baos.toByteArray()
        return new StreamWrapper(stream:new ByteArrayInputStream(bytes), totalSize:bytes.length)
    }
}
