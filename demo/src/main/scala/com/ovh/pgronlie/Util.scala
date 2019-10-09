package com.ovh.pgronlie

import java.io.{File, FileOutputStream, InputStream}
import java.nio.file.Path
import java.util.zip.{ZipEntry, ZipInputStream}

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.joda.time.{DateTime, DateTimeZone, Interval, Period}

import scala.collection.JavaConversions._


object Util {
  def unzipArchive(savedModelZip: InputStream, path: Path): Path = {
    // Load the zip file
    val zipFile = new ZipInputStream(savedModelZip)
    val buffer = new Array[Byte](2048)
    var entry: ZipEntry = zipFile.getNextEntry
    while (entry != null) {
      val newFile: File = path.resolve(entry.getName).toFile
      if (entry.isDirectory) {
        printf("creating folder %s\n", newFile.toString)
        newFile.mkdirs
      } else {
        printf("creating file %s\n", newFile.toString)
        newFile.getParentFile.mkdirs()
        val fileOutputStream = new FileOutputStream(newFile)
        var len = zipFile.read(buffer)
        while (len > 0) {
          fileOutputStream.write(buffer, 0, len)
          len = zipFile.read(buffer)
        }
        fileOutputStream.close()
      }
      entry = zipFile.getNextEntry
    }
    path
  }

  def createSpark(): (SparkSession, Config) = {
    val conf = ConfigFactory.load()
    val sconf = new SparkConf()
      .setAppName("Demo Wikipedia")
    if (conf.hasPath("spark"))
      conf.getConfig("spark").entrySet().foreach( // copy all spark config into SparkConf
        entry => sconf.set("spark." + entry.getKey, entry.getValue.unwrapped.toString)
      )

    if (!sconf.contains("spark.master")) { // If --master not specified when using spark-*, use local node
      sconf.set("spark.master", "local[*]")
      sconf.set("spark.driver.host", "localhost")
    }
    val sc = SparkSession.builder().config(sconf).appName("Demo Wikipedia").getOrCreate()
    sc.conf.getAll.foreach {
      case (k, v) => if (!k.endsWith("password")) printf("spark.conf: %s: %s\n", k, v)
    }
    (sc, conf)
  }

  def timeStart(): Long = {
    DateTime.now(DateTimeZone.UTC).getMillis
  }

  def timeStop(time_start: Long): Period = {
    new Interval(time_start, DateTime.now(DateTimeZone.UTC).getMillis).toPeriod
  }

}
