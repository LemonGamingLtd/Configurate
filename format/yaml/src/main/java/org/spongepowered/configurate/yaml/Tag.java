package org.spongepowered.configurate.yaml;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.net.URI;
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
        private final Pattern pattern;

        Scalar(final URI tagUri, final Set<Class<? extends V>> supportedTypes, final Pattern pattern) {
            super(tagUri, supportedTypes);
            this.pattern = pattern;
        }

        public final Pattern pattern() {
            return this.pattern;
        }

        public abstract V fromString(final String input);

        public abstract String toString(final V own);

    }

    static abstract class Mapping extends Tag {

        Mapping(final URI tagUri, final Set<Class<?>> supportedTypes) {
            super(tagUri, supportedTypes);
        }

        public abstract ConfigurationNode keyNode(final ConfigurationNode parent);

        public abstract ConfigurationNode valueNode(final ConfigurationNode parent, final ConfigurationNode keyNode);

    }

    static abstract class Sequence extends Tag {

        Sequence(final URI tagUri, final Set<Class<?>> supportedTypes) {
            super(tagUri, supportedTypes);
        }

        public ConfigurationNode provideNext(final ConfigurationNode parent) {
            return parent.appendListNode();
        }
    }

}
