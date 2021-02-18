package v1.article

import javax.inject.{Inject, Provider}
import play.api.{Logger, MarkerContext}

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import v1.models.Article
import v1.producer.KafkaProducerImplemented

import scala.util.{Failure, Success}

/**
  * DTO for displaying article information.
  */
case class ArticleResource(id: String, link: String, title: String, body: String)

object ArticleResource {
  /**
    * Mapping to read/write a PostResource out as a JSON value.
    */
    implicit val format: Format[ArticleResource] = Json.format
}


/**
  * Controls access to the backend data, returning [[ArticleResource]]
  */
class ArticleResourceHandler @Inject()(routerProvider: Provider[ArticleRouter], articleRepository: ArticleRepository, kafkaProducer : KafkaProducerImplemented)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  def create(postInput: ArticleFormInput)(implicit mc: MarkerContext): Future[ArticleResource] = {
    val createData = Article(postInput.title, postInput.body)
    articleRepository.create(createData)
    kafkaProducer.sendEvent(createData._id.toString, createData.title)
    // To simplify, we don't wait for the return : the result will be logged
    Future{
      createArticleResource(createData)
    }
  }

  def update(id: String, articleInput: ArticleFormInput)(implicit mc: MarkerContext): Future[ArticleResource] = {
    val updateData = Article(articleInput.title, articleInput.body)
    articleRepository.get(id) onComplete{
      case Success(value) => {
        if(value.isDefined && !articleInput.title.equals(value.get.title)){
          logger.trace(s"New title written : $value")
          articleRepository.update(id, updateData)
          val event = "db.collection.updateOne(_id = ObjectId(\"" + id + "\"), {$set: {\"title\": " + updateData.title + "}})"
          kafkaProducer.sendEvent(updateData._id.toString, event)
        }
        if(value.isDefined && !articleInput.body.equals(value.get.body)){
          logger.trace(s"New body written : $value")
          articleRepository.update(id, updateData)
          val event = "db.collection.updateOne(_id = ObjectId(\"" + id + "\"), {$set: {\"body\": " + updateData.body + "}})"
          kafkaProducer.sendEvent(updateData._id.toString, event)
        }
        if(value.isEmpty){
          logger.trace("Error : Id not found")
        }
      }
      case Failure(e) => {
        e.printStackTrace()
      }
    }
    Future{createArticleResource(updateData)}
  }

  // Return a specific article
  def lookup(id: String)(implicit mc: MarkerContext): Future[Option[ArticleResource]] = {
    val postFuture = articleRepository.get(id)
    postFuture.map { maybePostData =>
      maybePostData.map { postData =>
        createArticleResource(postData)
      }
    }
  }

  // Return a list of articles
  def find(implicit mc: MarkerContext): Future[Iterable[ArticleResource]] = {
    articleRepository.list().map { postDataList =>
      postDataList.map(postData => createArticleResource(postData))
    }
  }

  private def createArticleResource(p: Article): ArticleResource = {
    ArticleResource(p._id.toString, routerProvider.get.link(p._id.toString), p.title, p.body)
  }

}
