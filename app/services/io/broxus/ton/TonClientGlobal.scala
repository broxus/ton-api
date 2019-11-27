package services.io.broxus.ton

import com.google.inject.{Inject, Singleton}
import io.broxus.ton.TonApi.TestGiverAccountState
import io.broxus.ton.{TonApi, TonClient}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

@Singleton
class TonClientGlobal @Inject()(config: TonClientConfig, lifecycle: ApplicationLifecycle) {

    private val client = {
        Logger.info("Create new ton client")
        TonClient.create(
            new TonClient.ResultHandler {

                def onResult(o: TonApi.Object): Unit = {
                    o match {
                        case error: TonApi.Error =>
                            Logger.error(s"Ton client error message ${error.code} [${error.message}]")
                        case _ => // ignore
                    }
                }
            },

            new TonClient.ExceptionHandler {

                def onException(e: Throwable): Unit = {
                    Logger.error("Ton client exception", e)
                }
            },

            new TonClient.ExceptionHandler {

                def onException(e: Throwable): Unit = {
                    Logger.error("Ton client exception", e)
                }
            }
        )
    }

    def send(query: TonApi.Function): Future[TonApi.Object] = {

        val promise = Promise[TonApi.Object]

        client.send(query, new TonClient.ResultHandler {

            override def onResult(o: TonApi.Object): Unit = {
                o match {
                    case error: TonApi.Error =>
                        promise.failure(TonClientException(
                            code = error.code,
                            message = s"${error.message}"
                        ))
                    case other => promise.success(other)
                }
            }
        })

        promise.future
    }

    // init client
    {
        Await.result(send(
            new TonApi.SetLogVerbosityLevel(0)),
            5.seconds
        )

        Await.result(send(new TonApi.Init(
            new TonApi.Options(
                new TonApi.Config(
                    config.liteClient,
                    "testnet2",
                    config.useNetworkCallback,
                    false
                ),
                new TonApi.KeyStoreTypeDirectory(config.keystore)
            )
        )), 30.seconds) match {

            case _: TonApi.Ok =>
                Logger.info("Ton client successfully initialized")

            case error: TonApi.Error =>
                throw new RuntimeException(s"Get an error on initializing ton client ${error.code} [${error.message}]")

            case o: TonApi.Object =>
                Logger.warn(s"Ton client sync return unknown result class [${o.getClass}]")
        }

        // start background sync process
        Logger.info("Start ton client sync process...")
        send(new TonApi.TestGiverGetAccountState()).onComplete {

            case Success(_: TestGiverAccountState) =>
                Logger.info("Ton client sync successfully finished")

            case Success(error: TonApi.Error) =>
                Logger.error(s"Get an error on ton client syncing ${error.code} [${error.message}]")

            case Success(o: TonApi.Object) =>
                Logger.warn(s"Ton client sync return unknown result class [${o.getClass}]")

            case Failure(t) =>
                Logger.error(s"Get an error on ton client syncing", t)
        }(concurrent.ExecutionContext.global)
    }

    lifecycle.addStopHook(() => {
        Logger.info("Try to stop ton client...")
        Future { client.close() }(concurrent.ExecutionContext.global)
    })

}

case class TonClientConfig(liteClient: String, keystore: String, useNetworkCallback: Boolean, verbosityLevel: Int)

case class TonClientException(code: Int, message: String) extends RuntimeException(message)