
import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.kafka.KafkaUtils
import kafka.serializer.StringDecoder
import org.elasticsearch.spark._
import scala.collection.immutable.Map
import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import org.json4s.JsonAST.JString

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.parsing.json.{JSON, JSONObject}
object SimpleApp {
  def main(args: Array[String]) : Unit = {
    val conf = new SparkConf().setMaster("local[*]").setAppName("KafkaSparkStreaming")
    val ssc = new StreamingContext(conf, Seconds(10))
    val topic = Set("logstashtest2")
    val kafkaParams = Map[String,String]("metadata.broker.list" -> "127.0.0.1:9092")
    val directMessage = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc,kafkaParams,topic).map(_._2)
    //directMessage.count().print()
    val tweets = directMessage.flatMap(t => t.toString().split("\\n"))
    tweets.foreachRDD { (rd, time) =>
      rd.map(t => {
        val js = JSON.parseFull(t).get.asInstanceOf[Map[String, Any]]
        println("Tweet Hazem : " + detectSentiment(js.get("text").orNull.toString))
        println("Tweet Hazem Created at :" + js.get("created_at").orNull.toString)
        Map(
          "created_at" -> js.get("created_at").orNull.toString,
          "sentiment" -> detectSentiment(js.get("text").orNull.toString)
        )
      }).saveToEs("twitter/tweet")
    }
    //tweets.foreachRDD(r => r.foreach(t => println(t)))
    /*directMessage.filter(t => {
      val tags = t.toString().split(" ").map(_.toLowerCase)
      tags.contains("stupid")
    }
    ).count().print()*/

    ssc.start()
    ssc.awaitTermination()
  }
  val nlpProps = {
    val props = new Properties()
    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, parse, sentiment")
    props
  }

  def detectSentiment(message: String): SENTIMENT_TYPE = {

    val pipeline = new StanfordCoreNLP(nlpProps)

    val annotation = pipeline.process(message)
    var sentiments: ListBuffer[Double] = ListBuffer()
    var sizes: ListBuffer[Int] = ListBuffer()

    var longest = 0
    var mainSentiment = 0

    for (sentence <- annotation.get(classOf[CoreAnnotations.SentencesAnnotation])) {
      val tree = sentence.get(classOf[SentimentCoreAnnotations.AnnotatedTree])
      val sentiment = RNNCoreAnnotations.getPredictedClass(tree)
      val partText = sentence.toString

      if (partText.length() > longest) {
        mainSentiment = sentiment
        longest = partText.length()
      }

      sentiments += sentiment.toDouble
      sizes += partText.length

      println("debug: " + sentiment)
      println("size: " + partText.length)

    }

    val averageSentiment: Double = {
      if (sentiments.size > 0) sentiments.sum / sentiments.size
      else -1
    }

    val weightedSentiments = (sentiments, sizes).zipped.map((sentiment, size) => sentiment * size)
    var weightedSentiment = weightedSentiments.sum / (sizes.fold(0)(_ + _))

    if (sentiments.size == 0) {
      mainSentiment = -1
      weightedSentiment = -1
    }


    println("debug: main: " + mainSentiment)
    println("debug: avg: " + averageSentiment)
    println("debug: weighted: " + weightedSentiment)

    /*
     0 -> very negative
     1 -> negative
     2 -> neutral
     3 -> positive
     4 -> very positive
     */
    weightedSentiment match {
      case s if s <= 0.0 => NOT_UNDERSTOOD
      case s if s < 1.0 => VERY_NEGATIVE
      case s if s < 2.0 => NEGATIVE
      case s if s < 3.0 => NEUTRAL
      case s if s < 4.0 => POSITIVE
      case s if s < 5.0 => VERY_POSITIVE
      case s if s > 5.0 => NOT_UNDERSTOOD
    }
  }
    trait SENTIMENT_TYPE
    case object VERY_NEGATIVE extends SENTIMENT_TYPE
    case object NEGATIVE extends SENTIMENT_TYPE
    case object NEUTRAL extends SENTIMENT_TYPE
    case object POSITIVE extends SENTIMENT_TYPE
    case object VERY_POSITIVE extends SENTIMENT_TYPE
    case object NOT_UNDERSTOOD extends SENTIMENT_TYPE


  }
