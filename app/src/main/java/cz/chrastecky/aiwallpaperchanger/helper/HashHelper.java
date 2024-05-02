package cz.chrastecky.aiwallpaperchanger.helper;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashHelper {
    @Nullable
    public static String sha256(@NonNull String input) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            hash.update(input.getBytes(StandardCharsets.UTF_8));

            return bytesToHexString(hash.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.e("AiWallpaperChanger", "Missing SHA-256 algorithm", e);
            return null;
        }
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte byte_ : bytes) {
            String hex = Integer.toHexString(0xFF & byte_);
            if (hex.length() == 1) {
                buffer.append('0');
            }
            buffer.append(hex);
        }

        return buffer.toString();
    }
}
