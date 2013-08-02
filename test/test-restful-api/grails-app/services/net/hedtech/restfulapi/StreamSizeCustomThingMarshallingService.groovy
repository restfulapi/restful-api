/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
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
