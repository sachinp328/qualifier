// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import javax.annotation.Nullable;

@FunctionalInterface
public interface Qualifier<V> {
    Extension<String> CORE_NAME = Extension.key("core.name");
    Extension<Class<?>> CORE_TYPE = Extension.key("core.type");
    Extension<Class<?>[]> CORE_GENERICS = Extension.key("core.generics");
    Class<?>[] NO_GENERICS = new Class[0];

    default String getName() { return data(CORE_NAME); }

    default Class<V> getType() { return data(CORE_TYPE.as()); }

    default Class<?>[] getGenerics() { return data(CORE_GENERICS, NO_GENERICS); }

    /** Return the properties context of this qualifier. */
    Metadata getContext(); // { return Metadata.EMPTY; }

    @SuppressWarnings("unchecked")
    default @Nullable <T> T data(Extension<T> key) { return getContext().get(key); }

    @SuppressWarnings("unchecked")
    default <T> T data(Extension<T> key, T or) { return firstNonNull(getContext().get(key), or); }

    /** Return the property qualifiers of the bean qualifier. */
    default Collection<PropertyQualifier<V, ?>> getPropertyQualifiers() { return Collections.emptySet(); }

    /**
     * @deprecated created to easy fix self usages in TableBuilder columns, but! this is not required if Paths and
     * metadata are handled independently (path are the getter/setter methods of the property qualifier). If you don't
     * understand this comment, just understand that if you are using this method is because you are doing something
     * wrong! Another way of seen this situation is; why does you need to explain to some one (ex. TableBuilder) how
     * to obtain a property to itself, ie. how to do {@code x -> x}, obviously something is wrong!
     */
    @Deprecated
    default PropertyQualifier<V, V> asProperty() {
        final Qualifier<V> self = this;
        return new PropertyQualifier<V, V>() {
            @Override public Metadata getContext() { return self.getContext(); }

            @Override public @Nullable V get(V object) { return object; }

            @Override public Boolean isReadable() { return true; }

            @Override public Comparator<? super V> getComparator() { return ComparableQualifier.of(self).getOrdering(); }
        };
    }

    /** Traverse a qualifier returning a new qualifier which has source type this and value type property. */
//    default <ValueU> SimpleQualifier<V> as(PropertyQualifier<? super V, ValueU> property) {
//        return new PathQualifier<>(this, property);
//    }
}
