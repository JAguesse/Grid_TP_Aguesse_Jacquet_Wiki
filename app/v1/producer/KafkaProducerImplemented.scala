package v1.producer

import v1.models.Article

import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

class KafkaProducerImplemented {

  val kafkaProducerProps: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.serializer", classOf[StringSerializer].getName)
    props.put("value.serializer", classOf[StringSerializer].getName)
    props
  }

  val TOPIC = "articletopic"

  val producer = new KafkaProducer[String, String](kafkaProducerProps)

  def sendEvent(id : String, article: Article): Unit = {
    producer.send(new ProducerRecord[String, String](TOPIC, id, article.body))
  }

}
