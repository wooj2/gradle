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

package org.gradle.testing.junitplatform


import org.gradle.integtests.fixtures.DefaultTestExecutionResult

class TestClassClassLoaderOverrideIntegrationTest extends JUnitPlatformIntegrationSpec {

    def "can inject custom classloader to load test classes"() {
        given:
        file("src/test/java/org/gradle/CustomClassLoader.java") << """
            package org.gradle;

            import java.io.File;
            import java.net.MalformedURLException;
            import java.net.URL;
            import java.net.URLClassLoader;

            public class CustomClassLoader extends URLClassLoader {
                public CustomClassLoader(ClassLoader parent) throws MalformedURLException {
                    super("test", new URL[]{new File("${testDirectory.absolutePath}/build/classes/java/test/").toURI().toURL()}, parent);
                }
                @Override
                public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    if (!name.equals("org.gradle.SampleTest")) {
                        return super.loadClass(name, resolve);
                    }
                    Class<?> loadedClass = findLoadedClass(name);
                    if (loadedClass != null) {
                        return loadedClass;
                    }
                    try {
                        return findClass(name);
                    } catch (ClassNotFoundException e) {
                        return super.loadClass(name, resolve);
                    }
                }
            }
        """
        file("src/test/java/org/gradle/SampleTest.java") << """
            package org.gradle;
            import static org.junit.jupiter.api.Assertions.*;

            import org.junit.jupiter.api.*;

            class SampleTest {
                @Test
                void testClassLoader() {
                    assertEquals("test", getClass().getClassLoader().getName());
                    assertTrue(getClass().getClassLoader() instanceof CustomClassLoader);
                }
            }
        """
        buildFile << """
            test {
                systemProperties "org.gradle.api.internal.tasks.testing.junitplatform.testClassClassLoader": "org.gradle.CustomClassLoader"
            }
        """

        when:
        succeeds("test")

        then:
        def testResult = new DefaultTestExecutionResult(testDirectory)
        testResult.assertTestClassesExecuted("org.gradle.SampleTest")
        testResult.testClass("org.gradle.SampleTest").assertTestCount(1, 0, 0).assertTestPassed("testClassLoader()")
    }
}
