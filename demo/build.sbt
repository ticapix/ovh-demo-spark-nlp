version := "1.0"
scalaVersion := "2.11.12"

name := "DemoWiki"

logLevel in assembly := Level.Debug

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.9.5"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.5"
dependencyOverrides += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.9.5"

// with hadoop 3.1 for s3a
libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "3.2.1"
libraryDependencies += "org.apache.hadoop" % "hadoop-aws" % "3.2.1"
libraryDependencies += "org.apache.hadoop" % "hadoop-openstack" % "3.2.1"

libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.4"
libraryDependencies += "biz.paluch.logging" % "logstash-gelf" % "1.13.0"
libraryDependencies += "com.databricks" %% "spark-xml" % "0.6.0"
libraryDependencies += "com.johnsnowlabs.nlp" %% "spark-nlp" % "2.2.2"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.4.4"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
//  case x => MergeStrategy.last
}

mainClass in assembly := Some("DemoWiki")
