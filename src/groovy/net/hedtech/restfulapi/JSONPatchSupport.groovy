/* ***************************************************************************
 * Copyright 2015 Ellucian Company L.P. and its affiliates.
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


/**
 * Provides utility functions for applying patches (an extracted JSON Patch)
 * to a resource.
 */
class JSONPatchSupport {

    /**
     * Applies a JSON Patch to the currentState of a resource.
     * @param  patches the List of patches to apply in order
     * @param  currentState a Map representing the current state of a resource
     */
    static
    public def applyPatches( List patches, Map currentState ) {

        def patchedState = deepCopy(currentState)
        patches.each { patch ->

            String op = patch['op']
            List pathList = pathListFor(patch['path'])
            def value = (op == 'add' || op == 'replace') ? patch['value'] : null

            switch (op) {
                case 'add':
                    patchedState = add(patchedState, pathList, value)
                    break
                case 'move':
                    List fromPathList = pathListFor(patch['from'])
                    patchedState = move(patchedState, fromPathList, pathList)
                    break
                case 'remove':
                    patchedState = remove(patchedState, pathList)
                    break
                case 'replace':
                    patchedState = replace(patchedState, pathList, value)
                    break
            }
        }
        patchedState
    }


    static
    private def add( Map currentState, List pathList, def value ) {

        def obj = deepCopy( currentState )

        if (pathList?.size() == 1) {
            if (exists( obj, pathList )) {
                def prop = getProp(obj, pathList)
                if (isArray(prop)) {
                    prop << value
                    return obj
                } else {
                    throw new RuntimeException("Path $pathList already exists")
                }
            }
            else {
               obj.put("${pathList[0]}", value)
               return obj
            }
        } else {
            def prop = getProp(obj, pathList)
            if (prop) {
                if (isArray(prop)) {
                    // if the path is to an array, we'll just add the value to it
                    prop << value
                    return obj
                }
                throw new RuntimeException("Path $pathList already exists")
            }
            else {
                // prop does not exist, we'll either add to a parent array or parent object
                def parent = getProp(obj, pathList[0..-2])
                if (isArray(parent)) parent.putAt(pathList[-1], value)
                else                 parent?.put(pathList[-1], value)
                return obj
            }
        }
    }


    static
    private def move( Map currentState, List fromPathList, List toPathList ) {

        def obj = deepCopy( currentState )

        def value = getProp( obj, fromPathList )
        obj = remove( obj, fromPathList )
        return add( obj, toPathList, value )
    }


    static
    private def remove( Map currentState, List pathList ) {

        def obj = deepCopy(currentState)

        if (pathList.size() == 1) {
            obj?.remove(pathList[0])
        }
        else  {
            def parent
            def node = obj
            pathList[0..-2].each {
                parent = node
                node = node[it]
            }
            // don't leave an empty map sitting as an array element...
            if (isArray(parent) && node?.size() == 1) {
                parent.remove(pathList[-2])
            }
            else {
                node?.remove(pathList[-1])
            }
        }
        return obj
    }


    static
    private def replace( Map currentState, List pathList, def value ) {

        def obj = deepCopy(currentState)
        if (!exists(obj, pathList)) throw new RuntimeException("Path $pathList not found")

        if (pathList.size() == 0) {
            return value
        } else if (pathList.size() == 1) {
            obj["${pathList[0]}"] = value
            return obj
        } else {
            def parent = getProp(obj, pathList[0..-2])
            if (isArray(parent))  parent.putAt(pathList[-1], value)
            else                  parent[pathList[-1]] = value
            return obj
        }
    }


    static
    private boolean exists( def obj, List pathList) {
        switch (pathList?.size()) {
            case 0:
                return true; // an 'empty' path for our purposes always exists
            case 1:
                return getProp(obj, pathList) != null
            default:
                def parent = getProp(obj, pathList[0..-2])
                if (isArray(parent)) return parent.getAt(pathList[-1]) != null
                else                 return parent[pathList[-1]] != null
        }
    }


    static
    private boolean isArray( obj ) {
        obj != null && (obj.getClass().isArray() || obj instanceof List)
    }


    static
    private List pathListFor( pointer ) {

        List paths = pointer.tokenize('/')
        // convert ~0 and ~1 per http://tools.ietf.org/html/rfc6901#section-4
        paths = paths.collect { it.replaceAll("~1", "/").replaceAll("~0", "~") }
        paths = paths.collect { it.isInteger() ? it.toInteger() : it }
        paths
    }


    static
    private def deepCopy( obj ) {

        def byteOut = new ByteArrayOutputStream()
        def out = new ObjectOutputStream( byteOut )
        out.writeObject( obj )
        out.flush()
        def byteIn = new ByteArrayInputStream( byteOut.toByteArray() )
        def inStream = new ThreadContextObjectInputStream( byteIn )
        return inStream.readObject()
    }


    static
    private getProp( def object, List pathList ) {
        pathList.inject object, { obj, prop -> obj[prop] }
    }
}

class ThreadContextObjectInputStream extends ObjectInputStream {

    ThreadContextObjectInputStream( ByteArrayInputStream bais ) {
        super( bais )
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
    throws IOException, ClassNotFoundException {

        Class.forName( desc.getName(), true, Thread.currentThread().contextClassLoader )
    }
}
