# Real Estate Tweets Sentiments Real Time Analysis
### Logstash Kafka SparkStreaming

This Project analysis, real estate's tweet sentiments in real time. It was realized using Logstash, Kafka, Spark streaming and Elasticsearch.

To install sbt: documentation available in https://www.scala-sbt.org/1.0/docs/Setup.html

To run this project:
## Logstash
- Download Logstash and Install it:  https://www.elastic.co/guide/en/logstash/current/getting-started-with-logstash.html
- Add the file logstash-tweets.conf in the Logstash directory
- Update Twitter with the correct information in the file logstash-tweets.conf
- Open Terminal in Logstash directory and run the command : `bin/logstash -f path_file_conf/logstash-tweets.conf`

## Kafka
- Install Kafka by following this documentation : https://kafka.apache.org/quickstart
- Run zekeeper server with this command `bin/zookeeper-server-start.sh config/zookeeper.propertie` (open teminal in kafka directory)
- Open terminal in Kafka directory and run the kafka's server with this command `bin/kafka-server-start.sh config/server.properties`

## Elasticsearch
- Download Elasticsearch from https://www.elastic.co/downloads/elasticsearch
- Open terminal in Elastcsearch directory and run this command : `bin/elasticsearch`

## Spark Streaming
- Download spark-2.4.0-bin-hadoop2.7.tgz from https://spark.apache.org/downloads.html
- Open the terminal in the directory where you downloaded this project
- Run `sbt assembly`
- Run `Path_Spark_Directory/spark-2.4.0-bin-hadoop2.7/bin/spark-submit --class "SimpleApp" --master local[4] target/scala-2.11/Simple-Project-assembly-1.0.jar`

Then, the results will be available in localhost:9200/twitter/_search

PS: The sentiments are calculated thanks to the function detectSentiment(message: String) done by Mr.Vincent Spiewak in his [repository](https://github.com/vspiewak/twitter-sentiment-analysis/blob/master/src/main/scala/com/github/vspiewak/util/SentimentAnalysisUtils.scala)
