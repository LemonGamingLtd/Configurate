/*
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.spongepowered.configurate.yaml.emitter;

import org.yaml.snakeyaml.DumperOptions;

import java.io.Writer;

/**
 * A writer for YAML files.
 *
 * @since 4.1.0
 */
final class YamlEmitter {

    private final DumperOptions options;
    private final Writer writer;

    YamlEmitter(final DumperOptions options, final Writer writer) {
        this.options = options;
        this.writer = writer;
    }

    // stream header

    // directives

    // document start

    // document end

    // stream end/close

    // scalar

    // begin mapping

    // end mapping

    // begin block mapping

    // end block mapping

    // begin flow mapping

    // end flow mapping

    // begin sequence

    // end sequence

    // begin block sequence

    // end block sequence

    // begin flow sequence

    // end flow sequence

    // determine scalar type

}
