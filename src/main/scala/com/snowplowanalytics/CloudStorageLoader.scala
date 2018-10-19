package com.snowplowanalytics

import com.spotify.scio._
import org.apache.beam.sdk.io.{Compression, FileBasedSink, TextIO}
import org.apache.beam.sdk.io.fs.ResourceId
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.options.ValueProvider.{NestedValueProvider, StaticValueProvider}
import org.apache.beam.sdk.transforms.SerializableFunction
import org.apache.beam.sdk.transforms.windowing.{FixedWindows, Window}
import org.joda.time.{Duration, Instant}

object CloudStorageLoader {
  def main(args: Array[String]): Unit = {
    val options = PipelineOptionsFactory
      .fromArgs(args: _*)
      .withValidation
      .as(classOf[Options])

    options.setStreaming(true)

    run(options)
  }

  def run(options: Options): Unit = {
    val sc = ScioContext(options)

    val input = sc.pubsubSubscription[String](options.getInputSubscription).withName("input")
      .applyTransform(
        Window.into(FixedWindows.of(Duration.standardMinutes(options.getWindowDuration)))
      )

    input
      .saveAsCustomOutput("output", TextIO.write()
        .withWindowedWrites
        .withNumShards(options.getNumShards)
        .withWritableByteChannelFactory(
          FileBasedSink.CompressionType.fromCanonical(getCompression(options.getCompression)))
        .withTempDirectory(NestedValueProvider.of(
          StaticValueProvider.of(options.getOutputDirectory),
          new SerializableFunction[String, ResourceId] {
            def apply(input: String): ResourceId =
              FileBasedSink.convertToFileResourceIfPossible(input)
          }
        ))
        .to(WindowedFilenamePolicy(
          options.getOutputDirectory,
          options.getOutputFilenamePrefix,
          options.getShardTemplate,
          options.getOutputFilenameSuffix
        ))
      )

    sc.close()
  }

  private def getCompression(s: String): Compression = s.trim.toLowerCase match {
    case "bz2" => Compression.BZIP2
    case "gzip" => Compression.GZIP
    case _ => Compression.UNCOMPRESSED
  }
}
