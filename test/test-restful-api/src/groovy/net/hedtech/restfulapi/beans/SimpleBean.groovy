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
package net.hedtech.restfulapi.beans

class SimpleBean {

    String property
    public String publicField
    String property2
    public String publicField2
    protected String protectedField
    private String privateField
    String password
    public transient String transientField
    public static String staticField

    List listProperty
    public List listField

    Map mapProperty
    public Map mapField

    public String propertyAndField

    public String getPropertyAndField() {
        propertyAndField
    }

    public void setPropertyAndField(String s) {
        propertyAndField = s
    }

}
