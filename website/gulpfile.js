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
const rimraf = require("rimraf");
const cp = require("child_process");
const CleanCSS = require("clean-css");
const uncss = require("uncss");

const VERSION = fs.readFileSync("../version.txt", "utf-8").trim();

async function _execute(task) {
  // Always invoke as if it were a callback task
  return new Promise((resolve, reject) => {
    if (task.length === 1) {
      // this is a calback task
      task((err) => {
        if (err) {
          return reject(err);
        }
        resolve();
      });
      return;
    }
    const taskResult = task();
    if (typeof taskResult === "undefined") {
      // this is a sync task
      resolve();
      return;
    }
    if (typeof taskResult.then === "function") {
      // this is a promise returning task
      taskResult.then(resolve, reject);
      return;
    }
    // this is a stream returning task
    taskResult.on("end", (_) => resolve());
    taskResult.on("error", (err) => reject(err));
  });
}

function taskSeries(...tasks) {
  return async () => {
    for (let i = 0; i < tasks.length; i++) {
      await _execute(tasks[i]);
    }
  };
}
// --- website
const cleanWebsiteTask = function (cb) {
  rimraf("../release-ktfmt-website", { maxBusyTries: 1 }, cb);
};
const buildWebsiteTask = taskSeries(cleanWebsiteTask, function () {
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
        .pipe(gulp.dest("../release-ktfmt-website"))
    )
    .pipe(
      es.through(
        function (data) {
          this.emit("data", data);
        },
        function () {
          // temporarily create package.json so that npm install doesn't bark
          fs.writeFileSync('../release-ktfmt-website/package.json', '{}');
          fs.writeFileSync('../release-ktfmt-website/.nojekyll', '');
          cp.execSync('npm install monaco-editor@0.23', {
            cwd: path.join(__dirname, '../release-ktfmt-website')
          });
          rimraf('../release-ktfmt-website/node_modules/monaco-editor/dev', function() {});
          rimraf('../release-ktfmt-website/node_modules/monaco-editor/esm', function() {});
          fs.unlinkSync('../release-ktfmt-website/package.json');

          this.emit("end");
        }
      )
    );
});
gulp.task("build-website", buildWebsiteTask);

gulp.task("prepare-website-branch", async function () {
  cp.execSync("git init", {
    cwd: path.join(__dirname, "../release-ktfmt-website"),
  });
  cp.execSync("git checkout -b gh-pages", {
    cwd: path.join(__dirname, "../release-ktfmt-website"),
  });
  cp.execSync("git add .", {
    cwd: path.join(__dirname, "../release-ktfmt-website"),
  });
  cp.execSync('git commit -m "Publish website"', {
    cwd: path.join(__dirname, "../release-ktfmt-website"),
  });
  console.log("To push, cd into ../release-ktfmt-website and run `git push https://github.com/facebookincubator/ktfmt.git gh-pages --force`");
});
