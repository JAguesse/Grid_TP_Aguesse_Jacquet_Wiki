package v1.article

import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import com.mongodb.client.result.UpdateResult
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.{MongoClient, MongoCollection, MongoDatabase, Observer, Subscription}
import play.api.libs.concurrent.CustomExecutionContext
import play.api.{Logger, MarkerContext}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.result.InsertOneResult
import v1.models.Article
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.model.Updates.{combine, set}

import scala.concurrent.{Await, Future}

class ArticleExecutionContext @Inject()(actorSystem: ActorSystem)
    extends CustomExecutionContext(actorSystem, "repository.dispatcher")

/**
  * A pure non-blocking interface for the PostRepository.
  */
trait ArticleRepository {

  def create(data: Article)(implicit mc: MarkerContext) : Unit

  def update(id: String, article: Article)(implicit mc: MarkerContext) : Unit

  def list()(implicit mc: MarkerContext): Future[Iterable[Article]]

  def get(id: String)(implicit mc: MarkerContext): Future[Option[Article]]
}

/**
  * A trivial implementation for the Post Repository.
  *
  * A custom execution context is used here to establish that blocking operations should be
  * executed in a different thread than Play's ExecutionContext, which is used for CPU bound tasks
  * such as rendering.
  */
@Singleton
class ArticleRepositoryImpl @Inject()()(implicit ec: ArticleExecutionContext) extends ArticleRepository {

  private val logger = Logger(this.getClass)

  val codecRegistry = fromRegistries(fromProviders(classOf[Article]), DEFAULT_CODEC_REGISTRY )

  val mongoClient: MongoClient = MongoClient()
  val database: MongoDatabase = mongoClient.getDatabase("wikidb").withCodecRegistry(codecRegistry)
  val collection: MongoCollection[Article] = database.getCollection("articles")

  override def list()(implicit mc: MarkerContext): Future[Iterable[Article]] = {
    logger.trace("list() : list of articles was asked")
    collection.find().toFuture()
  }

  override def get(id: String)(implicit mc: MarkerContext): Future[Option[Article]] = {
    logger.trace(s"get() : _id = $id")
    collection.find(equal("_id", new ObjectId(id))).first().toFutureOption()
  }

  def create(article: Article)(implicit mc: MarkerContext) : Unit = {
    logger.trace(s"create() : $article")
    collection.insertOne(article).subscribe(new Observer[InsertOneResult] {

      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)

      override def onNext(result: InsertOneResult): Unit = logger.trace(s"onNext $result")

      override def onError(e: Throwable): Unit = logger.trace("Failed")

      override def onComplete(): Unit = logger.trace("Completed")
    })
  }

  def update(id: String, article: Article)(implicit mc: MarkerContext) : Unit = {
    logger.trace(s"update() : $id")
    val values = combine(set("title", article.title), set("body", article.body))
    collection.updateOne(equal("_id",new ObjectId(id)), values).subscribe(new Observer[UpdateResult] {

      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)

      override def onNext(result: UpdateResult): Unit = logger.trace(s"onNext $result")

      override def onError(e: Throwable): Unit = logger.trace("Failed")

      override def onComplete(): Unit = logger.trace("Completed")
    })
  }

}
