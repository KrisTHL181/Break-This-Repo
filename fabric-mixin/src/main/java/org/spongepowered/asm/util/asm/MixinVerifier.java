/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.asm.util.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import java.util.List;

/**
 * Verifier which handles class info lookups via {@link ClassInfo}
 */
public class MixinVerifier extends SimpleVerifier {
    private static final Type OBJECT_TYPE = Type.getType(Object.class);

    public MixinVerifier(int api, Type currentClass, Type currentSuperClass, List<Type> currentClassInterfaces, boolean isInterface) {
        super(api, currentClass, currentSuperClass, currentClassInterfaces, isInterface);
    }

    @Override
    protected boolean isInterface(Type type) {
        if (type.getSort() != Type.OBJECT) {
            return false;
        }
        return ClassInfo.forType(type, ClassInfo.TypeLookup.DECLARED_TYPE).isInterface();
    }

    @Override
    protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
        Type expectedType = expected.getType();
        Type type = value.getType();
        switch (expectedType.getSort()) {
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                return type.equals(expectedType);
            case Type.ARRAY:
            case Type.OBJECT:
                if (type.equals(NULL_TYPE)) {
                    return true;
                } else if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
                    if (isAssignableFrom(expectedType, type)) {
                        return true;
                    }
                    if (expectedType.getSort() == Type.ARRAY) {
                        if (type.getSort() != Type.ARRAY) {
                            return false;
                        }
                        int dim = expectedType.getDimensions();
                        expectedType = expectedType.getElementType();
                        if (dim > type.getDimensions() || expectedType.getSort() != Type.OBJECT) {
                            return false;
                        }
                        type = Type.getType(type.getDescriptor().substring(dim));
                    }
                    if (isInterface(expectedType)) {
                        // The merge of class or interface types can only yield class types (because it is not
                        // possible in general to find an unambiguous common super interface, due to multiple
                        // inheritance). Because of this limitation, we need to relax the subtyping check here
                        // if 'value' is an interface.
                        return type.getSort() >= Type.ARRAY;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            default:
                throw new AssertionError();
        }
    }

    @Override
    protected boolean isAssignableFrom(Type type1, Type type2) {
        return type1.equals(getCommonSupertype(type1, type2));
    }

    @Override
    public BasicValue merge(BasicValue value1, BasicValue value2) {
        if (value1.equals(value2)) {
            return value1;
        }
        if (value1.equals(BasicValue.UNINITIALIZED_VALUE) || value2.equals(BasicValue.UNINITIALIZED_VALUE)) {
            return BasicValue.UNINITIALIZED_VALUE;
        }
        Type supertype = getCommonSupertype(value1.getType(), value2.getType());
        return newValue(supertype);
    }

    private static Type getCommonSupertype(Type type1, Type type2) {
        if (type1.equals(type2) || type2.equals(NULL_TYPE)) {
            return type1;
        }
        if (type1.equals(NULL_TYPE)) {
            return type2;
        }
        if (type1.getSort() < Type.ARRAY || type2.getSort() < Type.ARRAY) {
            // We know they're not the same, so they must be incompatible.
            return null;
        }
        if (type1.getSort() == Type.ARRAY && type2.getSort() == Type.ARRAY) {
            int dim1 = type1.getDimensions();
            Type elem1 = type1.getElementType();
            int dim2 = type2.getDimensions();
            Type elem2 = type2.getElementType();
            if (dim1 == dim2) {
                Type commonSupertype;
                if (elem1.equals(elem2)) {
                    commonSupertype = elem1;
                } else if (elem1.getSort() == Type.OBJECT && elem2.getSort() == Type.OBJECT) {
                    commonSupertype = getCommonSupertype(elem1, elem2);
                } else {
                    return arrayType(OBJECT_TYPE, dim1 - 1);
                }
                return arrayType(commonSupertype, dim1);
            }
            Type smaller;
            int shared;
            if (dim1 < dim2) {
                smaller = elem1;
                shared = dim1 - 1;
            } else {
                smaller = elem2;
                shared = dim2 - 1;
            }
            if (smaller.getSort() == Type.OBJECT) {
                shared++;
            }
            return arrayType(OBJECT_TYPE, shared);
        }
        if (type1.getSort() == Type.ARRAY && type2.getSort() == Type.OBJECT || type2.getSort() == Type.ARRAY && type1.getSort() == Type.OBJECT) {
            return OBJECT_TYPE;
        }
        return ClassInfo.getCommonSuperClass(type1, type2).getType();
    }

    private static Type arrayType(final Type type, final int dimensions) {
        if (dimensions == 0) {
            return type;
        } else {
            StringBuilder descriptor = new StringBuilder();
            for (int i = 0; i < dimensions; ++i) {
                descriptor.append('[');
            }
            descriptor.append(type.getDescriptor());
            return Type.getType(descriptor.toString());
        }
    }

    @Override
    protected Class<?> getClass(Type type) {
        throw new UnsupportedOperationException(
                String.format(
                        "Live-loading of %s attempted by MixinVerifier! This should never happen!",
                        type.getClassName()
                )
        );
    }
}
