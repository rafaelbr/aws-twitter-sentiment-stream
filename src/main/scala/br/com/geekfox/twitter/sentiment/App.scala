package br.com.geekfox.twitter.sentiment

import br.com.geekfox.twitter.sentiment.exceptions.ArgumentNotFoundException
import br.com.geekfox.twitter.sentiment.properties.AppProperties
import br.com.geekfox.twitter.sentiment.spark.SparkSessionSingleton
import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.twitter.TwitterUtils
import org.elasticsearch.spark.rdd.EsSpark
import twitter4j.auth.Authorization
import twitter4j.conf.ConfigurationBuilder

import java.sql.{Date, Timestamp}
import java.util.Properties

/**
 * Hello world!
 *
 */
object App  {
  def main(args: Array[String]): Unit = {
    //primeira coisa a fazer, configurar arquivo de configuracao
    if (args.length == 0) {
      throw new ArgumentNotFoundException("O arquivo de propriedades nao foi informado")
    }
    val file = args(0)
    val ESEndpoint = args(1)

    AppProperties.setConfig(file)

    val config: Properties = AppProperties.getConfig()

    //configuracoes do twitter4j para o Twitter Streaming do Apache Bahir
    //a melhor forma de configurar eh mandar para as variaveis de sistema
    System.setProperty("twitter4j.oauth.consumerKey", config.getProperty("twitter.consumerKey"))
    System.setProperty("twitter4j.oauth.consumerSecret", config.getProperty("twitter.consumerSecret"))
    System.setProperty("twitter4j.oauth.accessToken", config.getProperty("twitter.accessToken"))
    System.setProperty("twitter4j.oauth.accessTokenSecret", config.getProperty("twitter.accessTokenSecret"))


    val spark: SparkSession = SparkSessionSingleton.getInstance(ESEndpoint)

    import spark.implicits._

    val ssc = new StreamingContext(spark.sparkContext, Seconds(5))

    val stream = TwitterUtils.createStream(ssc, None)

    //carrega modelo treinado anteriormente
    val model = PipelineModel.read.load(config.getProperty("model.s3.path"))

    //mapeia dados que queremos pegar do twitter
    val tweetsStream = stream.map(status => (status.getId.toString, new Timestamp(status.getCreatedAt.getTime), status.getUser.getName, status.getRetweetCount, status.getFavoriteCount, status.getText))

    tweetsStream.foreachRDD(rdd => {
      val df = rdd.toDF("id", "date", "user", "rt", "like", "tweet")
      val sentiment_df = model.transform(df)

      //grava no opensearch
      EsSpark
        .saveJsonToEs(sentiment_df.toJSON.rdd.repartition(config.getProperty("opensearch.dataframe.partitions").toInt), "twitter_messages", Map("es.mapping.id" -> "id"))
    })

    ssc.start()
    ssc.awaitTermination()

  }
}
