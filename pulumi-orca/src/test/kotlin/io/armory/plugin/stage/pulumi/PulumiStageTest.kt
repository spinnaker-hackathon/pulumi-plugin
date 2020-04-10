package io.armory.plugin.stage.pulumi

import com.netflix.spinnaker.orca.api.simplestage.SimpleStageInput
import com.netflix.spinnaker.orca.api.simplestage.SimpleStageStatus
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.armory.plugin.stage.pulumi.model.Credentials
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PulumiStageTest : JUnit5Minutests {

    fun tests() = rootContext {

        test("pulumi should throw an error when AWS credentials are not set"){

            // given
            val config = PulumiConfig(Credentials("123", "234"))
            val input = PulumiInput("aws", "https://github.com/castanedaci/pulumi-aws-typescript", "master")

            // when
            val result = PulumiStage(config).execute(SimpleStageInput(input))

            // then
            expectThat(result.status).isEqualTo(SimpleStageStatus.TERMINAL)
        }
    }
}
