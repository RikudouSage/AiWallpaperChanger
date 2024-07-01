package cz.chrastecky.annotationprocessor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
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

@SupportedAnnotationTypes("cz.chrastecky.annotationprocessor.InjectedWallpaperAction")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class InjectedWallpaperActionProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        ClassName wallpaperActionClassName = ClassName.get("cz.chrastecky.aiwallpaperchanger.action", "WallpaperAction");
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(InjectedWallpaperAction.class);

        MethodSpec.Builder getActionsMethod = MethodSpec.methodBuilder("getActions")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), wallpaperActionClassName))
                .addStatement("List<WallpaperAction> result = new $T<>()", ArrayList.class);

        for (Element element : elements) {
            String className = element.asType().toString();
            getActionsMethod.addStatement("result.add(new $L())", className);
        }

        getActionsMethod.addStatement("return result");

        MethodSpec.Builder findByIdMethod = MethodSpec.methodBuilder("findById")
                .addModifiers(Modifier.PUBLIC)
                .returns(wallpaperActionClassName)
                .addAnnotation(ClassName.get("androidx.annotation", "NonNull"))

                .addParameter(TypeName.get(String.class), "id", Modifier.FINAL)

                .beginControlFlow("for ($T action : getActions())", wallpaperActionClassName)

                .beginControlFlow("if (action.getId().equals(id))")
                .addStatement("return action")
                .endControlFlow()

                .endControlFlow()
                .addStatement("throw new $T(\"Unsupported action: \" + id)", RuntimeException.class)
        ;

        TypeSpec generatedClass = TypeSpec.classBuilder("WallpaperActionCollection")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(getActionsMethod.build())
                .addMethod(findByIdMethod.build())
                .build();

        JavaFile javaFile = JavaFile.builder("cz.chrastecky.aiwallpaperchanger.action", generatedClass).build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
