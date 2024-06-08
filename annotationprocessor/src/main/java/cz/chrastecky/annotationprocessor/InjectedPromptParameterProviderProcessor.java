package cz.chrastecky.annotationprocessor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
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

@SupportedAnnotationTypes("cz.chrastecky.annotationprocessor.InjectedPromptParameterProvider")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class InjectedPromptParameterProviderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(InjectedPromptParameterProvider.class);

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getProviders")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class),
                        ClassName.get("cz.chrastecky.aiwallpaperchanger.prompt_parameter_provider", "PromptParameterProvider")))
                .addStatement("List<PromptParameterProvider> result = new $T<>()", ArrayList.class);

        for (Element element : elements) {
            String className = element.asType().toString();
            methodBuilder.addStatement("result.add(new $L())", className);
        }

        methodBuilder.addStatement("return result");

        TypeSpec generatedClass = TypeSpec.classBuilder("PromptParameterProviders")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(methodBuilder.build())
                .build();

        JavaFile javaFile = JavaFile.builder("cz.chrastecky.aiwallpaperchanger", generatedClass).build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}