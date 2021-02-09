package v1.article

import javax.inject.Inject

import play.api.Logger
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class ArticleFormInput(title: String, body: String)

/**
  * Takes HTTP requests and produces JSON.
  */
class ArticleController @Inject()(cc: ArticleControllerComponents)(
    implicit ec: ExecutionContext)
    extends ArticleBaseController(cc) {

  private val logger = Logger(getClass)

  private val form: Form[ArticleFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "title" -> nonEmptyText,
        "body" -> text
      )(ArticleFormInput.apply)(ArticleFormInput.unapply)
    )
  }

  def index: Action[AnyContent] = articleAction.async { implicit request =>
    logger.trace("index: ")
    articleResourceHandler.find.map { posts =>
      Ok(Json.toJson(posts))
    }
  }

  def process(id : String): Action[AnyContent] = articleAction.async { implicit request =>
    logger.trace("process: ")
    processJsonArticle(id)
  }


  def show(id: String): Action[AnyContent] = articleAction.async {
    implicit request =>
      logger.trace(s"show: id = $id")
      articleResourceHandler.lookup(id).map { post =>
        Ok(Json.toJson(post))
      }
  }

  private def processJsonArticle[A](id: String)(implicit request: ArticleRequest[A]): Future[Result] = {

    def failure(badForm: Form[ArticleFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: ArticleFormInput) = {
      if(id == null){
        articleResourceHandler.create(input).map { post =>
          Created(Json.toJson(post)).withHeaders(LOCATION -> post.link)
        }
      }
      else{
        articleResourceHandler.update(id, input).map { post =>
          Ok(Json.toJson(post)).withHeaders(LOCATION -> post.link)
        }
      }
    }

    form.bindFromRequest().fold(failure, success)
  }
}
