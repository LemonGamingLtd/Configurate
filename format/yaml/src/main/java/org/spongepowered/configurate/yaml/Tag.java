package org.spongepowered.configurate.yaml;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.ScalarSerializer;

import java.net.URI;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A YAML tag encoding data type information
 */
interface Tag {

    static Builder builder() {
        return new TagImpl.BuilderImpl();
    }

    /**
     * The URI referring to this tag.
     *
     * <p>The returned URI will be in the `tag` schema.</p>
     *
     * @return the tag URI
     * @since 4.1.0
     */
    URI uri();

    /**
     * Get the Java type this tag will deserialize to.
     *
     * @return this tag's native type
     * @since 4.1.0
     */
    Class<?> nativeType();

    /**
     * Read from the parser into {@code target}
     * @param target node to write into
     * @param source source TODO: add extra context for
     */
    void compose(final ConfigurationNode target, final YamlParser source);

    void emit(final ConfigurationNode source, final YamlVisitor.State state);

    interface Scalar {

        /**
         * A pattern that indicates this tag can be used as an
         * <em>implicit tag</em>. This allows it to be resolved from plain
         * scalars without any tag specified.
         *
         * @return the pattern matching this tag
         */
        @Nullable Pattern matchPattern();
    }

    /**
     * A builder for tags.
     *
     * @since 4.1.0
     */
    interface Builder {

        /**
         * Apply a local URL (starting with {@code !} to this tag.
         *
         * @param id the tag id
         * @return this builder
         * @since 4.1.0
         */
        Builder local(final String id);

        /**
         * The {@link URI} uniquely pointing to this tag
         * @param uri
         * @return
         */
        Builder uri(final URI uri);

        /**
         * The native type this tag deserializes to
         * @param nativeType
         * @return
         */
        Builder nativeType(final Class<?> nativeType);

        Scalar scalar(final Pattern matchPattern);

        Collection collection();

        interface Scalar {
            Scalar compose(final BiConsumer<ConfigurationNode, YamlParser> composer);

            Scalar represent(final Function<ConfigurationNode, String> emit);

            Scalar from(final ScalarSerializer<?> serializer);

            Tag build();
        }

        interface Collection {
            Collection compose(final BiConsumer<ConfigurationNode, YamlParser> composer);
            Collection represent(final BiConsumer<ConfigurationNode, YamlVisitor.State> emitter);

            Tag build();
        }

    }

}
