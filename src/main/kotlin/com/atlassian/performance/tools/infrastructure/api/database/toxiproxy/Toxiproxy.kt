package com.atlassian.performance.tools.infrastructure.api.database.toxiproxy

import com.atlassian.performance.tools.infrastructure.DockerImage
import com.atlassian.performance.tools.infrastructure.api.os.Ubuntu
import com.atlassian.performance.tools.ssh.api.SshConnection
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Duration
import java.time.Instant

class Toxiproxy(
    private val standardDbPort: Int,
    private val alternativeDbPort: Int,
    explicitConfig: String? = null // e.g. """{"type":"latency", "attributes":{"latency":3}}&{"type":"timeout", "attributes":{"timeout":3000}}"""
) {
    private val logger: Logger = LogManager.getLogger(this::class.java)
    private val ubuntu = Ubuntu()
    private val config = explicitConfig?:System.getenv("JPT_TOXIPROXY")?:""
    private val enabled = config.isNotBlank()
    private val image: DockerImage = DockerImage(
        name = "shopify/toxiproxy:2.1.4",
        pullTimeout = Duration.ofMinutes(5)
    )
    private val port = 8474
    private val hostGateway = "172.17.0.1"

    fun dbPort() : Int {
        return if (enabled) alternativeDbPort else standardDbPort
    }

    fun setup(ssh: SshConnection) {
        if (enabled) {
            image.run(
                ssh = ssh,
                parameters ="-p $port:$port -p $standardDbPort:$standardDbPort",
                arguments = ""
            )
        }
    }

    fun start(ssh: SshConnection) {
        if (enabled) {
            waitForToxiproxy(ssh)
            ssh.execute("""curl -i -d '{"name": "toxic_$standardDbPort", "listen": ":$standardDbPort", "upstream": "$hostGateway:$alternativeDbPort"}' localhost:$port/proxies""")
            config.split("&").forEach {
                logger.info("applying toxic: $it")
                ssh.execute("curl -i -d '$it' localhost:$port/proxies/toxic_$standardDbPort/toxics")
            }
        }
    }

    private fun waitForToxiproxy(ssh: SshConnection) {
        ubuntu.install(ssh, listOf("curl"))
        val toxiproxyStart = Instant.now()
        while (!ssh.safeExecute("curl localhost:$port/proxies").isSuccessful()) {
            if (Instant.now() > toxiproxyStart + Duration.ofMinutes(3)) {
                throw RuntimeException("Toxiproxy didn't start in time")
            }
            logger.debug("Waiting for Toxiproxy...")
            Thread.sleep(Duration.ofSeconds(2).toMillis())
        }
    }
}
