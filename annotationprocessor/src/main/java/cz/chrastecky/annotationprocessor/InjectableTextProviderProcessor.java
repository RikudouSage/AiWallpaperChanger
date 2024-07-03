package cz.chrastecky.annotationprocessor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
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

@SupportedAnnotationTypes("cz.chrastecky.annotationprocessor.InjectableTextProvider")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class InjectableTextProviderProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (annotations.isEmpty()) {
            return false;
        }

        final String targetNamespace = "cz.chrastecky.aiwallpaperchanger.provider";
        final String targetClassName = "AiTextProviderCollection";

        final ClassName textProviderInterface = ClassName.get("cz.chrastecky.aiwallpaperchanger.provider", "AiTextProvider");
        final ClassName nonNull = ClassName.get("androidx.annotation", "NonNull");
        final ClassName nullable = ClassName.get("androidx.annotation", "Nullable");
        final ClassName context = ClassName.get("android.content", "Context");
        final ClassName defaultProvider = ClassName.get("cz.chrastecky.aiwallpaperchanger.provider", "AiHordeTextProvider");
        final ClassName sharedPreferences = ClassName.get("android.content", "SharedPreferences");
        final ClassName sharedPreferencesHelper = ClassName.get("cz.chrastecky.aiwallpaperchanger.helper", "SharedPreferencesHelper");
        final Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(InjectableTextProvider.class);

        MethodSpec.Builder getProvidersMethod = MethodSpec.methodBuilder("getProviders")
                .addModifiers(Modifier.PRIVATE)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), textProviderInterface))
                .addStatement("final List<$T> result = new $T<>()", textProviderInterface, ArrayList.class)
                .addAnnotation(nonNull)
        ;

        for (Element element : elements) {
            getProvidersMethod.addStatement("result.add(new $T(context))", element.asType());
        }
        getProvidersMethod.addStatement("return result");

        FieldSpec.Builder contextField = FieldSpec.builder(context, "context", Modifier.FINAL, Modifier.PRIVATE);

        MethodSpec.Builder constructorMethod = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(context, "context", Modifier.FINAL).addAnnotation(nonNull).build())
                .addStatement("this.context = context")
        ;

        MethodSpec.Builder findForModelMethod = MethodSpec.methodBuilder("findById")
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(nullable)
                .returns(textProviderInterface)
                .addParameter(ParameterSpec.builder(String.class, "id", Modifier.FINAL).addAnnotation(nonNull).build())
                .beginControlFlow("for (final $T provider : getProviders())", textProviderInterface)
                .beginControlFlow("if (provider.getId().equals(id))")
                .addStatement("return provider")
                .endControlFlow()
                .endControlFlow()
                .addStatement("return null")
        ;

        MethodSpec.Builder getCurrentProviderMethod = MethodSpec.methodBuilder("getCurrentProvider")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(nonNull)
                .returns(textProviderInterface)
                .addStatement("final $T preferences = new $T().get(context)", sharedPreferences, sharedPreferencesHelper)
                .addStatement("final $T currentId = preferences.getString($T.LLM_PROVIDER, $T.ID)", String.class, sharedPreferencesHelper, defaultProvider)
                .addStatement("final $T provider = findById(currentId)", textProviderInterface)
                .beginControlFlow("if (provider == null)")
                .addStatement("throw new $T($S)", RuntimeException.class, "Failed getting the current provider")
                .endControlFlow()
                .addStatement("return provider")
        ;

        TypeSpec generatedClass = TypeSpec.classBuilder(targetClassName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(contextField.build())
                .addMethod(constructorMethod.build())
                .addMethod(getProvidersMethod.build())
                .addMethod(findForModelMethod.build())
                .addMethod(getCurrentProviderMethod.build())
                .build();

        JavaFile javaFile = JavaFile.builder(targetNamespace, generatedClass).build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
