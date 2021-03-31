package org.spongepowered.configurate.yaml;

import static java.util.Objects.requireNonNull;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ScalarEvent;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;

abstract class TagImpl implements Tag {

    @Override
    public URI uri() {
        return null;
    }

    @Override
    public Class<?> nativeType() {
        return null;
    }

    static final class ScalarImpl extends TagImpl implements Tag.Scalar {

        @Override
        public @Nullable Pattern matchPattern() {
            return null;
        }

        @Override
        public void compose(final ConfigurationNode target, final YamlParser source) {

        }

        @Override
        public void emit(final ConfigurationNode source, final YamlVisitor.State state) {

        }
    }

    static final class CollectionImpl extends TagImpl {

        @Override
        public void compose(final ConfigurationNode target, final YamlParser source) {

        }

        @Override
        public void emit(final ConfigurationNode source, final YamlVisitor.State state) {

        }

    }

    static final class BuilderImpl implements Tag.Builder, Tag.Builder.Scalar, Tag.Builder.Collection {
        private @Nullable URI uri;
        private final Set<Class<?>> nativeTypes = new HashSet<>();
        private @Nullable Pattern collectionPattern;
        private @Nullable BiConsumer<ConfigurationNode, YamlVisitor.State> representer;
        private @Nullable BiConsumer<ConfigurationNode, YamlParser> composer;

        // Common

        @Override
        public Builder local(final String id) {
            return this.uri(new URI("tag:" + id));
        }

        @Override
        public Builder uri(final URI uri) {
            requireNonNull(uri, "uri");
            if (!"tag".equals(uri.getScheme())) {
                throw new IllegalArgumentException("Tag URIs must have the 'tag' scheme!");
            }
            this.uri = uri;
            return this;
        }

        @Override
        public Builder nativeType(final Class<?> nativeType) {
            this.nativeTypes.add(nativeType);
            return this;
        }

        @Override
        public BuilderImpl compose(final BiConsumer<ConfigurationNode, YamlParser> composer) {
            return this;
        }

        @Override
        public TagImpl build() {
            return ;
        }

        private BuilderImpl checkStateTransition() {
            if (this.uri == null) {
                throw new IllegalStateException("No URI specified for tag");
            }

            if (this.nativeTypes.isEmpty()) {
                throw new IllegalStateException("No native type");
            }
            return this;
        }

        // Collection

        @Override
        public Collection collection() {
            return this.checkStateTransition();
        }

        @Override
        public Collection represent(final BiConsumer<ConfigurationNode, YamlVisitor.State> representer) {
            this.representer = requireNonNull(representer, "representer");
            return this;
        }

        // Scalar

        @Override
        public Scalar scalar(final Pattern matchPattern) {
            this.collectionPattern = requireNonNull(matchPattern, "matchPattern");
            return this.checkStateTransition();
        }

        @Override
        public Scalar represent(final Function<ConfigurationNode, String> emit) {
            this.representer = (node, state) -> {
                state.emit(new ScalarEvent());
            }
            return this;
        }

        @Override
        public Scalar from(final ScalarSerializer<?> serializer) {
            this.represent(node -> ((ScalarSerializer<Object>) serializer).serializeToString(node.rawScalar()));
            this.compose((node, parser) -> {
                node.raw(serializer.deserialize(parser.requireEvent(Event.ID.Scalar, ScalarEvent.class).getValue());
            });
            return this;
        }
    }

}
