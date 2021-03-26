# AWS Lambda to format Kotlin code using ktfmt

## Build

```
./gradlew build
```

## Deploy

```
./build_and_deploy.sh
```

The script creates two jars, one with the `com.facebook.ktfmt.onlineformatter` package, and the other with all of its dependencies (including ktfmt itself). This makes deploying just the Lambda faster.
