<!-- ***************************************************************************
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
 *************************************************************************** -->

#Example of using iCalendar as a media type for RESTful APIs
Since the plugin supports custom marshalling services, it is straightforward to support APIs that return iCalendar object representations.

##Details
For the sake of this demo, we will use the ical4j library.  Add

    compile 'org.mnode.ical4j:ical4j:1.0.4'

to the dependencies section of your BuildConfig.groovy.

Next, write a simple service that can show a calendar.  For the sake of example, we will create a service that returns a hard-coded calendar for any id:

    package net.hedtech.restfulapi

    import net.fortuna.ical4j.model.*
    import net.fortuna.ical4j.model.property.*

    class CalendarService{

        def show( Map params ) {
            def builder = new ContentBuilder()
            def calendar = builder.calendar() {
                prodid('-//John Smith//iCal4j 1.0//EN')
                version('2.0')
                vevent() {
                    uid('1')
                    dtstamp(new DtStamp())
                    dtstart('20090810', parameters: parameters() {
                        value('DATE')})
                    action('DISPLAY')
                    attach('http://example.com/attachment', parameters: parameters() {
                        value('URI')})
                }
            }
            calendar.validate()

            calendar
        }
    }

This uses the ical4j ContentBuilder class to simplify calendar construction.  We can get a text/calendar representation simply by calling toString() on the calendar.

So now we need a simple custom marshalling service for ical4j Calendars.  The ical4j implementation is doing all the work, so we'll just do a basic check that we've been handed an ical4j Calendar instance:

    package net.hedtech.restfulapi

    import net.hedtech.restfulapi.config.RepresentationConfig

    /**
     * A demonstration class for custom marshalling of iCalendar objects.
     * In this case, we are using ical4j, so we only need to invoke
     * toString on the passed objects.
     */
    class ICalendarMarshallingService {

        String marshalObject(Object o, RepresentationConfig config) {
            if (!(o instanceof net.fortuna.ical4j.model.Calendar)) {
                throw new Exception("Cannot marshal instances of" + o.getClass().getName())
            }
            return o.toString()
        }
    }

Now we just need to configure the resource in the restfulApiConfig:

    restfulApiConfig = {
        resource 'calendars' config {
            methods = ['show']
            representation {
                mediaTypes = ['text/calendar']
                marshallerFramework = 'ICalendarMarshallingService'
            }
        }
    }
