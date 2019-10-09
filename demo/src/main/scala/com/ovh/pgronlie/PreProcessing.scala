package com.ovh.pgronlie

import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

//  https://stackoverflow.com/questions/47450253/how-to-embed-spark-dataframe-columns-to-a-map-column
//  https://stackoverflow.com/questions/49499263/how-to-explode-an-array-into-multiple-columns-in-spark
//  https://stackoverflow.com/questions/56662066/how-to-convert-column-values-into-a-single-array-in-scala
//  https://alvinalexander.com/scala/seq-class-methods-examples-syntax

object PreProcessing {
//  var wiki_pages_xml: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream-500MB.xml"
  var wiki_pages_xml: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream2.xml-p30304p88444"
  var wiki_schema: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream-schema.json"
  var wiki_texts_parquet: String = "swift://wiki.OVH/enwiki-20190801-texts.parquet"

  val SHORTEST_EN_PARAGRAPH = 500
  val LONGUEST_EN_TOKEN = 15

  def extractTexts(sc: SparkSession, df: DataFrame): DataFrame = {
    df
      .select(explode(split(col("revision")("text"), "\n|!|\\.|\\?")).as("sentence"))
      .filter(length(col("sentence")) > SHORTEST_EN_PARAGRAPH)
      .withColumn("id", monotonically_increasing_id())
      .withColumn("token", explode(split(col("sentence"), " ")))
      .filter(length(col("token")) < LONGUEST_EN_TOKEN)
      .groupBy("id")
      .agg(array_join(collect_list(col("token")), " ").as("text"))
  }

  def fromxml(sc: SparkSession, wiki_pages: String, wiki_schema: String): DataFrame = {
    val schema: StructType = DataType.fromJson(sc.read.json(wiki_schema).first().getString(0)).asInstanceOf[StructType]
    sc.read.format("com.databricks.spark.xml")
      .option("rowTag", "page")
      .option("excludeAttribute ", true)
      .schema(schema)
      .load(wiki_pages)
  }

  def main(args: Array[String]): Unit = {
    if (args.length >= 1)
      wiki_pages_xml = args(0)
    printf("Reading data from %s\n", wiki_pages_xml);

    val (sc, conf) = Util.createSpark()
    val df = fromxml(sc, wiki_pages_xml, wiki_schema);
    val texts = extractTexts(sc, df)
    printf("There are %d pages\n", df.count())
    printf("There are %d tokens\n", df.select(explode(split(col("revision")("text"), "\n "))).count());
    printf("There are %d 'words'\n", texts.select(explode(split(col("text"), "\n "))).count());

    println("done")

    /*         val logFile = "swift://wiki.OVH/Makefile" // Should be some file on your system
        val text_file = spark.sparkContext.textFile(logFile);
        println("TEXT: " + text_file);
        val words = text_file.flatMap(line => line.split(" "))
        println("WORDS: " + words);
        val pairs = words.map(word => (word, 1))
        println("PAIRS: " + pairs);
        val count = pairs.reduceByKey((a,b) => a + b) //.collect
        println("COUNT: " + count);
        count.collect()
        println("COLLECT: " + count);

   */
  }
}
