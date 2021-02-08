package v1.post

import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}
import play.api.libs.concurrent.CustomExecutionContext
import play.api.{Logger, MarkerContext}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

final case class PostData(id: PostId, title: String, body: String)

class PostId private (val underlying: Int) extends AnyVal {
  override def toString: String = underlying.toString
}

object PostId {
  def apply(raw: String): PostId = {
    require(raw != null)
    new PostId(Integer.parseInt(raw))
  }
}

class PostExecutionContext @Inject()(actorSystem: ActorSystem)
    extends CustomExecutionContext(actorSystem, "repository.dispatcher")

/**
  * A pure non-blocking interface for the PostRepository.
  */
trait PostRepository {
  def create(data: PostData)(implicit mc: MarkerContext): Future[PostId]

  def list()(implicit mc: MarkerContext): Future[Iterable[PostData]]

  def get(id: PostId)(implicit mc: MarkerContext): Future[Option[PostData]]
}

/**
  * A trivial implementation for the Post Repository.
  *
  * A custom execution context is used here to establish that blocking operations should be
  * executed in a different thread than Play's ExecutionContext, which is used for CPU bound tasks
  * such as rendering.
  */
@Singleton
class PostRepositoryImpl @Inject()()(implicit ec: PostExecutionContext)
    extends PostRepository {

  private val logger = Logger(this.getClass)

  val mongoClient: MongoClient = MongoClient()
  val database: MongoDatabase = mongoClient.getDatabase("wikidb")
  // [PostData] was [Document]
  val collection: MongoCollection[PostData] = database.getCollection("articles")

  override def list()(implicit mc: MarkerContext): Future[Iterable[PostData]] = {
    logger.trace("The list was asked")
    collection.find().toFuture()
  }

  override def get(id: PostId)(implicit mc: MarkerContext): Future[Option[PostData]] = {
    logger.trace(s"get: id = $id")
    collection.find(equal("id", id)).first().toFuture()
  }

  def create(data: PostData)(implicit mc: MarkerContext): Future[PostId] = {
    collection.insertOne(data).toFuture().
  }

}
