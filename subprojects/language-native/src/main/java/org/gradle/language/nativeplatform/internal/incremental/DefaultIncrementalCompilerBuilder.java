/*
 * Copyright 2013 the original author or authors.
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
package org.gradle.language.nativeplatform.internal.incremental;

import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.changedetection.state.FileSystemSnapshotter;
import org.gradle.cache.PersistentStateCache;
import org.gradle.language.base.internal.compile.Compiler;
import org.gradle.language.nativeplatform.internal.incremental.sourceparser.CSourceParser;
import org.gradle.nativeplatform.toolchain.NativeToolChain;
import org.gradle.nativeplatform.toolchain.internal.NativeCompileSpec;

public class DefaultIncrementalCompilerBuilder implements IncrementalCompilerBuilder {
    @Override
    public <T extends NativeCompileSpec> Compiler<T> createIncrementalCompiler(TaskInternal task, Compiler<T> compiler, IncrementalCompilation incrementalCompilation, PersistentStateCache<CompilationState> stateCache) {
        return new IncrementalNativeCompiler<T>(task, incrementalCompilation, stateCache, compiler);
    }
}
