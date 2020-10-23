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

import io.leangen.geantyref.GenericTypeReflector;
import net.kyori.coffee.function.Consumer1E;
import net.kyori.coffee.function.Predicate1;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.util.Types;

import java.lang.reflect.Array;
import java.lang.reflect.Type;

/**
 * A serializer for array classes. Primitive arrays need special handling
 * because they don't have autoboxing like single primitives do.
 *
 * @param <T> array type
 */
abstract class ArraySerializer<T> extends AbstractListChildSerializer<T> {

    @Override
    Type elementType(final Type containerType) throws SerializationException {
        final Type componentType = GenericTypeReflector.getArrayComponentType(containerType);
        if (componentType == null) {
            throw new SerializationException(containerType, "Must be array type");
        }
        return componentType;
    }

    static class Objects extends ArraySerializer<Object[]> {

        public static Predicate1<Type> predicate() {
            return token -> {
                if (!Types.isArray(token)) {
                    return false;
                }

                final Type componentType = GenericTypeReflector.getArrayComponentType(token);
                return componentType.equals(GenericTypeReflector.box(componentType));
            };
        }

        @Override
        Object[] createNew(final int length, final Type elementType) {
            return (Object[]) Array.newInstance(GenericTypeReflector.erase(elementType), length);
        }

        @Override
        void forEachElement(final Object[] collection, final Consumer1E<Object, SerializationException> action) throws SerializationException {
            for (Object o : collection) {
                action.accept(o);
            }
        }

        @Override
        void deserializeSingle(final int index, final Object[] collection, final @Nullable Object deserialized) {
            collection[index] = deserialized;
        }

    }

    static class Booleans extends ArraySerializer<boolean[]> {

        static final Class<boolean[]> TYPE = boolean[].class;

        @Override
        boolean[] createNew(final int length, final Type elementType) {
            return new boolean[length];
        }

        @Override
        void forEachElement(final boolean[] collection, final Consumer1E<Object, SerializationException> action) throws SerializationException {
            for (boolean b : collection) {
                action.accept(b);
            }
        }

        @Override
        void deserializeSingle(final int index, final boolean[] collection, final @Nullable Object deserialized) throws SerializationException {
            collection[index] = deserialized == null ? false : Scalars.BOOLEAN.deserialize(deserialized);
        }

    }

    static class Bytes extends ArraySerializer<byte[]> {

        static final Class<byte[]> TYPE = byte[].class;

        @Override
        byte[] createNew(final int length, final Type elementType) {
            return new byte[length];
        }

        @Override
        void forEachElement(final byte[] collection, final Consumer1E<Object, SerializationException> action) throws SerializationException {
            for (byte b : collection) {
                action.accept(b);
            }
        }

        @Override
        void deserializeSingle(final int index, final byte[] collection, final @Nullable Object deserialized) throws SerializationException {
            collection[index] = deserialized == null ? 0 : Scalars.INTEGER.deserialize(deserialized).byteValue();
        }

    }

    static class Chars extends ArraySerializer<char[]> {

        static final Class<char[]> TYPE = char[].class;

        @Override
        char[] createNew(final int length, final Type elementType) {
            return new char[length];
        }

        @Override
        void forEachElement(final char[] collection, final Consumer1E<Object, SerializationException> action) throws SerializationException {
            for (char b : collection) {
                action.accept(b);
            }
        }

        @Override
        void deserializeSingle(final int index, final char[] collection, final @Nullable Object deserialized) throws SerializationException {
            collection[index] = deserialized == null ? 0 : Scalars.CHAR.deserialize(deserialized);
        }

    }

    static class Shorts extends ArraySerializer<short[]> {

        static final Class<short[]> TYPE = short[].class;

        @Override
        short[] createNew(final int length, final Type elementType) {
            return new short[length];
        }

        @Override
        void forEachElement(final short[] collection, final Consumer1E<Object, SerializationException> action) throws SerializationException {
            for (short b : collection) {
                action.accept(b);
            }
        }

        @Override
        void deserializeSingle(final int index, final short[] collection, final @Nullable Object deserialized) throws SerializationException {
            collection[index] = deserialized == null ? 0 : Scalars.INTEGER.deserialize(deserialized).shortValue();
        }

    }

    static class Ints extends ArraySerializer<int[]> {

        static final Class<int[]> TYPE = int[].class;

        @Override
        int[] createNew(final int length, final Type elementType) {
            return new int[length];
        }

        @Override
        void forEachElement(final int[] collection, final Consumer1E<Object, SerializationException> action) throws SerializationException {
            for (int b : collection) {
                action.accept(b);
            }
        }

        @Override
        void deserializeSingle(final int index, final int[] collection, final @Nullable Object deserialized) throws SerializationException {
            collection[index] = deserialized == null ? 0 : Scalars.INTEGER.deserialize(deserialized);
        }

    }

    static class Longs extends ArraySerializer<long[]> {

        static final Class<long[]> TYPE = long[].class;

        @Override
        long[] createNew(final int length, final Type elementType) {
            return new long[length];
        }

        @Override
        void forEachElement(final long[] collection, final Consumer1E<Object, SerializationException> action) throws SerializationException {
            for (long b : collection) {
                action.accept(b);
            }
        }

        @Override
        void deserializeSingle(final int index, final long[] collection, final @Nullable Object deserialized) throws SerializationException {
            collection[index] = deserialized == null ? 0 : Scalars.LONG.deserialize(deserialized);
        }

    }

    static class Floats extends ArraySerializer<float[]> {

        static final Class<float[]> TYPE = float[].class;

        @Override
        float[] createNew(final int length, final Type elementType) {
            return new float[length];
        }

        @Override
        void forEachElement(final float[] collection, final Consumer1E<Object, SerializationException> action) throws SerializationException {
            for (float b : collection) {
                action.accept(b);
            }
        }

        @Override
        void deserializeSingle(final int index, final float[] collection, final @Nullable Object deserialized) throws SerializationException {
            collection[index] = deserialized == null ? 0 : Scalars.FLOAT.deserialize(deserialized);
        }

    }

    static class Doubles extends ArraySerializer<double[]> {

        static final Class<double[]> TYPE = double[].class;

        @Override
        double[] createNew(final int length, final Type elementType) {
            return new double[length];
        }

        @Override
        void forEachElement(final double[] collection, final Consumer1E<Object, SerializationException> action) throws SerializationException {
            for (double b : collection) {
                action.accept(b);
            }
        }

        @Override
        void deserializeSingle(final int index, final double[] collection, final @Nullable Object deserialized) throws SerializationException {
            collection[index] = deserialized == null ? 0 : Scalars.DOUBLE.deserialize(deserialized);
        }

    }

}
