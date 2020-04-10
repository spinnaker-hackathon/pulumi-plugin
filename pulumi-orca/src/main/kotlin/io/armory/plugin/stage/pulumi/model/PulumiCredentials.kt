package io.armory.plugin.stage.pulumi.model;

/**
 * Pulumi credentials
 */
data class PulumiCredentials(
        var current: String?,
        var accessTokens: Map<String?, String?>,
        var accounts : Map<String?, Account>
)

data class Account (
        val serverUri: String?,
        var accessToken: String?,
        var username: String?
)


