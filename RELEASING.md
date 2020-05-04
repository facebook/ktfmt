# Releasing a New `ktfmt` Version

1. Update `bump_versions.sh` with the version information, and comment out its `exit` command.
2. Run `./bump_versions.sh`.
3. Obtain the private key used to sign `ktfmt` releases, as well as its passphrase.
3. Make sure your `~/.m2/settings.xml` is:

    ```
    <settings>
    <servers>
        <server>
        <id>ossrh</id>
        <username>your-jira-id</username>
        <password>your-jira-pwd</password>
        </server>
    </servers>
    </settings>
    ```

4. Run `mvn -Prelease clean deploy`
5. Enter the passphrase when requested.
6. Create a new release at https://github.com/facebookincubator/ktfmt/releases . Use a previous release as a template.
7. Upload release files by dragging them from `core/target/`.

This is terribly manual. We'll hopefully soon improve this using GitHub Actions.
