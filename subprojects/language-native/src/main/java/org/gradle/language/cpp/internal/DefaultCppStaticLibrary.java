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

package org.gradle.language.cpp.internal;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.component.SoftwareComponentInternal;
import org.gradle.api.internal.component.UsageContext;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.internal.provider.Providers;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.language.cpp.CppPlatform;
import org.gradle.language.cpp.CppStaticLibrary;
import org.gradle.language.nativeplatform.internal.ConfigurableComponentWithLinkUsage;
import org.gradle.language.nativeplatform.internal.ConfigurableComponentWithRuntimeUsage;
import org.gradle.language.nativeplatform.internal.ConfigurableComponentWithStaticLibrary;
import org.gradle.nativeplatform.Linkage;
import org.gradle.nativeplatform.tasks.CreateStaticLibrary;
import org.gradle.nativeplatform.toolchain.internal.NativeToolChainInternal;
import org.gradle.nativeplatform.toolchain.internal.PlatformToolProvider;
import org.testng.collections.Sets;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Set;

public class DefaultCppStaticLibrary extends DefaultCppBinary implements CppStaticLibrary, ConfigurableComponentWithStaticLibrary, ConfigurableComponentWithLinkUsage, ConfigurableComponentWithRuntimeUsage, SoftwareComponentInternal {
    private final RegularFileProperty linkFile;
    private final Property<CreateStaticLibrary> createTaskProperty;
    private final Property<Configuration> linkElements;
    private final Property<Configuration> runtimeElements;
    private final ConfigurableFileCollection outputs;
    public final NativeVariantIdentity identity;

    @Inject
    public DefaultCppStaticLibrary(String name, ProjectLayout projectLayout, ObjectFactory objectFactory, FileOperations fileOperations, Provider<String> baseName, FileCollection sourceFiles, FileCollection componentHeaderDirs, ConfigurationContainer configurations, Configuration implementation, CppPlatform targetPlatform, NativeToolChainInternal toolChain, PlatformToolProvider platformToolProvider, NativeVariantIdentity identity) {
        super(name, projectLayout, objectFactory, baseName, sourceFiles, componentHeaderDirs, configurations, implementation, targetPlatform, toolChain, platformToolProvider, identity);
        this.linkFile = projectLayout.fileProperty();
        this.createTaskProperty = objectFactory.property(CreateStaticLibrary.class);
        this.linkElements = objectFactory.property(Configuration.class);
        this.runtimeElements = objectFactory.property(Configuration.class);
        this.outputs = fileOperations.files();
        this.identity = identity;
    }

    @Override
    public ConfigurableFileCollection getOutputs() {
        return outputs;
    }

    @Override
    public RegularFileProperty getLinkFile() {
        return linkFile;
    }

    @Override
    public Property<CreateStaticLibrary> getCreateTask() {
        return createTaskProperty;
    }

    @Override
    public Property<Configuration> getLinkElements() {
        return linkElements;
    }

    @Override
    public Property<Configuration> getRuntimeElements() {
        return runtimeElements;
    }

    @Nullable
    @Override
    public Linkage getLinkage() {
        return Linkage.STATIC;
    }

    @Override
    public boolean hasRuntimeFile() {
        return false;
    }

    @Override
    public Provider<RegularFile> getRuntimeFile() {
        return Providers.notDefined();
    }

    @Override
    public Set<? extends UsageContext> getUsages() {
        Set<UsageContext> result = Sets.newHashSet();
        for (UsageContext usageContext : identity.getUsages()) {
            if (usageContext.getName().contains("-link")) {
                result.add(new DefaultUsageContext(usageContext.getName(), usageContext.getUsage(), linkElements.get().getAllArtifacts(), linkElements.get()));
            } else {
                result.add(new DefaultUsageContext(usageContext.getName(), usageContext.getUsage(), runtimeElements.get().getAllArtifacts(), runtimeElements.get()));
            }
        }
        return result;
    }
}
