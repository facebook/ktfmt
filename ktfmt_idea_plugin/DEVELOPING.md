## Meaning of Versions

Versions are defined in `build.gradle.kts`.

`intellij.version` is used for -
1. Building the plugin, so the source code needs to be compatible with it.
2. Determining the _maximum_ supported version. This means that it needs to be updated every time a new IntelliJ _Branch_ version is released. For example, 2021.1, 2021.2, etc.

`tasks.patchPluginXml.sinceBuild` is used for -
1. Determinig the _minimum_ supported version, so the code must not reference classes not introduced yet.


## Testing

* Use Gradle to run the plugin.
* _Don't_ use IntelliJ to run the plugin.

```
ktfmt/ktfmt_idea_plugin $ ./gradlew runIde
```

Point `intellij.alternativeIdePath` in `build.gradle.kts` to a local installation of IntelliJ / Android Studio to test the plugin with it. Don't forget to enable the plugin. Easiest way: Cmd-shift-A, type 'ktfmt', look for Preferences.

Another way: change `intellij.version` and comment out `alternativeIdePath`. This will run the plugin on whatever version you put - it'll be downloaded through Gradle.
