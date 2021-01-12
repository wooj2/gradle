/*
 * Copyright 2016 the original author or authors.
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
package org.gradle.integtests.fixtures

import org.gradle.integtests.fixtures.executer.GradleDistribution
import org.gradle.integtests.fixtures.versions.ReleasedVersionDistributions
import org.gradle.util.GradleVersion
import spock.lang.Specification

/**
 * See {@link org.gradle.integtests.fixtures.AbstractContextualMultiVersionSpecRunner} for information on running these tests.
 */
class ToolingApiCompatibilitySuiteRunner extends AbstractCompatibilityTestRunner {
    private static final GradleVersion MINIMAL_VERSION = GradleVersion.version("2.6")

    ToolingApiCompatibilitySuiteRunner(Class<? extends Specification> target) {
        super(target)
    }

    /**
     * Tooling API tests will can run against any version back to Gradle 0.8.
     */
    @Override
    protected List<GradleDistribution> choosePreviousVersionsToTest(ReleasedVersionDistributions previousVersions) {
        return previousVersions.all
    }

    @Override
    protected void createExecutions() {
        String version = System.getProperty(VERSIONS_SYSPROP_NAME)
        if (version == "default") {
            // For Tooling API tests, the default is to test the current Tooling API against the current Gradle version
            add(createToolingApiExecution(current))
        } else {
            add(createToolingApiExecution(getAllVersions().find {it.distribution.version.version == version }.distribution))
        }
    }

    protected ToolingApiExecution createToolingApiExecution(GradleDistribution gradleDistribution) {
        def testClassloaderHasCurrentToolingApi = System.getProperty("org.gradle.integtest.tooling-api-to-load") != null
        if (testClassloaderHasCurrentToolingApi) {
            // Running version defined in 'org.gradle.integtest.versions' towards 'current'
            new ToolingApiExecution(gradleDistribution, current)
        } else {
            // Running 'current' towards version defined in 'org.gradle.integtest.versions'
            new ToolingApiExecution(current, gradleDistribution)
        }
    }

    @Override
    protected boolean isAvailable(GradleDistributionTool version) {
        return version.distribution.toolingApiSupported
    }
}
