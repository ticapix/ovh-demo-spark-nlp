package com.ovh.pgronlie

import java.io.FileInputStream
import java.nio.file.{Files, Path, Paths}

import com.johnsnowlabs.nlp.annotator.PerceptronModel
import com.johnsnowlabs.nlp.{DocumentAssembler, Finisher}
import com.johnsnowlabs.nlp.annotators.{LemmatizerModel, Normalizer, Tokenizer}
import com.johnsnowlabs.nlp.annotators.sbd.pragmatic.SentenceDetector
import org.apache.commons.io.FilenameUtils
import org.apache.spark.SparkFiles
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.apache.spark.ml.Pipeline


object DemoWiki {
  //  var wiki_pages_xml: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream-500MB.xml"
  var wiki_pages_xml: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream2.xml-p30304p88444"
  var wiki_schema: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream-schema.json"
  var wiki_verbs: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream-verbs.parquet"

  var pos_model_path: Path = Paths.get("")
  var lemma_model_path: Path = Paths.get("")

  val SHORTEST_EN_PARAGRAPH = 500
  val LONGUEST_EN_TOKEN = 15

  def extractVerb32(sc: SparkSession, df: DataFrame): DataFrame = {
    val preprocessed = df
      .select(explode(split(col("revision")("text"), "\n|!|\\.|\\?")).as("sentence"))
      .filter(length(col("sentence")) > SHORTEST_EN_PARAGRAPH)
      .withColumn("id", monotonically_increasing_id())
      .withColumn("token", explode(split(col("sentence"), " ")))
      .filter(length(col("token")) < LONGUEST_EN_TOKEN)
      .groupBy("id")
      .agg(array_join(collect_list(col("token")), " ").as("text"))
      .repartition(1000)

    val documenter = new DocumentAssembler().setInputCol("text").setOutputCol("document")
    val tokenizer = new Tokenizer().setInputCols(Array("document")).setOutputCol("token")
    val normalizer = new Normalizer().setInputCols("token").setOutputCol("normalized").setLowercase(true)
    val pos_tagger = PerceptronModel.load(pos_model_path.toString).setInputCols("document", "normalized").setOutputCol("pos")
    val lemmatizer = LemmatizerModel.load(lemma_model_path.toString).setInputCols("normalized").setOutputCol("lemma")
    val finisher = new Finisher().setInputCols(Array("normalized", "pos", "lemma")).setOutputCols(Array("normalized", "pos", "lemma"))

    val pipeline = new Pipeline().setStages(Array(
      documenter,
      tokenizer,
      normalizer,
      pos_tagger,
      lemmatizer,
      finisher
    ))
    pipeline.fit(preprocessed).transform(preprocessed)
      .withColumn("result", explode(arrays_zip(col("lemma"), col("pos"))))
      .select(col("result"))
      .filter(col("result.pos").startsWith("VB"))
      .select(col("result.lemma").as("verb"))
      .groupBy("verb")
      .count()
      .orderBy(desc("count"))
  }

  def fromxml(sc: SparkSession, wiki_pages: String, wiki_schema: String): DataFrame = {
    val schema: StructType = DataType.fromJson(sc.read.json(wiki_schema).first().getString(0)).asInstanceOf[StructType]
    sc.read.format("com.databricks.spark.xml")
      .option("rowTag", "page")
      .option("excludeAttribute ", true)
      .schema(schema)
      .load(wiki_pages)
  }

  def extractJavaResource(resource_path: String): Path = {
    val path = Paths.get("/tmp/", FilenameUtils.getBaseName(resource_path))
    val modelStream = getClass.getResourceAsStream(resource_path)
    printf("extract model from %s to %d\n", resource_path, path.toString)
    Util.unzipArchive(modelStream, path)
  }

  def extractSparkResource(resource_name: String): Path = {
    val folder_path = Paths.get("/tmp/", FilenameUtils.getBaseName(resource_name))
    val resource_path = SparkFiles.get(resource_name)
    if (!Files.exists(Paths.get(resource_path)))
      return extractJavaResource("/" + resource_name)
    printf("extract model from %s to %s\n", resource_path, folder_path.toString)
    Util.unzipArchive(new FileInputStream(resource_path), folder_path)
  }

  //  https://stackoverflow.com/questions/47450253/how-to-embed-spark-dataframe-columns-to-a-map-column
  //  https://stackoverflow.com/questions/49499263/how-to-explode-an-array-into-multiple-columns-in-spark
  //  https://stackoverflow.com/questions/56662066/how-to-convert-column-values-into-a-single-array-in-scala
  //  https://alvinalexander.com/scala/seq-class-methods-examples-syntax

  def main(args: Array[String]): Unit = {
    if (args.length >= 1)
      wiki_pages_xml = args(0)
    printf("Reading data from %s\n", wiki_pages_xml);

    val (sc, conf) = Util.createSpark()
    pos_model_path = Paths.get("/tmp/pos_model/")
    lemma_model_path = Paths.get("/tmp/lemma_model/")
    val df = fromxml(sc, wiki_pages_xml, wiki_schema)

    /*
        val df = sc.read.json(Seq(
          """{ "revision": { "text": "This is a very useless/ugly sentence. 2   \n\n     +--------------------------+ \n\n       || this | is | an | array. || \n\n      | What else ? 3"}}""",
          """{ "revision": { "text": "Sun is **[blue|https://www.bing.com/color]** ! ok \n\n "}}""").toDS)
     */
    {
      val start = Util.timeStart()
      val verbs = extractVerb32(sc, df)
      verbs.limit(1000).write.mode("overwrite").parquet(wiki_verbs)
      printf("[32] Took %s to process\n", Util.timeStop(start))
    }
    {
      printf("List of verbs\n")
      val verbs = sc.read.parquet(wiki_verbs)
      verbs.show(100)
    }
    println("done")
  }
}
