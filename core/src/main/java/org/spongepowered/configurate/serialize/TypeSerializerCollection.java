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
package org.spongepowered.configurate.serialize;

import static io.leangen.geantyref.GenericTypeReflector.annotate;
import static io.leangen.geantyref.GenericTypeReflector.isSuperType;
import static java.util.Objects.requireNonNull;
import static org.spongepowered.configurate.util.Types.requireCompleteParameters;

import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import net.kyori.coffee.function.Predicate1;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A calculated collection of {@link TypeSerializer}s.
 */
public final class TypeSerializerCollection {

    private static final TypeSerializerCollection DEFAULTS;

    static {
        DEFAULTS = TypeSerializerCollection.builder()
                .registerExact(Scalars.STRING)
                .registerExact(Scalars.BOOLEAN)
                .register(MapSerializer.TYPE, new MapSerializer())
                .register(ListSerializer.TYPE, new ListSerializer())
                .registerExact(Scalars.BYTE)
                .registerExact(Scalars.SHORT)
                .registerExact(Scalars.INTEGER)
                .registerExact(Scalars.LONG)
                .registerExact(Scalars.FLOAT)
                .registerExact(Scalars.DOUBLE)
                .registerAnnotatedObjects(ObjectMapper.factory())
                .register(Scalars.ENUM)
                .registerExact(Scalars.CHAR)
                .registerExact(Scalars.URI)
                .registerExact(Scalars.URL)
                .registerExact(Scalars.UUID)
                .registerExact(Scalars.PATTERN)
                .register(ArraySerializer.Objects.predicate(), new ArraySerializer.Objects())
                .registerExact(ArraySerializer.Booleans.TYPE, new ArraySerializer.Booleans())
                .registerExact(ArraySerializer.Bytes.TYPE, new ArraySerializer.Bytes())
                .registerExact(ArraySerializer.Chars.TYPE, new ArraySerializer.Chars())
                .registerExact(ArraySerializer.Shorts.TYPE, new ArraySerializer.Shorts())
                .registerExact(ArraySerializer.Ints.TYPE, new ArraySerializer.Ints())
                .registerExact(ArraySerializer.Longs.TYPE, new ArraySerializer.Longs())
                .registerExact(ArraySerializer.Floats.TYPE, new ArraySerializer.Floats())
                .registerExact(ArraySerializer.Doubles.TYPE, new ArraySerializer.Doubles())
                .register(SetSerializer.TYPE, new SetSerializer())
                .register(ConfigurationNodeSerializer.TYPE, new ConfigurationNodeSerializer())
                .register(PathSerializer.TYPE, PathSerializer.INSTANCE)
                .registerExact(FileSerializer.TYPE, FileSerializer.INSTANCE)
                .build();
    }

    private final @Nullable TypeSerializerCollection parent;
    private final List<RegisteredSerializer> serializers;
    private final Map<Type, TypeSerializer<?>> typeMatches = new ConcurrentHashMap<>();

    private TypeSerializerCollection(final @Nullable TypeSerializerCollection parent, final List<RegisteredSerializer> serializers) {
        this.parent = parent;
        this.serializers = UnmodifiableCollections.copyOf(serializers);
    }

