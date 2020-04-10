package io.armory.plugin.stage.pulumi

import io.armory.plugin.stage.pulumi.exception.SimpleStageException

/**
 * Context is used within the stage itself and returned to the Orca pipeline execution.
 */
data class Context(var maxWaitTime: Int? = null, var exception: SimpleStageException? = null) {}
