# AI Wallpaper Changer

An app using AI Horde to generate a new wallpaper at a specified interval.

In simple mode you only choose a prompt and a negative prompt (and NSFW toggle if you have the NSFW-enabled version), while in advanced mode you can change all the common parameters.

## Download

The app can either be downloaded from [Google Play](https://play.google.com/store/apps/details?id=cz.chrastecky.aiwallpaperchanger) or from the [latest release](https://github.com/RikudouSage/AiWallpaperChanger/releases/latest)
here on GitHub. I recommend using [Obtainium])https://github.com/ImranR98/Obtainium) instead of manually downloading apk files.

## Play Store version vs. GitHub release

Due to the Play Store policy, the version that's on there has NSFW always disabled. When downloading from GitHub releases, you can either install a SFW or a NSFW version.
Note that the NSFW version does not mean that every image is NSFW, just that you can toggle whether you want to enable it or not.

The Play Store version also contains optional Premium subscription, there's no equivalent in the GitHub releases version.

All apps are signed using the same signing key, meaning you can update between each other without having to uninstall first.

## Building

You need to create the file `local.properties`, even if it's empty. Other than that there's nothing out of the ordinary - open the project in Android Studio and build away.
Or use Gradle to build it from CLI.