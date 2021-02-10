package v1.article

import javax.inject.{Inject, Provider}
import play.api.MarkerContext

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import v1.models.Article
import v1.producer.KafkaProducerImplemented

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
class ArticleResourceHandler @Inject()(routerProvider: Provider[ArticleRouter], postRepository: ArticleRepository)(implicit ec: ExecutionContext) {

  val kafkaProducer = new KafkaProducerImplemented

  def create(postInput: ArticleFormInput)(implicit mc: MarkerContext): Future[ArticleResource] = {
    val createData = Article(postInput.title, postInput.body)
    postRepository.create(createData)
    kafkaProducer.sendEvent(createData._id.toString, createData)
    // To simplify, we don't wait for the return : the result will be logged
    Future{
      createArticleResource(createData)
    }
  }

  def update(id: String, articleInput: ArticleFormInput)(implicit mc: MarkerContext): Future[ArticleResource] = {
    val updateData = Article(articleInput.title, articleInput.body)
    postRepository.update(id, updateData)
    kafkaProducer.sendEvent(updateData._id.toString, updateData)
    Future{
      createArticleResource(updateData)
    }
  }

  // Return a specific article
  def lookup(id: String)(implicit mc: MarkerContext): Future[Option[ArticleResource]] = {
    val postFuture = postRepository.get(id)
    postFuture.map { maybePostData =>
      maybePostData.map { postData =>
        createArticleResource(postData)
      }
    }
  }

  // Return a list of articles
  def find(implicit mc: MarkerContext): Future[Iterable[ArticleResource]] = {
    postRepository.list().map { postDataList =>
      postDataList.map(postData => createArticleResource(postData))
    }
  }

  private def createArticleResource(p: Article): ArticleResource = {
    ArticleResource(p._id.toString, routerProvider.get.link(p._id.toString), p.title, p.body)
  }

}
