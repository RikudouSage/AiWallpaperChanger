package cz.chrastecky.aiwallpaperchanger.text_formatter;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import cz.chrastecky.annotationprocessor.LlmTextFormatter;

@LlmTextFormatter
public class MetaLlama3Instruct implements TextFormatter {
    @Override
    public boolean supports(@NonNull String model) {
        return model.toLowerCase().contains("l3") || model.toLowerCase().contains("llama-3") || model.toLowerCase().contains("llama3");
    }

    @NonNull
    @Override
    public String encode(@NonNull final String message) {
        return "<|begin_of_text|>" +
                "<|start_header_id|>user<|end_header_id|>" +
                "\n\n" +
                message + "<|eot_id|>" +
                "<|start_header_id|>assistant<|end_header_id|>";
    }

    @NonNull
    @Override
    public String decode(@NonNull final String message) {
        return message.trim().split(Pattern.quote("<|eot_id|>"))[0];
    }
}
