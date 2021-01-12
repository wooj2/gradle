/*
 * Copyright 2021 the original author or authors.
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

package org.junit

import org.apache.commons.io.output.TeeOutputStream
import org.gradle.integtests.tooling.fixture.ClassLoaderFixture
import org.gradle.integtests.tooling.fixture.ToolingApiDistributionResolver
import org.gradle.integtests.tooling.fixture.ToolingApiSpecification
import org.gradle.integtests.tooling.fixture.TargetGradleVersion
import org.gradle.integtests.tooling.fixture.ToolingApiVersion
import org.gradle.internal.classloader.ClasspathUtil
import org.gradle.internal.classloader.DefaultClassLoaderFactory
import org.gradle.internal.classloader.FilteringClassLoader
import org.gradle.internal.classloader.MultiParentClassLoader
import org.gradle.internal.classloader.VisitableURLClassLoader
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.RedirectStdOutAndErr
import org.gradle.util.Requires
import org.gradle.util.SetSystemProperties
import org.gradle.util.TestPrecondition

class ToolingApiTestClassClassLoaderProvider {

    static ClassLoader loadClass(String className) {
        String toolingApiToLoad = System.getProperty("org.gradle.integtest.tooling-api-to-load")
        return createTestClassLoader(className, toolingApiToLoad)
    }

    private static ClassLoader createTestClassLoader(String target, String toolingApiToLoad) {
        ClassLoader applicationClassloader = Thread.currentThread().getContextClassLoader()

        def toolingApi = new ToolingApiDistributionResolver().withDefaultRepository().resolve(toolingApiToLoad)

        List<File> testClassPath = []
        testClassPath << ClasspathUtil.getClasspathForClass(target)
        testClassPath << ClasspathUtil.getClasspathForClass(ToolingApiSpecification)
        //TODO testClassPath.addAll(collectAdditionalClasspath()) ???

        def classLoaderFactory = new DefaultClassLoaderFactory()

        def sharedSpec = new FilteringClassLoader.Spec()
        sharedSpec.allowPackage('org.junit')
        sharedSpec.allowPackage('org.hamcrest')
        sharedSpec.allowPackage('junit.framework')
        sharedSpec.allowPackage('groovy')
        sharedSpec.allowPackage('org.codehaus.groovy')
        sharedSpec.allowPackage('spock')
        sharedSpec.allowPackage('org.spockframework')
        sharedSpec.allowClass(SetSystemProperties)
        sharedSpec.allowClass(RedirectStdOutAndErr)
        sharedSpec.allowPackage('org.gradle.integtests.fixtures')
        sharedSpec.allowPackage('org.gradle.play.integtest.fixtures')
        sharedSpec.allowPackage('org.gradle.plugins.ide.fixtures')
        sharedSpec.allowPackage('org.gradle.test.fixtures')
        sharedSpec.allowPackage('org.gradle.nativeplatform.fixtures')
        sharedSpec.allowPackage('org.gradle.language.fixtures')
        sharedSpec.allowPackage('org.gradle.workers.fixtures')
        sharedSpec.allowPackage('org.gradle.launcher.daemon.testing')
        sharedSpec.allowClass(OperatingSystem)
        sharedSpec.allowClass(Requires)
        sharedSpec.allowClass(TestPrecondition)
        sharedSpec.allowClass(TargetGradleVersion)
        sharedSpec.allowClass(ToolingApiVersion)
        sharedSpec.allowClass(TeeOutputStream)
        sharedSpec.allowClass(ClassLoaderFixture)

        sharedSpec.allowResources(target.replace('.', '/'))

        def sharedClassLoader = classLoaderFactory.createFilteringClassLoader(applicationClassloader, sharedSpec)

        def parentClassLoader = new MultiParentClassLoader(toolingApi.classLoader, sharedClassLoader)

        return new VisitableURLClassLoader("test", parentClassLoader, testClassPath.collect { it.toURI().toURL() })
    }
}
