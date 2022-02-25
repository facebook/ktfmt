# A demo website for ktfmt

## Run

```
npm install .
npm run simpleserver
```

## Build for deployment

```
npm install .
KTFMT_TMP_DIR=$(mktemp -d)
KTFMT_WEBSITE_OUTPUT_DIR=$KTFMT_TMP_DIR gulp build-website

# Clean up:
rm -rf "$KTFMT_TMP_DIR"
```
