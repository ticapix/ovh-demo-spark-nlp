package com.ovh.pgronlie

import org.apache.spark.sql._
import org.apache.spark.sql.types._


object CreateDataset {
  // var wiki_pages_xml: String = "s3a://wiki/enwiki-20190801-pages-articles-multistream-500MB.xml"
  // var wiki_schema: String = "s3a://wiki/enwiki-20190801-pages-articles-multistream-schema.json"
  // val wiki_pages: String = "s3a://wiki/enwiki-20190801-pages-articles-multistream.parquet"

  var wiki_pages_xml: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream-500MB.xml"
  var wiki_schema: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream-schema.json"
  val wiki_pages: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream2.parquet"

  def fromxml(spark: SparkSession, wiki_pages: String, wiki_schema: String): DataFrame = {
    val schema: StructType = DataType.fromJson(spark.read.json(wiki_schema).first().getString(0)).asInstanceOf[StructType]
    spark.read.format("com.databricks.spark.xml")
      .option("rowTag", "page")
      .option("excludeAttribute ", true)
      .schema(schema)
      .load(wiki_pages)
  }

  def main(args: Array[String]): Unit = {
    if (args.length >= 1)
      wiki_pages_xml = args(0)
    if (args.length >= 2)
      wiki_schema = args(1)
    printf("Reading data from %s\n", wiki_pages_xml)
    printf("Reading schema from %s\n", wiki_schema)
    printf("Writing data to %s\n", wiki_pages)

    val (sc, conf) = Util.createSpark();
    {
      val df = fromxml(sc, wiki_pages_xml, wiki_schema)
      printf("There are %d items\n", df.count())
      df.write.mode("overwrite")
        .parquet(wiki_pages);
    }
    {
      val df = sc.read.parquet(wiki_pages)
      printf("There are %d items\n", df.count())
    }
  }
}
