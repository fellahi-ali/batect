/*
   Copyright 2017 Charles Korn.

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

package batect

import batect.config.Container
import batect.docker.DockerContainer
import batect.docker.DockerImage
import batect.docker.DockerNetwork
import batect.model.steps.BuildImageStep
import batect.model.steps.CleanUpContainerStep
import batect.model.steps.CreateContainerStep
import batect.model.steps.CreateTaskNetworkStep
import batect.model.steps.DisplayTaskFailureStep
import batect.model.steps.RemoveContainerStep
import batect.model.steps.RunContainerStep
import batect.model.steps.StartContainerStep
import batect.testutils.CreateForEachTest
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object EventLoggerSpec : Spek({
    describe("an event logger") {
        val whiteConsole by CreateForEachTest(this) { mock<Console>() }
        val console by CreateForEachTest(this) {
            mock<Console> {
                on { withColor(eq(ConsoleColor.White), any()) } doAnswer {
                    val printStatements = it.getArgument<Console.() -> Unit>(1)
                    printStatements(whiteConsole)
                }
            }
        }

        val redErrorConsole by CreateForEachTest(this) { mock<Console>() }
        val errorConsole by CreateForEachTest(this) {
            mock<Console> {
                on { withColor(eq(ConsoleColor.Red), any()) } doAnswer {
                    val printStatements = it.getArgument<Console.() -> Unit>(1)
                    printStatements(redErrorConsole)
                }
            }
        }

        val logger by CreateForEachTest(this) { EventLogger(console, errorConsole) }
        val container = Container("the-cool-container", "/build/dir/doesnt/matter")

        describe("handling when steps start") {
            on("when a 'build image' step is starting") {
                val step = BuildImageStep("doesnt-matter", container)
                logger.logBeforeStartingStep(step)

                it("prints a message to the output") {
                    inOrder(whiteConsole) {
                        verify(whiteConsole).print("Building ")
                        verify(whiteConsole).printBold("the-cool-container")
                        verify(whiteConsole).println("...")
                    }
                }
            }

            on("when a 'start container' step is starting") {
                val step = StartContainerStep(container, DockerContainer("not-important", "not-important"))
                logger.logBeforeStartingStep(step)

                it("prints a message to the output") {
                    inOrder(whiteConsole) {
                        verify(whiteConsole).print("Starting dependency ")
                        verify(whiteConsole).printBold("the-cool-container")
                        verify(whiteConsole).println("...")
                    }
                }
            }

            describe("when a 'run container' step is starting") {
                on("and no 'create container' step has been seen") {
                    val step = RunContainerStep(container, DockerContainer("not-important", "not-important"))
                    logger.logBeforeStartingStep(step)

                    it("prints a message to the output without mentioning a command") {
                        inOrder(whiteConsole) {
                            verify(whiteConsole).print("Running ")
                            verify(whiteConsole).printBold("the-cool-container")
                            verify(whiteConsole).println("...")
                        }
                    }
                }

                describe("and a 'create container' step has been seen") {
                    on("and that step did not contain a command") {
                        val createContainerStep = CreateContainerStep(container, null, DockerImage("some-image"), DockerNetwork("some-network"))
                        val runContainerStep = RunContainerStep(container, DockerContainer("not-important", "not-important"))

                        logger.logBeforeStartingStep(createContainerStep)
                        logger.logBeforeStartingStep(runContainerStep)

                        it("prints a message to the output without mentioning a command") {
                            inOrder(whiteConsole) {
                                verify(whiteConsole).print("Running ")
                                verify(whiteConsole).printBold("the-cool-container")
                                verify(whiteConsole).println("...")
                            }
                        }
                    }

                    on("and that step contained a command") {
                        val createContainerStep = CreateContainerStep(container, "do-stuff.sh", DockerImage("some-image"), DockerNetwork("some-network"))
                        val runContainerStep = RunContainerStep(container, DockerContainer("not-important", "not-important"))

                        logger.logBeforeStartingStep(createContainerStep)
                        logger.logBeforeStartingStep(runContainerStep)

                        it("prints a message to the output including the command") {
                            inOrder(whiteConsole) {
                                verify(whiteConsole).print("Running ")
                                verify(whiteConsole).printBold("do-stuff.sh")
                                verify(whiteConsole).print(" in ")
                                verify(whiteConsole).printBold("the-cool-container")
                                verify(whiteConsole).println("...")
                            }
                        }
                    }

                    on("but the logger has been reset since that event") {
                        val createContainerStep = CreateContainerStep(container, "do-stuff.sh", DockerImage("some-image"), DockerNetwork("some-network"))
                        val runContainerStep = RunContainerStep(container, DockerContainer("not-important", "not-important"))

                        logger.logBeforeStartingStep(createContainerStep)
                        logger.reset()
                        logger.logBeforeStartingStep(runContainerStep)

                        it("prints a message to the output without mentioning a command") {
                            inOrder(whiteConsole) {
                                verify(whiteConsole).print("Running ")
                                verify(whiteConsole).printBold("the-cool-container")
                                verify(whiteConsole).println("...")
                            }
                        }
                    }
                }
            }

            on("when a 'display task failure' step is starting") {
                val step = DisplayTaskFailureStep("Something went wrong.")
                logger.logBeforeStartingStep(step)

                it("prints the message to the output") {
                    inOrder(redErrorConsole) {
                        verify(redErrorConsole).println()
                        verify(redErrorConsole).println(step.message)
                    }
                }
            }

            mapOf(
                    "remove container" to RemoveContainerStep(container, DockerContainer("some-id", "some-name")),
                    "clean up container" to CleanUpContainerStep(container, DockerContainer("some-id", "some-name"))
            ).forEach { description, step ->
                describe("when a '$description' step is starting") {
                    on("and no 'remove container' or 'clean up container' steps have run before") {
                        logger.logBeforeStartingStep(step)

                        it("prints a message to the output") {
                            verify(whiteConsole).println("Cleaning up...")
                        }
                    }

                    on("and a 'remove container' step has already been run") {
                        val previousStep = RemoveContainerStep(Container("other-container", "/other-build-dir"), DockerContainer("some-other-id", "some-other-name"))
                        logger.logBeforeStartingStep(previousStep)

                        logger.logBeforeStartingStep(step)

                        it("only prints one message to the output") {
                            verify(whiteConsole, times(1)).println("Cleaning up...")
                        }
                    }

                    on("and a 'clean up container' step has already been run") {
                        val previousStep = CleanUpContainerStep(Container("other-container", "/other-build-dir"), DockerContainer("some-other-id", "some-other-name"))
                        logger.logBeforeStartingStep(previousStep)

                        logger.logBeforeStartingStep(step)

                        it("only prints one message to the output") {
                            verify(whiteConsole, times(1)).println("Cleaning up...")
                        }
                    }
                }
            }

            on("when another kind of step is starting") {
                val step = CreateTaskNetworkStep
                logger.logBeforeStartingStep(step)

                it("does not print anything to the output") {
                    verifyZeroInteractions(console)
                }
            }
        }

        on("when the task fails") {
            logger.logTaskFailed("some-task")

            it("prints a message to the output") {
                inOrder(redErrorConsole) {
                    verify(redErrorConsole).println()
                    verify(redErrorConsole).print("The task ")
                    verify(redErrorConsole).printBold("some-task")
                    verify(redErrorConsole).println(" failed. See above for details.")
                }
            }
        }

        on("when the task does not exist") {
            logger.logTaskDoesNotExist("some-task")

            it("prints a message to the output") {
                inOrder(redErrorConsole) {
                    verify(redErrorConsole).print("The task ")
                    verify(redErrorConsole).printBold("some-task")
                    verify(redErrorConsole).println(" does not exist.")
                }
            }
        }
    }
})
