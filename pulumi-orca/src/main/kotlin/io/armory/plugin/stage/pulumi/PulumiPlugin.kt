package io.armory.plugin.stage.pulumi

import com.google.gson.Gson
import com.netflix.spinnaker.orca.api.simplestage.SimpleStage
import com.netflix.spinnaker.orca.api.simplestage.SimpleStageInput
import com.netflix.spinnaker.orca.api.simplestage.SimpleStageOutput
import com.netflix.spinnaker.orca.api.simplestage.SimpleStageStatus
import io.armory.plugin.stage.pulumi.command.PulumiCli
import io.armory.plugin.stage.pulumi.exception.SimpleStageException
import io.armory.plugin.stage.pulumi.exception.SimpleStageExceptionDetails
import io.armory.plugin.stage.pulumi.model.Account
import io.armory.plugin.stage.pulumi.model.Credentials
import io.armory.plugin.stage.pulumi.model.PulumiCredentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.pf4j.Extension
import org.pf4j.Plugin
import org.pf4j.PluginWrapper
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL


class PulumiPlugin(wrapper: PluginWrapper) : Plugin(wrapper) {
    private val logger = LoggerFactory.getLogger(PulumiPlugin::class.java)

    override fun start() {
        logger.info("PulumiPlugin.start()")
    }

    override fun stop() {
        logger.info("PulumiPlugin.stop()")
    }
}

/**
 * By implementing SimpleStage, your stage is available for use in Spinnaker.
 * @see com.netflix.spinnaker.orca.api.SimpleStage
 */
@Extension
class PulumiStage(val configuration: PulumiConfig) : SimpleStage<PulumiInput> {

    private val log = LoggerFactory.getLogger(SimpleStage::class.java)

    /**
     * This sets the name of the stage
     * @return the name of the stage
     */
    override fun getName(): String {
        return "pulumi"
    }

    protected open fun getHttpClient() : OkHttpClient {
        return OkHttpClient()
    }

    /**
     * This is called when the stage is executed. It takes in an object that you create. That
     * object contains fields that you want to pull out of the pipeline context. This gives you a
     * strongly typed object that you have full control over.
     * The SimpleStageOutput class contains the status of the stage and any stage outputs that should be
     * put back into the pipeline context.
     * @param stageInput<RandomWaitInput>
     * @return SimpleStageOutput; the status of the stage and any context that should be passed to the pipeline context
     */
    override fun execute(stageInput: SimpleStageInput<PulumiInput>): SimpleStageOutput<Output, Context> {

        val stageOutput = SimpleStageOutput<Output, Context>()
        val output = Output()
        val context = Context()

        val credentials = when (stageInput.value.cloudProvider) {
            "aws" -> configuration.credentials
            else -> null
        }

        if (credentials == null){
            context.exception = SimpleStageException(SimpleStageExceptionDetails("", "AWS Credentials not provided.", listOf("Please add 'AWS_ACCESS_KEY_ID' and 'AWS_SECRET_ACCESS_KEY' under pulumi.credentials property")))
            stageOutput.status = SimpleStageStatus.TERMINAL
            stageOutput.context = context
            return stageOutput
        }

        storePulumiCredentials(configuration.accounts)

        val workspace = createWorkspace()
        val githubPath = URI(stageInput.value.githubRepository).path
        val repositoryInfo = stageInput.value.githubRepository.substring(stageInput.value.githubRepository.lastIndexOf(githubPath) +1).split("/")

        val downloaded = downloadGithubRepository(repositoryInfo.joinToString(separator = "/"), stageInput.value.githubBranch, workspace)

        if (!downloaded){
            context.exception = SimpleStageException(SimpleStageExceptionDetails("", "Github repositoy not found.", emptyList()))
            stageOutput.status = SimpleStageStatus.TERMINAL
            stageOutput.context = context
            return stageOutput
        }

        val buildCli = PulumiCli()
        buildCli.setCredentials(getCredentials(credentials))
        buildCli.setPath(workspace + "/" + repositoryInfo[1] + "-" + stageInput.value.githubBranch)
        buildCli.build("Typescript")

        val cliStack = PulumiCli()
        cliStack.setCredentials(getCredentials(credentials))
        cliStack.setPath(workspace + "/" + repositoryInfo[1] + "-" + stageInput.value.githubBranch)
        cliStack.selectStack(stageInput.value.pulumiStack)

        val cli = PulumiCli()
        cli.setCredentials(getCredentials(credentials))
        cli.setPath(workspace + "/" + repositoryInfo[1] + "-" + stageInput.value.githubBranch)
        val resultCli = cli.up()

        val response = if(resultCli.exitCode != 0)  {
            context.exception = SimpleStageException(SimpleStageExceptionDetails("", "Pulumi up fail.", listOf(resultCli.result)))
            SimpleStageStatus.TERMINAL
        } else {
            output.result = resultCli.result
            SimpleStageStatus.SUCCEEDED
        }

        stageOutput.output = output
        stageOutput.context = context
        stageOutput.status = response

        return stageOutput
    }

    private fun createWorkspace(): String {
        val home = System.getProperty("user.home")
        val workspaceId = System.currentTimeMillis().toString()
        val path = "$home/$workspaceId"

        File(path).mkdir()

        return path
    }

    private fun getCredentials(credentials: Credentials): Map<String, String?> {
        return mapOf("AWS_ACCESS_KEY_ID" to credentials.secretKeyId,"AWS_SECRET_ACCESS_KEY" to credentials.secretAccessKey, "AWS_SESSION_TOKEN" to System.getenv("AWS_SESSION_TOKEN") )
    }

    private fun downloadGithubRepository(repository: String, branch: String, workspace: String): Boolean {
        val repositoryUrl = String.format("https://codeload.github.com/%s/zip/%s", repository, branch)
        return try {
            val request = Request.Builder()
                    .url(repositoryUrl)
                    .get()
                    .build()

            getHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful){
                    return false
                }

                val zipFile = "$workspace/application.zip"
                val fos = FileOutputStream(zipFile)
                fos.write(response.body()!!.bytes())
                fos.close()
                unzip(zipFile, workspace)
            }
            return true
        }catch (e: Exception){
            log.error("Error downloading repository: $repositoryUrl", e)
            print(e.stackTrace)
            return false
        }
    }


    private fun unzip(zipFileName: String, destDir: String) {
        ProcessBuilder()
                .command("unzip", zipFileName, "-d", destDir)
                .start()
                .waitFor()
    }

    private fun storePulumiCredentials(account: Account) {

        val accessTokens = mapOf(account.serverUri to account.accessToken)
        val accounts = mapOf(account.serverUri to account)
        val pulumiCredentials = PulumiCredentials(account.serverUri, accessTokens, accounts)

        val home = System.getProperty("user.home")
        val pulumiPath = "/.pulumi/credentials.json"
        val file=File(home + pulumiPath)
        file.writeText(Gson().toJson(pulumiCredentials))
    }
}
