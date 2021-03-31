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

import static io.leangen.geantyref.GenericTypeReflector.erase;
import static java.util.Objects.requireNonNull;

import com.google.auto.value.AutoValue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A collection of tags that are understood when reading a document.
 *
 * @since 4.1.0
 */
final class TagRepository {

    private final Tag unresolvedTag;
    private final Tag stringTag;
    private final Tag sequenceTag;
    private final Tag mappingTag;
    private final List<OldTag> tags;
    private final Map<Class<?>, OldTag> byErasedType;
    private final Map<String, OldTag> byName;

    /**
     * Create a new tag repository.
     *
     * @param unresolved a tag to assign to nodes whose value could not
     *     be resolved
     * @param tags known tags
     * @return new tag repository
     * @since 4.1.0
     */
    public static TagRepository of(final OldTag unresolved, final List<OldTag> tags) {
        return new TagRepository(requireNonNull(unresolved, "unresolved"), UnmodifiableCollections.copyOf(tags));
    }

    /**
     * Create a new tag repository.
     *
     * @param unresolved a tag to assign to nodes whose value could not
     *     be resolved
     * @param tags known tags
     * @return a new tag repository
     * @since 4.1.0
     */
    public static TagRepository of(final OldTag unresolved, final OldTag... tags) {
        return new TagRepository(requireNonNull(unresolved, "unresolved"), UnmodifiableCollections.toList(tags));
    }

    TagRepository(final OldTag unresolved, final List<OldTag> tags) {
        this.unresolvedTag = unresolved;
        this.tags = tags;
        this.byErasedType = UnmodifiableCollections.buildMap(map -> {
            for (final OldTag tag : this.tags) {
                map.put(erase(tag.nativeType()), tag);
            }
        });
        this.byName = UnmodifiableCollections.buildMap(map -> {
            for (final OldTag tag : this.tags) {
                map.put(tag.uri().toString(), tag);
            }
        });
    }

    /**
     * Determine the implicit tag for a scalar value.
     *
     * @param scalar scalar to test
     * @return the first matching tag
     * @since 4.1.0
     */
    public @Nullable OldTag forInput(final String scalar) {
        for (final OldTag tag : this.tags) {
            if (tag.targetPattern().matcher(scalar).matches()) {
                return tag;
            }
        }

        return null;
    }

    /**
     * Resolve a tag by its URI.
     *
     * @param name the tag URI
     * @return a tag, if any is present
     * @since 4.1.0
     */
    public @Nullable OldTag named(final String name) {
        return this.byName.get(name);
    }

    /**
     * Resolve a tag by the Java type it represents.
     *
     * @param type the type used
     * @return a tag, if any is registered
     * @since 4.1.0
     */
    public @Nullable OldTag byType(final Class<?> type) {
        return this.byErasedType.get(type);
    }

    /**
     * Analyze a node to determine what tag its value should have.
     *
     * @param node the node to analyze
     * @return a calculated tag
     * @since 4.1.0
     */
    public AnalyzedTag analyze(final ConfigurationNode node) {
        final @Nullable OldTag explicit = node.hint(YamlConfigurationLoader.TAG);
        final @Nullable OldTag calculated;
        if (node.isMap()) {
            calculated = this.byType(Map.class);
        } else if (node.isList()) {
            calculated = this.byType(List.class);
        } else if (node.isNull()) {
            calculated = this.byType(void.class);
        } else {
            calculated = this.byType(node.rawScalar().getClass());
        }
        return AnalyzedTag.of(explicit == null ? this.unresolvedTag : explicit, calculated);
    }


    /**
     * A combination of resolved tag, and whether the tag is the same as the tag
     * that would be implicitly calculated.
     *
     * @since 4.1.0
     */
    @AutoValue
    public abstract static class AnalyzedTag {

        /**
         * Create a new resolved tag.
         *
         * @param resolved the resolved type
         * @param specified the specified type
         * @return the resolved tag
         * @since 4.1.0
         */
        static AnalyzedTag of(final OldTag resolved, final @Nullable OldTag specified) {
            return new AutoValue_TagRepository_AnalyzedTag(resolved, specified);
        }

        AnalyzedTag() {
        }

        /**
         * Get the calculated tag, if any is present.
         *
         * <p>If no tag could be resolved, this will always return the parser's
         * <em>unresolved</em> tag.</p>
         *
         * @return the calculated tag
         * @since 4.1.0
         */
        public abstract OldTag resolved();

        /**
         * Get the manually specified tag for this node.
         *
         * @return the specified tag
         * @since 4.1.0
         */
        public abstract @Nullable OldTag specified();

        /**
         * Get whether the provided tag is an implicit tag or not.
         *
         * <p>A tag is implicit when no type has been specified, or the resolved
         * type equals the specified type.</p>
         *
         * @return whether the tag is implicit.
         * @since 4.1.0
         */
        public final boolean implicit() {
            return this.specified() == null || Objects.equals(this.resolved(), this.specified());
        }

    }

}
