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
package org.spongepowered.configurate.yaml;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.loader.ParsingException;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

class Tag {
    private final URI tagUri;
    private final Set<Class<?>> supportedTypes;

    Tag(final URI tagUri, final Set<? extends Class<?>> supportedTypes) {
        this.tagUri = tagUri;
        this.supportedTypes = UnmodifiableCollections.copyOf(supportedTypes);
    }

    public final URI tagUri() {
        return this.tagUri;
    }

    public final Set<Class<?>> supportedTypes() {
        return this.supportedTypes;
    }

    static abstract class Scalar<V> extends Tag {
        private final @Nullable Pattern pattern;

        // for unregistered tags on scalars
        static Scalar<String> ofUnknown(final URI tagURi) {
            return new Scalar<String>(tagURi, Collections.emptySet(), null) {
                @Override
                public String fromString(final String input) {
                    return input;
                }

                @Override
                public String toString(final String own) {
                    return own;
                }
            };
        }

        Scalar(final URI tagUri, final Set<Class<? extends V>> supportedTypes, final @Nullable Pattern pattern) {
            super(tagUri, supportedTypes);
            this.pattern = pattern;
        }

        /**
         * Pattern to use to detect this tag.
         *
         * <p>May be {@code null} if this tag cannot be used as an implicit tag.</p>
         *
         * @return the detection pattern
         */
        public final @Nullable Pattern pattern() {
            return this.pattern;
        }

        public abstract V fromString(final String input) throws ParsingException;

        public abstract String toString(final V own) throws ConfigurateException;

    }

    static class Mapping extends Tag {

        Mapping(final URI tagUri, final Set<Class<?>> supportedTypes) {
            super(tagUri, supportedTypes);
        }

    }

    static class Sequence extends Tag {

        Sequence(final URI tagUri, final Set<Class<?>> supportedTypes) {
            super(tagUri, supportedTypes);
        }

    }

    @Override
    public boolean equals(final @Nullable Object that) {
        // todo: ensure type of tag is equal
        return that instanceof Tag
            && ((Tag) that).tagUri().equals(this.tagUri);
    }
}