    /**
     * Resolve a type serializer.
     *
     * <p>First, all registered serializers from this collection are queried
     * then if a parent collection is set, that collection is queried.
     *
     * @param token the type a serializer is required for
     * @param <T> the type to serialize
     * @return a serializer if any is present, or null if no applicable
     *          serializer is found
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable TypeSerializer<T> get(final TypeToken<T> token) {
        requireNonNull(token, "type");
        return (TypeSerializer<T>) get(token.getType());
    }

    /**
     * Resolve a type serializer.
     *
     * <p>First, all registered serializers from this collection are queried
     * then if a parent collection is set, that collection is queried.
     *
     * <p>This method will fail when provided a raw parameterized type</p>
     *
     * @param token the type a serializer is required for
     * @param <T> the type to serialize
     * @return a serializer if any is present, or null if no applicable
     *          serializer is found
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable TypeSerializer<T> get(final Class<T> token) {
        requireNonNull(token, "type");
        requireCompleteParameters(token);

        return (TypeSerializer<T>) get((Type) token);
    }

    /**
     * Resolve a type serializer.
     *
     * <p>First, all registered serializers from this collection are queried
     * then if a parent collection is set, that collection is queried.
     *
     * @param type the type a serializer is required for
     * @return a serializer if any is present, or null if no applicable
     *          serializer is found
     */
    public @Nullable TypeSerializer<?> get(Type type) {
        type = GenericTypeReflector.toCanonicalBoxed(annotate(requireNonNull(type, "type"))).getType();
        @Nullable TypeSerializer<?> serial = this.typeMatches.computeIfAbsent(type, param -> {
            for (RegisteredSerializer ent : this.serializers) {
                if (ent.predicate.test(param)) {
                    return ent.serializer;
                }
            }
            return null;
        });

        if (serial == null && this.parent != null) {
            serial = this.parent.get(type);
        }
        return serial;
    }

    /**
     * Create a new builder to begin building a collection of type serializers
     * that inherits from this collection.
     *
     * @return the new builder
     */
    public Builder childBuilder() {
        return new Builder(this);
    }

    @Override
    public String toString() {
        return "TypeSerializerCollection{"
                + "parent=" + this.parent
                + ", serializers=" + this.serializers
                + '}';
    }

