# Releasing a New `ktfmt` Version

1. Run `./bump_versions.sh $OLD_VERSION $NEW_VERSION` and commit the changes.
2. Create a new Release in GitHub. A GitHub Action is automatically triggered and builds and publishes the artifacts to
    1. Maven
    2. IntelliJ Plugin marketplace
3. TODO: also automate website generation (https://facebookincubator.github.io/ktfmt/) and the AWS Lambda that powers it. For now, you must clone the repo locally, and manually run some steps.
    1. pushd online_formatter; ./build_and_deploy.sh; popd
        1. Credentials should be configured using https://docs.aws.amazon.com/cli/latest/topic/config-vars.html#credentials
    2. Follow instructions in website/README.md
