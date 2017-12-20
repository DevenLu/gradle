/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.resolve.http

import org.gradle.integtests.fixtures.GradleMetadataResolveRunner
import org.gradle.integtests.fixtures.RequiredFeature
import org.gradle.integtests.fixtures.RequiredFeatures
import org.gradle.integtests.resolve.AbstractModuleDependencyResolveTest
import org.gradle.test.fixtures.HttpRepository

class MetadataSourcesResolveIntegrationTest extends AbstractModuleDependencyResolveTest {

    @RequiredFeatures([
        @RequiredFeature(feature = GradleMetadataResolveRunner.GRADLE_METADATA, value = "true")
    ])
    def "can resolve with only gradle metadata"() {
        buildFile << """
            repositories.all {
                metadataSources {
                    gradleMetadata()
                }
            }
            dependencies {
                conf 'org.test:projectA:1.+'
            }
        """

        when:
        repository {
            'org.test:projectA' {
                '1.1'()
                '1.2'()
            }
        }
        repositoryInteractions {
            'org.test:projectA' {
                expectVersionListing()

                '1.2' {
                    expectResolve()
                }
            }
        }

        then:
        succeeds ":checkDeps"
        resolve.expectGraph {
            root(":", ":test:") {
                edge("org.test:projectA:1.+", "org.test:projectA:1.2")
            }
        }
    }

    def "can resolve with only repository-specific metadata"() {
        def metadataSource = useIvy() ? "ivyDescriptor" : "mavenPom"
        buildFile << """
            repositories.all {
                metadataSources {
                    ${metadataSource}()
                }
            }
            dependencies {
                conf 'org.test:projectA:1.+'
            }
        """

        when:
        repository {
            'org.test:projectA' {
                '1.1'()
                '1.2'()
            }
        }
        repositoryInteractions(HttpRepository.MetadataType.ONLY_ORIGINAL) {
            'org.test:projectA' {
                expectVersionListing()

                '1.2' {
                    expectResolve()
                }
            }
        }

        then:
        succeeds ":checkDeps"

        // We are resolving with "legacy" metadata: always gives default configuration
        resolve.expectDefaultConfiguration("default")
        resolve.expectGraph {
            root(":", ":test:") {
                edge("org.test:projectA:1.+", "org.test:projectA:1.2")
            }
        }
    }

    def "can resolve with only artifact metadata"() {
        buildFile << """
            repositories.all {
                metadataSources {
                    artifact()
                }
            }
            dependencies {
                conf 'org.test:projectA:1.+'
            }
        """

        when:
        repository {
            'org.test:projectA' {
                '1.1'()
                '1.2'()
            }
        }
        repositoryInteractions {
            'org.test:projectA' {
                expectVersionListing()

                '1.2' {
                    expectHeadArtifact()
                    expectGetArtifact()
                }
            }
        }

        then:
        succeeds ":checkDeps"

        // We are resolving with `artifact()` metadata: always gives default configuration
        resolve.expectDefaultConfiguration("default")
        resolve.expectGraph {
            root(":", ":test:") {
                edge("org.test:projectA:1.+", "org.test:projectA:1.2")
            }
        }
    }

    def "will only search for defined metadata sources"() {
        def metadataSource = isGradleMetadataEnabled() ? "gradleMetadata" : useIvy() ? "ivyDescriptor" : "mavenPom"
        def metadataType = isGradleMetadataEnabled() ? HttpRepository.MetadataType.ONLY_GRADLE : HttpRepository.MetadataType.ONLY_ORIGINAL
        def metadataUri = isGradleMetadataEnabled() ? metadataURI("org.test", "projectA", "1.1"): legacyMetadataURI("org.test", "projectA", "1.1")
        buildFile << """
            repositories.all {
                metadataSources {
                    ${metadataSource}()
                }
            }
            dependencies {
                conf 'org.test:projectA:1.1'
            }
        """

        when:
        repositoryInteractions(metadataType) {
            'org.test:projectA:1.1' {
                expectGetMetadataMissing()
            }
        }

        then:
        fails ":checkDeps"

        and:
        failure.assertHasCause("""Could not find org.test:projectA:1.1.
Searched in the following locations:
    ${metadataUri}
Required by:""")
    }
}
