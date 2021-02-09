package v1.models

import org.mongodb.scala.bson.ObjectId

object Article {
  def apply(title: String, body: String): Article =
    Article(new ObjectId(), title, body)
}
case class Article(_id: ObjectId, title: String, body: String)