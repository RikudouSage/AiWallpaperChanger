package cz.chrastecky.annotationprocessor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("cz.chrastecky.annotationprocessor.LlmTextFormatter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class LlmTextFormatterProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (annotations.isEmpty()) {
            return false;
        }

        ClassName textFormatterInterface = ClassName.get("cz.chrastecky.aiwallpaperchanger.text_formatter", "TextFormatter");
        ClassName nonNull = ClassName.get("androidx.annotation", "NonNull");
        ClassName nullable = ClassName.get("androidx.annotation", "Nullable");
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(LlmTextFormatter.class);

        MethodSpec.Builder getFormattersMethod = MethodSpec.methodBuilder("getFormatters")
                .addModifiers(Modifier.PRIVATE)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), textFormatterInterface))
                .addStatement("final List<$T> result = new $T<>()", textFormatterInterface, ArrayList.class)
                .addAnnotation(nonNull)
        ;

        for (Element element : elements) {
            getFormattersMethod.addStatement("result.add(new $T())", element.asType());
        }
        getFormattersMethod.addStatement("return result");

        MethodSpec.Builder findForModelMethod = MethodSpec.methodBuilder("findForModel")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(nullable)
                .returns(textFormatterInterface)
                .addParameter(ParameterSpec.builder(String.class, "model", Modifier.FINAL).addAnnotation(nonNull).build())
                .beginControlFlow("for (final $T formatter : getFormatters())", textFormatterInterface)
                .beginControlFlow("if (formatter.supports(model))")
                .addStatement("return formatter")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return null")
        ;

        TypeSpec generatedClass = TypeSpec.classBuilder("LlmTextFormatterProvider")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(getFormattersMethod.build())
                .addMethod(findForModelMethod.build())
                .build();

        JavaFile javaFile = JavaFile.builder("cz.chrastecky.aiwallpaperchanger.text_formatter", generatedClass).build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
