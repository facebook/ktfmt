## Meaning of Versions

Versions are defined in `ktfmt/gradle/libs.versions.toml`.

`intellijPlatform.pluginConfiguration.ideaVersion.sinceBuild` is used for -
* Building the plugin, so the source code needs to be compatible with it.
* Determinig the _minimum_ supported version, so the code must not reference classes not introduced yet.

`intellijPlatform.pluginConfiguration.ideaVersion.untilBuild` is used for -
* Determining the _maximum_ supported version


## Testing

* Use Gradle to run the plugin.
* _Don't_ use IntelliJ to run the plugin.

```
ktfmt $ ./gradlew :idea_plugin:runIde
```


### Outdated docs

NOTE: This section needs to be updated. Information here is no longer accurate.

Point `intellij.alternativeIdePath` in `build.gradle.kts` to a local installation of IntelliJ / Android Studio to test the plugin with it. Don't forget to enable the plugin. Easiest way: Cmd-shift-A, type 'ktfmt', look for Preferences.

Another way: change `intellij.version` and comment out `alternativeIdePath`. This will run the plugin on whatever version you put - it'll be downloaded through Gradle.
