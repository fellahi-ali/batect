/*
   Copyright 2017-2019 Charles Korn.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package batect.journeytests

import batect.journeytests.testutils.ApplicationRunner
import batect.journeytests.testutils.itCleansUpAllContainersItCreates
import batect.journeytests.testutils.itCleansUpAllNetworksItCreates
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object SimpleTaskJourneyTests : Spek({
    mapOf(
        "simple-task-using-dockerfile" to "a simple task that uses a Dockerfile with the command specified on the task in the configuration file",
        "simple-task-using-image" to "a simple task that uses an existing image",
        "simple-task-dockerfile-command" to "a simple task with the command specified in the Dockerfile",
        "simple-task-container-command" to "a simple task with the command specified on the container in the configuration file",
        "simple-task-with-environment" to "a simple task with a task-level environment variable",
        "container-with-health-check-overrides" to "a task with a dependency container that has a batect-specific health check configuration"
    ).forEach { testName, description ->
        given(description) {
            val runner = ApplicationRunner(testName)

            on("running that task") {
                val result = runner.runApplication(listOf("the-task"))

                it("prints the output from that task") {
                    assertThat(result.output, containsSubstring("This is some output from the task\r\n"))
                }

                it("returns the exit code from that task") {
                    assertThat(result.exitCode, equalTo(123))
                }

                itCleansUpAllContainersItCreates(result)
                itCleansUpAllNetworksItCreates(result)
            }
        }
    }
})
