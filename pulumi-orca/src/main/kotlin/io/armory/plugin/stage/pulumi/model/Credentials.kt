package io.armory.plugin.stage.pulumi.model;

/**
 * Cloud Provider credentials
 */
data class Credentials(
        var secretKeyId: String?,
        var secretAccessKey: String?
)

