package io.armory.plugin.stage.pulumi

import com.netflix.spinnaker.kork.plugins.api.ExtensionConfiguration
import io.armory.plugin.stage.pulumi.model.Credentials

/**
 * Data in this class maps to the plugin configuration in a service's config YAML.
 * The data can be key/value pairs or an entire configuration tree.
 *
 */
@ExtensionConfiguration("pulumi")
data class PulumiConfig(
        var credentials: Credentials
)
