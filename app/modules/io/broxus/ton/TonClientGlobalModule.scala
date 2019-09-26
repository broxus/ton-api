package modules.io.broxus.ton

import com.google.inject.{AbstractModule, Provides, Singleton}
import play.api.{Configuration, Environment}
import services.io.broxus.ton.{TonClientConfig, TonClientGlobal}

class TonClientGlobalModule(environment: Environment,
                            configuration: Configuration) extends AbstractModule {

    override def configure(): Unit = {
        bind(classOf[TonClientGlobal]).asEagerSingleton()
    }

    @Provides
    @Singleton
    def tonClientConfig(): TonClientConfig = {

        val liteClient = configuration.get[String]("ton.lite-client")
        val keystore = configuration.get[String]("ton.keystore")
        val useNetworkCallback = configuration.get[Boolean]("ton.use-network-callback")
        val verbosityLevel = configuration.get[Int]("ton.verbosity-level")

        TonClientConfig(
            liteClient = liteClient,
            keystore = keystore,
            useNetworkCallback = useNetworkCallback,
            verbosityLevel = verbosityLevel
        )
    }
}
