package net.hedtech.restfulapi.config

interface MergeableConfig {
    MergeableConfig merge( MergeableConfig other )
}