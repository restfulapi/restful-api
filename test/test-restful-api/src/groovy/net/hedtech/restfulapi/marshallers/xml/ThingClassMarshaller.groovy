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
package net.hedtech.restfulapi.marshallers.xml

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import grails.converters.XML

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

import org.springframework.beans.BeanWrapper


/**
 * A JSON marshaller for the Thing domain class.
 **/
class ThingClassMarshaller extends BasicDomainClassMarshaller {


    protected static final Log log =
        LogFactory.getLog(ThingClassMarshaller.class)


    public ThingClassMarshaller(app) {
        super()
        setApp( app )
    }


    @Override
    protected boolean processField(BeanWrapper beanWrapper,
                                   GrailsDomainClassProperty property, XML xml) {
        Object referenceObject = beanWrapper.getPropertyValue(property.getName())

        // As an example, we'll add a 'numParts' property to the representation
        if ("parts" == property.getName()) {
            xml.startNode('numParts')
            xml.convertAnother(referenceObject.size())
            xml.end()
        }
        return true // and we'll allow the superclass to process parts normally
    }

    @Override
    protected void processAdditionalFields(BeanWrapper beanWrapper, XML xml) {
        super.processAdditionalFields(beanWrapper, xml)
        xml.startNode('sha1')
        xml.convertAnother(beanWrapper.getWrappedInstance().getSupplementalRestProperties()['sha1'])
        xml.end()
    }



}
