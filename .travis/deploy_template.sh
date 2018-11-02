#!/bin/bash

tag=$1

export GOOGLE_APPLICATION_CREDENTIALS="${HOME}/service-account.json"

cd ${TRAVIS_BUILD_DIR}

project_version=$(sbt -no-colors version | perl -ne 'print "$1\n" if /info.*(\d+\.\d+\.\d+[^\r\n]*)/' | tail -n 1 | tr -d '\n')
if [[ "${tag}" = *"${project_version}" ]]; then
    sbt "runMain com.snowplowanalytics.storage.googlecloudstorage.loader.CloudStorageLoader --project=engineering-sandbox \
      --templateLocation=gs://snowplow-hosted-assets-tmp/4-storage/snowplow-google-cloud-storage-loader/${tag}/SnowplowGoogleCloudStorageLoaderTemplate-${tag} \
      --stagingLocation=gs://snowplow-hosted-assets-tmp/4-storage/snowplow-google-cloud-storage-loader/${tag}/staging \
      --runner=DataflowRunner \
      --tempLocation=gs://snowplow-hosted-assets-tmp/tmp"
else
    echo "Tag version '${tag}' doesn't match version in scala project ('${project_version}'). aborting!"
    exit 1
fi
