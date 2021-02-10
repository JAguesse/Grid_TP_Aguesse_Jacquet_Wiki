package v1.producer

import com.fasterxml.jackson.databind.ser.std.StringSerializer
import play.api.MarkerContext
import v1.models.Article

import java.util.Properties
import scala.concurrent.Future

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

class KafkaProducerImplemented {

  val kafkaProducerProps: Properties = {
    val props = new Properties()
    props.put("bootstrap.servers", "localhost:9092")
    props.put("key.serializer", classOf[StringSerializer].getName)
    props.put("value.serializer", classOf[StringSerializer].getName)
    props
  }

  val TOPIC = "articletopic"

  val producer = new KafkaProducer[String, Article](kafkaProducerProps)

  def sendEvent(id : String, article: Article): Unit = {
    producer.send(new ProducerRecord[String, Article](TOPIC, id, article))
  }

}