    @Override public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TypeSerializerCollection)) {
            return false;
        }
        final TypeSerializerCollection that = (TypeSerializerCollection) other;
        return Objects.equals(this.parent, that.parent)
                && this.serializers.equals(that.serializers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.parent, this.serializers);
    }

    /**
     * Create a builder for a new type serializer collection without a
     * parent set.
     *
     * <p>If <em>any</em> of the standard serializers provided by Configurate
     * are desired, either the default collection or a collection inheriting
     * from the default collection should be applied.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder(null);
    }

    /**
     * Get a collection containing all of Configurate's built-in
     * type serializers.
     *
     * @return the collection
     */
    public static TypeSerializerCollection defaults() {
        return DEFAULTS;
    }

    /**
     * A builder to construct new serializer collections.
     */
    public static class Builder {
        private final @Nullable TypeSerializerCollection parent;
        private final List<RegisteredSerializer> serializers = new ArrayList<>();

        Builder(final @Nullable TypeSerializerCollection parent) {
            this.parent = parent;
        }

        /**
         * Register a type serializer for a given type.
         *
         * <p>Serializers registered will match all subclasses of the provided
         * type, as well as unwrapped primitive equivalents of the type.
         *
         * @param type the type to accept
         * @param serializer the serializer that will be serialized with
         * @param <T> the type to generify around
         * @return this builder
         */
        public <T> Builder register(final TypeToken<T> type, final TypeSerializer<? super T> serializer) {
            return register0(type.getType(), serializer);
        }

        /**
         * Register a type serializer for a given type.
         *
         * <p>Serializers registered will match all subclasses of the provided
         * type, as well as unboxed primitive equivalents of the type.
         *
         * @param type the type to accept
         * @param serializer the serializer that will be serialized with
         * @param <T> the type to generify around
         * @return this builder
         */
        public <T> Builder register(final Class<T> type, final TypeSerializer<? super T> serializer) {
            return register0(type, serializer);
        }

        /**
         * Register a type serializer matching against a given predicate.
         *
         * @param test the predicate to match types against
         * @param serializer the serializer to serialize matching types with
         * @param <T> the type parameter
         * @return this builder
         */
        public <T> Builder register(final Predicate1<Type> test, final TypeSerializer<? super T> serializer) {
            requireNonNull(test, "test");
            requireNonNull(serializer, "serializer");
            this.serializers.add(new RegisteredSerializer(test, serializer));
            return this;
        }

        /**
         * Register a scalar serializer with its own attached type token.
         *
         * <p>Serializers registered will match all subclasses of the provided
         * type, as well as unboxed primitive equivalents of the type.</p>
         *
         * @param serializer serializer to register
         * @param <T> value type
         * @return this builder
         */
        public <T> Builder register(final ScalarSerializer<T> serializer) {
            requireNonNull(serializer, "serializer");
            return register(serializer.type(), serializer);
        }

        private Builder register0(final Type type, final TypeSerializer<?> serializer) {
            requireNonNull(type, "type");
            requireNonNull(serializer, "serializer");
            this.serializers.add(new RegisteredSerializer(test -> {
                // Test direct type
                if (GenericTypeReflector.isSuperType(type, test)) {
                    return true;
                }

                // And upper bounds
                if (test instanceof WildcardType) {
                    final Type[] upperBounds = ((WildcardType) test).getUpperBounds();
                    if (upperBounds.length == 1) {
                        return isSuperType(type, upperBounds[0]);
                    }
                }
                return false;
            }, serializer));
            return this;
        }

        /**
         * Register an <em>exact</em> type serializer for a given type.
         *
         * <p>Serializers will only match exact object types. For example, a
         * serializer registered for {@code List<String>} would not match when
         * {@code ArrayList<String>} is queried.</p>
         *
         * @param type the type to accept
         * @param serializer the serializer that will be serialized with
         * @param <T> the type to generify around
         * @return this builder
         */
        public <T> Builder registerExact(final TypeToken<T> type, final TypeSerializer<? super T> serializer) {
            return registerExact0(type.getType(), serializer);
        }

        /**
         * Register an <em>exact</em> type serializer for a given type.
         *
         * <p>Serializers will only match exact object types. For example, a
         * serializer registered for {@code List<String>} would not match when
         * {@code ArrayList<String>} is queried.</p>
         *
         * @param type the type to accept
         * @param serializer the serializer that will be serialized with
         * @param <T> the type to generify around
         * @return this builder
         */
        public <T> Builder registerExact(final Class<T> type, final TypeSerializer<? super T> serializer) {
            return registerExact0(type, serializer);
        }

        /**
         * Register a scalar serializer with its own attached type token.
         *
         * <p>Serializers will only match exact object types. For example, a
         * serializer registered for {@code List<String>} would not match when
         * {@code ArrayList<String>} is queried.</p>
         *
         * @param serializer serializer to register
         * @param <T> value type
         * @return this builder
         */
        public <T> Builder registerExact(final ScalarSerializer<T> serializer) {
            requireNonNull(serializer, "serializer");
            return registerExact(serializer.type(), serializer);
        }

        private Builder registerExact0(final Type type, final TypeSerializer<?> serializer) {
            requireNonNull(type, "type");
            requireNonNull(serializer, "serializer");
            this.serializers.add(new RegisteredSerializer(test -> test.equals(type), serializer));
            return this;
        }

        /**
         * Register all serializers from {@code other} into this collection.
         *
         * <p>Creating a child collection should be preferred, but when merging
         * multiple sets of serializers together, directly adding other
         * collections may be the best choice.</p>
         *
         * @param other source collection
         * @return this builder
         */
        public Builder registerAll(final TypeSerializerCollection other) {
            this.serializers.addAll(requireNonNull(other, "other").serializers);
            return this;
        }

        /**
         * Register a customized object mapper to handle
         * {@link org.spongepowered.configurate.objectmapping.ConfigSerializable}-annotated objects.
         *
         * @param factory factory to retrieve object mappers from
         * @return this builder
         */
        public Builder registerAnnotatedObjects(final ObjectMapper.Factory factory) {
            return register(ObjectMapper.annotatedSerializerPredicate(), factory.asTypeSerializer());
        }

        /**
         * Create a new type serializer collection.
         *
         * @return a newly created collection
         */
        public TypeSerializerCollection build() {
            return new TypeSerializerCollection(this.parent, this.serializers);
        }
    }

    private static final class RegisteredSerializer {

        private final Predicate1<Type> predicate;
        private final TypeSerializer<?> serializer;

        private RegisteredSerializer(final Predicate1<Type> predicate, final TypeSerializer<?> serializer) {
            this.predicate = predicate;
            this.serializer = serializer;
        }

    }

}
