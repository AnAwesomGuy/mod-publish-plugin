package me.modmuss50.mpp.test.curseforge

import me.modmuss50.mpp.test.IntegrationTest
import me.modmuss50.mpp.test.MockWebServer
import org.gradle.testkit.runner.TaskOutcome
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class CurseforgeTest : IntegrationTest {
    @Test
    fun uploadCurseforge() {
        val server = MockWebServer(MockCurseforgeApi())

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                    modLoaders.add("fabric")
                
                    curseforge {
                        accessToken = "123"
                        projectId = "123456"
                        minecraftVersions.add("1.20.1")
                        
                        requires {
                            slug = "fabric-api"
                        }
                        
                        apiEndpoint = "${server.endpoint}"
                    }
                }
                """.trimIndent(),
            )
            .run("publishCurseforge")
        server.close()

        val metadata = server.api.lastMetadata!!

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforge")!!.outcome)
        assertEquals(metadata.changelog, "Hello!")
        assertContains(metadata.gameVersions, 9990) // 1.20.1
        assertContains(metadata.gameVersions, 7499) // Fabric
        assertEquals(metadata.relations.projects[0].slug, "fabric-api")
    }

    @Test
    fun uploadCurseforgeWithOptions() {
        val server = MockWebServer(MockCurseforgeApi())

        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                
                    // Common options that can be re-used between diffrent curseforge tasks
                    val curseforgeOptions = curseforgeOptions {
                        accessToken = "123"
                        minecraftVersions.add("1.20.1")
                        apiEndpoint = "${server.endpoint}"
                    }
                
                    curseforge("curseforgeFabric") {
                        from(curseforgeOptions)
                        file = tasks.jar.flatMap { it.archiveFile }
                        projectId = "123456"
                        modLoaders.add("fabric")
                        requires {
                            slug = "fabric-api"
                        }
                    }
                    
                    curseforge("curseforgeForge") {
                        from(curseforgeOptions)
                        file = tasks.jar.flatMap { it.archiveFile }
                        projectId = "789123"
                        modLoaders.add("forge")
                    }
                }
                """.trimIndent(),
            )
            .run("publishMods")
        server.close()

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforgeFabric")!!.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforgeForge")!!.outcome)
    }

    @Test
    fun dryRunCurseforge() {
        val result = gradleTest()
            .buildScript(
                """
                publishMods {
                    file = tasks.jar.flatMap { it.archiveFile }
                    changelog = "Hello!"
                    version = "1.0.0"
                    type = BETA
                    modLoaders.add("fabric")
                    
                    dryRun = true
                
                    curseforge {
                        accessToken = providers.environmentVariable("TEST_TOKEN_THAT_DOES_NOT_EXISTS")
                        projectId = "123456"
                        minecraftVersions.add("1.20.1")
                    }
                }
                """.trimIndent(),
            )
            .run("publishCurseforge")

        assertEquals(TaskOutcome.SUCCESS, result.task(":publishCurseforge")!!.outcome)
    }
}