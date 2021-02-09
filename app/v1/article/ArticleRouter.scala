package v1.article

import javax.inject.Inject

import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs to the PostResource controller.
  */
class ArticleRouter @Inject()(controller: ArticleController) extends SimpleRouter {

  val prefix = "/v1/articles"

  def link(id: String): String = {
    import io.lemonlabs.uri.dsl._
    val url = prefix / id
    url.toString()
  }

  override def routes: Routes = {
    case GET(p"/") =>
      controller.index

    case POST(p"/") =>
      controller.process(null)

    case PUT(p"/$id") =>
      controller.process(id)

    case GET(p"/$id") =>
      controller.show(id)
  }

}
