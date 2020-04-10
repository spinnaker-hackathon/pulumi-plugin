package io.armory.plugin.stage.pulumi.command

/**
 * Command Response
 */
data class CommandResponse(
        var exitCode: Int?,
        var result: String
)