/* ****************************************************************************
Copyright 2013 Ellucian Company L.P. and its affiliates.
******************************************************************************/
package net.hedtech.restfulapi.config

interface MergeableConfig {
    /** Merges two configurations instances together
     * The values of the other instance augment or override
     * the settings in this instance.
     */
    MergeableConfig merge( MergeableConfig other )
}
