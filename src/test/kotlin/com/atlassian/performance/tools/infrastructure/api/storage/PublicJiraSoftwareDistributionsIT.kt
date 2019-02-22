package com.atlassian.performance.tools.infrastructure.api.storage

import com.atlassian.performance.tools.infrastructure.docker.SshUbuntuContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PublicJiraSoftwareDistributionsIT {

    @Test
    fun shouldDownloadJiraSoftware() {
        SshUbuntuContainer().run { ssh ->
            @Suppress("DEPRECATION") val jiraDistribution: ProductDistribution = PublicJiraSoftwareDistributions().get("7.2.0")
            val targetFolder = "test"
            ssh.execute("mkdir $targetFolder")

            val remoteDirectory = jiraDistribution
                .install(ssh, targetFolder)

            val directories = ssh.execute("ls $remoteDirectory").output
            assertThat(directories).contains("atlassian-jira")
        }
    }
}