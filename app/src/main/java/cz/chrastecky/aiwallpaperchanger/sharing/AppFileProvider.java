package cz.chrastecky.aiwallpaperchanger.sharing;

import androidx.core.content.FileProvider;

import cz.chrastecky.aiwallpaperchanger.R;

public class AppFileProvider extends FileProvider {
    public AppFileProvider() {
        super(R.xml.file_paths);
    }
}
