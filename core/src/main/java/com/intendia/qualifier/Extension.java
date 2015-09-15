// Copyright 2015 Intendia, SL.
package com.intendia.qualifier;

@SuppressWarnings("unchecked")
public interface Extension<T> {
    String getKey();

    /** Always false at run time. */
    default boolean isAnonymous() { return false; }

    default <V extends T> Extension<V> as() { return (Extension<V>) this; }

    static <T> Extension<T> anonymous() { return new AnonymousExtension<>(); }

    static <T> Extension<T> key(String key) { return new NamedExtension<>(key); }
}

class NamedExtension<T> implements Extension<T> {
    private final String key;

    NamedExtension(String key) { this.key = key.intern(); }

    @Override public String getKey() { return key; }

    @Override public String toString() { return key + "@" + Integer.toHexString(hashCode()); }
}

class AnonymousExtension<T> implements Extension<T> {
    @Override public String getKey() { throw new UnsupportedOperationException("processing time extension"); }

    @Override public boolean isAnonymous() { return true; }
}
