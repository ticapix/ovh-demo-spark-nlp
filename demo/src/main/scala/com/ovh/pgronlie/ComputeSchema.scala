package com.ovh.pgronlie

import org.apache.spark.sql.types.{DataType, StructType}

object ComputeSchema {
  var wiki_pages: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream2.xml-p30304p88444"
  var wiki_schema: String = "swift://wiki.OVH/enwiki-20190801-pages-articles-multistream-schema.json"

  def main(args: Array[String]): Unit = {
    if (args.length > 0)
      wiki_pages = args(0)
    if (args.length > 1)
      wiki_schema = args(1)
    printf("Infering schema from %s\n", wiki_pages)

    val (sc, conf) = Util.createSpark()
    val df = sc.read
      .format("com.databricks.spark.xml")
      .option("rowTag", "page")
      .option("excludeAttribute", true)
      .load(wiki_pages)
    print("\n\n>>>>> SCHEMA <<<<<\n\n")
    df.printSchema()
    print("\n\n>>>>> SCHEMA <<<<<\n\n")
    import sc.implicits._
    Seq(df.schema.json).toDF("schema").write.mode("overwrite").json(wiki_schema);
    {
      // sanity check
      println(DataType.fromJson(sc.read.json(wiki_schema).first().getString(0)).asInstanceOf[StructType].prettyJson)
    }
  }
}
