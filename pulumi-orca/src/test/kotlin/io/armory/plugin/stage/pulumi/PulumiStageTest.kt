package io.armory.plugin.stage.pulumi

import com.netflix.spinnaker.orca.api.simplestage.SimpleStageInput
import com.netflix.spinnaker.orca.api.simplestage.SimpleStageStatus
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.armory.plugin.stage.pulumi.model.Account
import io.armory.plugin.stage.pulumi.model.Credentials
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PulumiStageTest : JUnit5Minutests {

    fun tests() = rootContext {

        test("pulumi should throw an error when AWS credentials are not set"){

            // given
            val config = PulumiConfig(Credentials(System.getenv("AWS_ACCESS_KEY_ID"), System.getenv("AWS_SECRET_ACCESS_KEY")), Account("https://api.pulumi.com", "pul-token", "username"))
            val input = PulumiInput("aws", "https://github.com/castanedaci/pulumi-aws-typescript", "master", "dev")

            // when
            val result = PulumiStage(config).execute(SimpleStageInput(input))

            // then
            expectThat(result.status).isEqualTo(SimpleStageStatus.TERMINAL)
        }
    }
}
