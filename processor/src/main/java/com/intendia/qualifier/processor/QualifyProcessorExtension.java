// Copyright 2014 Intendia, SL.
package com.intendia.qualifier.processor;

import static com.google.common.collect.FluentIterable.from;
import static com.intendia.qualifier.Qualifier.CORE_GENERICS;
import static com.intendia.qualifier.Qualifier.CORE_NAME;
import static com.intendia.qualifier.Qualifier.CORE_TYPE;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.auto.common.MoreElements;
import com.intendia.qualifier.annotation.Qualify;
import com.intendia.qualifier.annotation.QualifyExtension;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

public class QualifyProcessorExtension extends AbstractQualifierProcessorExtension {
    public static final WildcardTypeName WILDCARD = WildcardTypeName.subtypeOf(TypeName.OBJECT);
    public static final ClassName LANG_CLASS = ClassName.get(Class.class);

    public QualifyProcessorExtension() { registerAnnotation(Qualify.class, this::processQualify); }

    private void processQualify(AnnotationContext<Qualify> ctx) {
        for (QualifyExtension qualifyExtension : ctx.annotation().extend()) {
            addQualifyExtension(ctx.metadata(), ctx.annotatedElement(), ctx.annotationMirror(), qualifyExtension);
        }
    }

    public void addQualifyExtension(QualifierMetadata context, Element annotatedElement,
            AnnotationMirror annotationMirror, QualifyExtension qualifyExtension) {
        final QualifierMetadata.Entry<?> entry = context.use(qualifyExtension);

        final TypeMirror type = entry.type().get();
        final TypeElement element = MoreElements.asType(types().asElement(type));

        // Do not requires validation
        if (type.toString().equals("java.lang.String")) return;
        if (type.toString().equals("java.lang.Class")) return;

        // Check extensions types has a valid valueOf(String) static method
        final boolean valid = from(getExecutableElements(element)).anyMatch(method ->
                method.getSimpleName().toString().equals("valueOf")
                        && method.getModifiers().contains(Modifier.STATIC)
                        && method.getParameters().size() == 1
                        && isFirstParameterStringType(method));
        if (!valid) {
            getProcessingEnv().getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(
                            "Qualifier extension type '%s' with key '%s' must have a 'valueOf(String)' static " +
                                    "method", entry.type(), entry.extension()),
                    annotatedElement, annotationMirror);
        }

    }

    public boolean isFirstParameterStringType(ExecutableElement input) {
        return input.getParameters().get(0).asType().toString().equals("java.lang.String");
    }

    private Collection<ExecutableElement> getExecutableElements(TypeElement classRepresenter) {
        return ElementFilter.methodsIn(getProcessingEnv().getElementUtils().getAllMembers(classRepresenter));
    }

    @Override public void processProperty(TypeSpec.Builder writer, PropertyDescriptor descriptor) {
        // Property name
        writer.addMethod(methodBuilder("getName")
                .addModifiers(PUBLIC)
                .returns(String.class)
                .addStatement("return $S", descriptor.getName())
                .build());
        descriptor.metadata().put(CORE_NAME).valueBlock("getName()");

        // Property type
        writer.addMethod(MethodSpec.methodBuilder("getType")
                .addModifiers(PUBLIC)
                .returns(ParameterizedTypeName.get(LANG_CLASS, TypeName.get(descriptor.getPropertyType())))
                .addStatement("return $L$T.class",
                        descriptor.getPropertyType().getTypeArguments().isEmpty() ? "" : "(Class) ",
                        TypeName.get(types().erasure(descriptor.getPropertyType())))
                .build());
        descriptor.metadata().put(CORE_TYPE).valueBlock("getType()");

        // Property generics
        final List<? extends TypeMirror> typeArguments = descriptor.getPropertyType().getTypeArguments();
        if (!typeArguments.isEmpty()) writer.addMethod(MethodSpec.methodBuilder("getGenerics")
                .addModifiers(PUBLIC)
                .returns(ArrayTypeName.of(ParameterizedTypeName.get(LANG_CLASS, WILDCARD)))
                .addStatement("return new Class<?>[]{$L}", typeArguments.stream()
                        .map(t -> t.getKind() == TypeKind.WILDCARD ? "null" : t + ".class")
                        .collect(joining(",")))
                .build());
        descriptor.metadata().put(CORE_GENERICS).valueBlock("getGenerics()");
    }
}
