#!/usr/bin/env bash
set -e -u -o pipefail

test_name="$(basename "${0}" .sh)"
project=checkstyle
repository=https://github.com/checkstyle/checkstyle.git
revision=checkstyle-10.13.0

if [ "${#}" -gt 2 ] || ([ "${#}" = 2 ] && [ "${1:---sync}" != '--sync' ]); then
  echo "Usage: ${0} [--sync] [<report_directory>]"
  exit 1
fi
do_sync="$([ "${#}" = 0 ] || [ "${1:-}" != '--sync' ] || echo 1)"
report_directory="$([ "${#}" = 0 ] || ([ -z "${do_sync}" ] && echo "${1}") || ([ "${#}" = 1 ] || echo "${2}"))"

# XXX: Configure Renovate to manage the AssertJ version declared here.
build_flags="-Dassertj.version=3.24.2"
validation_mvn_flags="-Dtest=!MetadataGeneratorUtilTest#metadataFilesGenerationAllFiles,!XdocsJavaDocsTest#allCheckSectionJavaDocs" 

./integration-test.sh "$test_name" "$project" "$repository" "$revision" "$build_flags" "" "" "$validation_mvn_flags" "$do_sync" "$report_directory"
