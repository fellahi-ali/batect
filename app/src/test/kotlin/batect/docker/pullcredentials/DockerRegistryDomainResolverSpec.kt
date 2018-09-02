/*
   Copyright 2017-2018 Charles Korn.

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

package batect.docker.pullcredentials

import batect.testutils.equalTo
import com.natpryce.hamkrest.assertion.assertThat
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it

object DockerRegistryDomainResolverSpec : Spek({
    describe("a Docker registry domain resolver") {
        val resolver = DockerRegistryDomainResolver()

        given("an empty image name") {
            it("returns the default registry domain name") {
                assertThat(resolver.resolveDomainForImage(""), equalTo("docker.io"))
            }
        }

        given("an image name without a registry or repository name") {
            it("returns the default registry domain name") {
                assertThat(resolver.resolveDomainForImage("ubuntu"), equalTo("docker.io"))
            }
        }

        given("an image name with a repository but no registry name") {
            it("returns the default registry domain name") {
                assertThat(resolver.resolveDomainForImage("library/ubuntu"), equalTo("docker.io"))
            }
        }

        given("an image name with a repository and the default registry name") {
            it("returns the default registry domain name") {
                assertThat(resolver.resolveDomainForImage("docker.io/library/ubuntu"), equalTo("docker.io"))
            }
        }

        given("an image name with a repository and the legacy default registry name") {
            it("returns the default registry domain name") {
                assertThat(resolver.resolveDomainForImage("index.docker.io/library/ubuntu"), equalTo("docker.io"))
            }
        }

        given("an image name with a repository and a non-default registry name") {
            it("returns that registry domain name") {
                assertThat(resolver.resolveDomainForImage("some-docker-registry.com/library/ubuntu"), equalTo("some-docker-registry.com"))
            }
        }

        given("an image name with a nested repository and a non-default registry name") {
            it("returns that registry domain name") {
                assertThat(resolver.resolveDomainForImage("some-docker-registry.com/library/linux/ubuntu"), equalTo("some-docker-registry.com"))
            }
        }
    }
})