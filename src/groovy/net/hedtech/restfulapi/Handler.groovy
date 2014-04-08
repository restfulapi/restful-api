/* ***************************************************************************
 * Copyright 2014 Ellucian Company L.P. and its affiliates.
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

/*
Attribution:
The handler concept is based on the grails converters and related configuration work
by Siegfried Puchbauer.
*/
/**
 * A handler for a given type, T.
 * Handlers must be able to answer true or false to the question of whether
 * they support an instance of the type T.
 **/
interface Handler<T> {
    boolean supports(T object)
}
