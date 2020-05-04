# Releasing a New `ktfmt` Version

1. Land a diff bumping the version to a non-SNAPSHOT version (e.g., `0.11`). For example, https://github.com/facebookincubator/ktfmt/commit/a674918aa9fddffed5321da3828fa028a520a406. Note that there are _2_ places to update.
2. Land another diff, bumping the version to the next SNAPSHOT version (e.g., `0.12-SNAPSHOT`). For example, https://github.com/facebookincubator/ktfmt/commit/ed2d3d0b363cc04d867f7c9fe84c5a28b1f5c62e
3. Obtain the private key used to sign `ktfmt` releases, as well as its passphrase.
4. Run `cd core && mvn -Prelease clean deploy`
5. Enter the passphrase when requested.
6. Create a new release at https://github.com/facebookincubator/ktfmt/releases . Use a previous release as a template.
7. Upload release files by dragging them from `core/target/`.

This is terribly manual. We'll hopefully soon improve this using GitHub Actions.
