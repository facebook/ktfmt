/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Forked from https://microsoft.github.io/monaco-editor/ */

const gulp = require("gulp");
const es = require("event-stream");
const path = require("path");
const fs = require("fs");
const os = require('os');
const rimraf = require("rimraf");
const cp = require("child_process");
const CleanCSS = require("clean-css");
const uncss = require("uncss");

const VERSION = fs.readFileSync("../version.txt", "utf-8").trim();

allow_deploy_to_github = process.env.KTFMT_WEBSITE_ALLOW_DEPLOY_TO_GITHUB == '1';
outdir = process.env.KTFMT_WEBSITE_OUTPUT_DIR || path.join(__dirname, '../release-ktfmt-website');
console.log('Using output dir: ' + outdir)

// --- website
const cleanWebsiteTask = function (cb) {
  rimraf(outdir, { maxBusyTries: 1 }, cb);
};
const buildWebsiteTask = function () {
  function replaceWithRelativeResource(dataPath, contents, regex, callback) {
    return contents.replace(regex, function (_, m0) {
      var filePath = path.join(path.dirname(dataPath), m0);
      return callback(m0, fs.readFileSync(filePath));
    });
  }

  var waiting = 0;
  var done = false;

  return es
    .merge(
      gulp
        .src(["**/*"], {
          dot: true,
          ignore: [
            "package.json",
            "package-lock.json",
            "node_modules/**/*",
            "gulpfile.js",
            '.DS_Store',
          ],
        })
        .pipe(
          es.through(
            function (data) {
              if (!data.contents || !/\.(html)$/.test(data.path)) {
                return this.emit("data", data);
              }

              var contents = data.contents.toString();
              contents = contents.replace(/{{version}}/g, VERSION);
              contents = contents.replace(
                /{{year}}/g,
                new Date().getFullYear()
              );

              var allCSS = "";
              var tmpcontents = replaceWithRelativeResource(
                data.path,
                contents,
                /<link data-inline="yes-please" href="([^"]+)".*/g,
                function (m0, fileContents) {
                  allCSS += fileContents.toString("utf8");
                  return "";
                }
              );
              tmpcontents = tmpcontents.replace(/<script.*/g, "");
              tmpcontents = tmpcontents.replace(/<link.*/g, "");

              waiting++;
              uncss(
                tmpcontents,
                { raw: allCSS },
                function (err, output) {
                  waiting--;

                  if (!err) {
                    output = new CleanCSS().minify(output).styles;
                    var isFirst = true;
                    contents = contents.replace(
                      /<link data-inline="yes-please" href="([^"]+)".*/g,
                      function (_, m0) {
                        if (isFirst) {
                          isFirst = false;
                          return "<style>" + output + "</style>";
                        }
                        return "";
                      }
                    );
                  }

                  // Inline javascript
                  contents = replaceWithRelativeResource(
                    data.path,
                    contents,
                    /<script data-inline="yes-please" src="([^"]+)".*/g,
                    function (m0, fileContents) {
                      return (
                        "<script>" + fileContents.toString("utf8") + "</script>"
                      );
                    }
                  );

                  data.contents = Buffer.from(
                    contents.split(/\r\n|\r|\n/).join("\n")
                  );
                  this.emit("data", data);

                  if (done && waiting === 0) {
                    this.emit("end");
                  }
                }.bind(this)
              );
            },
            function () {
              done = true;
              if (waiting === 0) {
                this.emit("end");
              }
            }
          )
        )
        .pipe(gulp.dest(outdir))
    )
    .pipe(
      es.through(
        function (data) {
          this.emit("data", data);
        },
        function () {
          // temporarily create package.json so that npm install doesn't bark
          fs.writeFileSync(path.join(outdir, 'package.json'), '{}');
          fs.writeFileSync(path.join(outdir, '.nojekyll'), '');
          cp.execSync('npm install monaco-editor@0.23', {
            cwd: outdir
          });
          rimraf.sync(path.join(outdir, 'node_modules/monaco-editor/dev'));
          rimraf.sync(path.join(outdir, 'node_modules/monaco-editor/esm'));
          fs.unlinkSync(path.join(outdir, 'package.json'));

          this.emit("end");
        }
      )
    );
}
buildWebsiteSeries = gulp.series(cleanWebsiteTask, buildWebsiteTask);
gulp.task("build-website", buildWebsiteSeries);
