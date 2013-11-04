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
package net.hedtech.restfulapi.beans;

import java.util.List;
import java.util.Map;

public class SimpleJavaBean {

    private String property;
    public String publicField;
    private String property2;
    public String publicField2;
    protected String protectedField;
    private String privateField;
    public String password;
    public transient String transientField;
    public static String staticField;

    private List listProperty;
    public List listField;

    private Map mapProperty;
    public Map mapField;

    public String propertyAndField;

    public String getProperty() {
        return property;
    }

    public void setProperty(String s) {
        this.property = s;
    }

    public String getProperty2() {
        return property2;
    }

    public void setProperty2(String s) {
        this.property2 = s;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String s) {
        this.password = s;
    }

    public List getListProperty() {
        return listProperty;
    }

    public void setListProperty(List l) {
        this.listProperty = l;
    }

    public Map getMapProperty() {
        return this.mapProperty;
    }

    public void setMapProperty(Map m) {
        this.mapProperty = m;
    }

    public String getPropertyAndField() {
        return this.propertyAndField;
    }

    public void setPropertyAndField(String s) {
        this.propertyAndField = s;
    }
}
