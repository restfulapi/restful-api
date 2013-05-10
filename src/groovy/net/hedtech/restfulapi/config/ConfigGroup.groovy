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
        def includes = getIncludesChain( config )
        def stack = includes.reverse(false)
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


    private def getIncludesChain( def config ) {
        def includedConfigs = []
        config.includes.each { name ->
            def includeConfig = getConfig( name )
            includedConfigs.addAll getIncludesChain( includeConfig )
        }
        includedConfigs.add config
        includedConfigs
    }
}