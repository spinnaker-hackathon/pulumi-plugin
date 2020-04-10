package io.armory.plugin.stage.pulumi.command

import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.TimeUnit

class PulumiCli {

    private val logger = LoggerFactory.getLogger(PulumiCli::class.java)
    private var credentials = emptyMap<String, String?>()
    private val commandTimeout = 30L
    private var path = "/home/spinnaker"

    fun up(): CommandResponse {
        return exec(listOf("pulumi","up","--yes"))
    }

    fun build(language: String): CommandResponse {
        return when(language){
            "Typescript" -> exec(listOf("npm", "install"))
            else -> CommandResponse(null, null)
        }
    }

    fun setCredentials(credentials: Map<String, String?>){
        this.credentials = credentials
    }

    fun setPath(path: String){
        this.path = path
    }

    private fun exec(command: List<String>): CommandResponse {
        val process = ProcessBuilder(command)
        process.environment().putAll(credentials)
        process.directory(File(path))
        val resultProcess = process.start()
        val result: StringBuilder = StringBuilder()
        resultProcess.inputStream.reader(Charsets.UTF_8).use {
            val content = it.readText()
            print(content)
            result.append(content)
        }
        resultProcess.waitFor(commandTimeout, TimeUnit.SECONDS)

        return CommandResponse(resultProcess.exitValue(), result.toString())
    }

}