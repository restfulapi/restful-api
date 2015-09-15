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
package net.hedtech.restfulapi.config

/**
 * A holder class for grouping named MergeableConfig instances
 * together and providing merge capabilities for them.
 */
class ConfigGroup {
    def configs = [:]

    MergeableConfig getConfig(String name) {
        def config = configs[name]
        if (config == null) {
            throw new ConfigGroupMissingConfigException( name:name )
        }
        config
    }

    MergeableConfig getMergedConfig( MergeableConfig config ) {
        //resolve named references to domain templates in order
        def inherits = resolveInherited( config )
        def stack = inherits.reverse(false)
        //merge all configurations, so that later named
        //configs override or augment earlier ones
        while (stack.size() > 1) {
            def one = stack.pop()
            def two = stack.pop()
            def merged = one.merge(two)
            stack.push(merged)
        }
        //This is now the combined configuration
        config = stack.pop()
        config
    }


    private def resolveInherited( def config ) {
        def configs = []
        config.inherits.each { name ->
            def includeConfig = getConfig( name )
            configs.addAll resolveInherited( includeConfig )
        }
        configs.add config
        configs
    }
}
