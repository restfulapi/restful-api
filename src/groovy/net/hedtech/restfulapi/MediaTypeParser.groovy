/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/

package net.hedtech.restfulapi

/**
 * Parser for headers that contain media types.
 * Based on the grails DefaultAcceptHeaderParser, but
 * parses the header without taking into consideration
 * whether the type is registered in the grail config.
 **/
class MediaTypeParser {

    MediaTypeParser() {}

    MediaType[] parse(String header) {
        def mimes = []
        def qualifiedMimes = []

        if (!header) {
            return []
        }

        def tokens = header.split(',')
        for (t in tokens) {
            if (t.indexOf(';') > -1) {
                t = t.split(';')
                def params = [:]
                t[1..-1].each {
                    def i = it.indexOf('=')
                    if (i > -1) {
                        params[it[0..i-1].trim()] = it[i+1..-1].trim()
                    }
                }
                if (params) {
                    createMediaTypeAndAddToList(t[0].trim(), mimes, params)
                }
                else {
                    createMediaTypeAndAddToList(t[0].trim(), mimes)
                }
            }
            else {
                createMediaTypeAndAddToList(t.trim(), mimes)
            }
        }

        if (!mimes) {
            return mimes
        }

        // remove duplicate text/xml and application/xml entries
        MediaType textXml = mimes.find { it.name == 'text/xml' }
        MediaType appXml = mimes.find { it.name ==  MediaType.XML }
        if (textXml && appXml) {
            // take the largest q value
            appXml.parameters.q = [textXml.parameters.q.toBigDecimal(), appXml.parameters.q.toBigDecimal()].max()

            mimes.remove(textXml)
        } else if (textXml) {
            textXml.name = MediaType.XML
        }

        if (appXml) {
            // prioritise more specific XML types like xhtml+xml if they are of equal quality
            def specificTypes = mimes.findAll { it.name ==~ /\S+?\+xml$/ }
            def appXmlIndex = mimes.indexOf(appXml)
            def appXmlQuality = appXml.parameters.q.toBigDecimal()
            for (mime in specificTypes) {
                if (mime.parameters.q.toBigDecimal() < appXmlQuality) continue

                def mimeIndex = mimes.indexOf(mime)
                if (mimeIndex > appXmlIndex) {
                    mimes.remove(mime)
                    mimes.add(appXmlIndex, mime)
                }
            }
        }
        return mimes.sort(new QualityComparator()) as MediaType[]
    }

    private createMediaTypeAndAddToList(name, mimes, params = null) {
        def mime = params ? new MediaType(name, params) : new MediaType(name)
        mimes << mime
    }
}

class QualityComparator implements Comparator {

    int compare(Object t, Object t1) {
        def left = t.parameters.q.toBigDecimal()
        def right = t1.parameters.q.toBigDecimal()
        if (left > right) return -1
        if (left < right) return 1
        return 0
    }
}
