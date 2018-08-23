package com.atlassian.performance.tools.infrastructure.jira

import com.atlassian.performance.tools.infrastructure.DockerImage
import com.atlassian.performance.tools.infrastructure.jira.JiraHomeSource
import com.atlassian.performance.tools.ssh.SshConnection
import java.time.Duration

internal data class DockerJiraHome(
    private val image: DockerImage
) : JiraHomeSource {

    private val localPath: String = "jira-home"

    override fun download(
        ssh: SshConnection
    ): String {
        val containerName = image.run(ssh)
        ssh.execute("docker cp $containerName:/var/lib/jira-home $localPath", Duration.ofMinutes(4))
        return localPath
    }
}