package com.intendia.qualifier.processor;

import java.util.Collection;
import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public interface Metamodel {
    String SELF = "self";

    String name();

    DeclaredType beanType();

    TypeElement beanElement();

    TypeMirror propertyType();

    @Nullable TypeElement propertyElement();

    @Nullable ExecutableElement getterElement();

    @Nullable ExecutableElement setterElement();

    @Nullable VariableElement fieldElement();

    Metaqualifier metadata();

    default Collection<Metaextension<?>> extensions() { return metadata().values(); }

    default boolean isProperty() { return !name().equals(SELF); }
}
