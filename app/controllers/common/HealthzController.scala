package controllers.common

import com.google.inject.{Singleton, Inject}
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.Future

@Singleton
class HealthzController @Inject()(controllerComponents: ControllerComponents)
    extends AbstractController(controllerComponents) {

    def healthz = Action.async {
        Future.successful(Ok)
    }
}

