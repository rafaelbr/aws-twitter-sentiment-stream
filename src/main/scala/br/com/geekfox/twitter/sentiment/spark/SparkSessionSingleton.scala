package br.com.geekfox.twitter.sentiment.spark

import br.com.geekfox.twitter.sentiment.properties.AppProperties
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import java.util.Properties

object SparkSessionSingleton {
  @transient private var spark: SparkSession = _
  private val config:Properties= AppProperties.getConfig()

  /* Método responsável por inicializar ou retornar uma sessão spark
   * @return spark sessão spark
   */
  def getInstance(ESEndpoint: String): SparkSession = {

    if (spark == null) {
      val sparkConfig: SparkConf = new SparkConf()
        .setAppName(config.getProperty("app.name"))
        .setMaster("yarn")
        .set("es.nodes", "https://" + ESEndpoint)
        .set("es.port", config.getProperty("opensearch.port"))
        .set("es.nodes.wan.only", config.getProperty("opensearch.nodes.wan"))
        .set("es.net.http.auth.user", config.getProperty("opensearch.auth.user"))
        .set("es.net.http.auth.pass", config.getProperty("opensearch.auth.pwd"))
        .set("es.net.https.auth.user", config.getProperty("opensearch.auth.user"))
        .set("es.net.https.auth.pass", config.getProperty("opensearch.auth.pwd"))
        .set("es.batch.size.entries", config.getProperty("opensearch.batch.entries"))
        .set("es.batch.size.bytes", config.getProperty("opensearch.batch.size"))
        .set("es.batch.write.refresh", config.getProperty("opensearch.write.refresh"))
      spark = SparkSession
        .builder
        .config(sparkConfig)
        .getOrCreate
    }

    spark
  }

}
