package controllers.io.broxus.ton

import com.google.inject.{Inject, Singleton}
import io.broxus.ton.TonApi
import models.io.broxus.ton._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import services.io.broxus.ton.{TonClientException, TonClientGlobal}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TonApiController @Inject()(controllerComponents: ControllerComponents,
                                 client: TonClientGlobal)
                                (implicit ec: ExecutionContext)
    extends AbstractController(controllerComponents) {

    def createNewKey: Action[AnyContent] = Action.async {
        client.send(new TonApi.CreateNewKey(
            null,
            null,
            null
        )).flatMap {

            case key: TonApi.Key =>
                client.send(new TonApi.ExportKey(new TonApi.InputKey(
                    key,
                    null
                ))).map {

                    case exportedKey: TonApi.ExportedKey =>
                        Ok(Json.toJson(CreateNewKeyResponse(
                            publicKey = key.publicKey,
                            password = BinaryData(key.secret),
                            seed = exportedKey.wordList
                        )))

                    case other =>
                        BadRequest(Json.toJson(ResponseError(
                            code = -100,
                            message = s"Unknown response class [${other.getClass.getName}]"
                        )))
                }

            case other =>
                Future.successful(BadRequest(Json.toJson(ResponseError(
                    code = -100,
                    message = s"Unknown response class [${other.getClass.getName}]"
                ))))
        }.recover {

            case TonClientException(code, message) =>
                BadRequest(Json.toJson(ResponseError(
                    code = code,
                    message = message
                )))

            case t: Throwable =>
                Logger.error("Client request error", t)
                InternalServerError(Json.toJson(ResponseError(
                    code = -1000,
                    message = s"Get an error on send request [${t.getMessage}]"
                )))
        }
    }

    def accountAddress: Action[AccountAddressRequest] = Action.async(parse.json[AccountAddressRequest]) { r =>

        val addressRequest = r.body

        val addressRequestFuture = addressRequest.accountType match {

            case AccountAddressTypes.TestWallet =>
                client.send(new TonApi.TestWalletGetAccountAddress(new TonApi.TestWalletInitialAccountState(
                    r.body.publicKey
                )))

            case AccountAddressTypes.Wallet =>
                client.send(new TonApi.WalletGetAccountAddress(new TonApi.WalletInitialAccountState(
                    r.body.publicKey
                )))

            case other =>
                Future.failed(throw new Exception(s"Unsupported account address type [$other]"))

        }

        addressRequestFuture.flatMap {

            case address: TonApi.AccountAddress =>
                client.send(new TonApi.UnpackAccountAddress(address.accountAddress)).flatMap {

                    case unpacked: TonApi.UnpackedAccountAddress =>
                        client.send(new TonApi.PackAccountAddress(new TonApi.UnpackedAccountAddress(
                            unpacked.workchainId,
                            false,
                            unpacked.testnet,
                            unpacked.addr
                        ))).map {

                            case initAddress: TonApi.AccountAddress =>
                                Ok(Json.toJson(AccountAddressResponse(
                                    address = address.accountAddress,
                                    initAddress = initAddress.accountAddress,
                                    unpacked = UnpackedAccountAddress(unpacked)
                                )))

                            case other =>
                                BadRequest(Json.toJson(ResponseError(
                                    code = -100,
                                    message = s"Unknown response class [${other.getClass.getName}]"
                                )))
                        }

                    case other =>
                        Future.successful(BadRequest(Json.toJson(ResponseError(
                            code = -100,
                            message = s"Unknown response class [${other.getClass.getName}]"
                        ))))
                }

            case other =>
                Future.successful(BadRequest(Json.toJson(ResponseError(
                    code = -100,
                    message = s"Unknown response class [${other.getClass.getName}]"
                ))))
        }.recover {

            case TonClientException(code, message) =>
                BadRequest(Json.toJson(ResponseError(
                    code = code,
                    message = message
                )))

            case t: Throwable =>
                Logger.error("Client request error", t)
                InternalServerError(Json.toJson(ResponseError(
                    code = -1000,
                    message = s"Get an error on send request [${t.getMessage}]"
                )))
        }
    }

    def accountStatus: Action[AccountStatusRequest] = Action.async(parse.json[AccountStatusRequest]) { r =>

        client.send(new TonApi.GenericGetAccountState(
            new TonApi.AccountAddress(r.body.address)
        )).map {

            case uninitedAccount: TonApi.GenericAccountStateUninited =>
                Ok(Json.toJson(AccountStatusResponse(
                    balance = uninitedAccount.accountState.balance,
                    timestamp = uninitedAccount.accountState.syncUtime,
                    accountType = AccountAddressTypes.Uninited,
                    lastTransaction = Option(uninitedAccount.accountState.lastTransactionId).map { tx =>
                        AccountTransaction(
                            hash = AccountTransaction.convertBytesToHex(tx.hash),
                            lt = tx.lt
                        )
                    }
                )))

            case testWallet: TonApi.GenericAccountStateTestWallet =>
                Ok(Json.toJson(AccountStatusResponse(
                    balance = testWallet.accountState.balance,
                    timestamp = testWallet.accountState.syncUtime,
                    accountType = AccountAddressTypes.TestWallet,
                    sequence = Some(testWallet.accountState.seqno),
                    lastTransaction = Option(testWallet.accountState.lastTransactionId).map { tx =>
                        AccountTransaction(
                            hash = AccountTransaction.convertBytesToHex(tx.hash),
                            lt = tx.lt
                        )
                    }
                )))

            case wallet: TonApi.GenericAccountStateWallet =>
                Ok(Json.toJson(AccountStatusResponse(
                    balance = wallet.accountState.balance,
                    timestamp = wallet.accountState.syncUtime,
                    accountType = AccountAddressTypes.Wallet,
                    sequence = Some(wallet.accountState.seqno),
                    lastTransaction = Option(wallet.accountState.lastTransactionId).map { tx =>
                        AccountTransaction(
                            hash = AccountTransaction.convertBytesToHex(tx.hash),
                            lt = tx.lt
                        )
                    }
                )))

            case giver: TonApi.GenericAccountStateTestGiver =>
                Ok(Json.toJson(AccountStatusResponse(
                    balance = giver.accountState.balance,
                    timestamp = giver.accountState.syncUtime,
                    accountType = AccountAddressTypes.Giver,
                    sequence = Some(giver.accountState.seqno),
                    lastTransaction = Option(giver.accountState.lastTransactionId).map { tx =>
                        AccountTransaction(
                            hash = AccountTransaction.convertBytesToHex(tx.hash),
                            lt = tx.lt
                        )
                    }
                )))

            case raw: TonApi.GenericAccountStateRaw =>
                Ok(Json.toJson(AccountStatusResponse(
                    balance = raw.accountState.balance,
                    timestamp = raw.accountState.syncUtime,
                    accountType = AccountAddressTypes.Raw,
                    lastTransaction = Option(raw.accountState.lastTransactionId).map { tx =>
                        AccountTransaction(
                            hash = AccountTransaction.convertBytesToHex(tx.hash),
                            lt = tx.lt
                        )
                    }
                )))

            case other =>
                BadRequest(Json.toJson(ResponseError(
                    code = -100,
                    message = s"Unknown response class [${other.getClass.getName}]"
                )))
        }.recover {

            case TonClientException(code, message) =>
                BadRequest(Json.toJson(ResponseError(
                    code = code,
                    message = message
                )))

            case t: Throwable =>
                Logger.error("Client request error", t)
                InternalServerError(Json.toJson(ResponseError(
                    code = -1000,
                    message = s"Get an error on send request [${t.getMessage}]"
                )))
        }
    }

    def importKey: Action[ImportKeyRequest] = Action.async(parse.json[ImportKeyRequest]) { r =>

        client.send(new TonApi.ImportKey(
            null,
            null,
            new TonApi.ExportedKey(
                Array(r.body.seed:_*)
            )
        )).map {

            case key: TonApi.Key =>
                Ok(Json.toJson(ImportKeyResponse(
                    publicKey = key.publicKey,
                    password = BinaryData(key.secret)
                )))

            case other =>
                BadRequest(Json.toJson(ResponseError(
                    code = -100,
                    message = s"Unknown response class [${other.getClass.getName}]"
                )))
        }.recover {

            case TonClientException(code, message) =>
                BadRequest(Json.toJson(ResponseError(
                    code = code,
                    message = message
                )))

            case t: Throwable =>
                Logger.error("Client request error", t)
                InternalServerError(Json.toJson(ResponseError(
                    code = -1000,
                    message = s"Get an error on send request [${t.getMessage}]"
                )))
        }
    }

    def sendGrams: Action[SendGramsRequest] = Action.async(parse.json[SendGramsRequest]) { r =>

        val gramRequest = r.body

        if(r.body.amount <= 0L) {
            Future.successful(BadRequest(Json.toJson(ResponseError(
                code = -101,
                message = s"Amount value must be greater then zero"
            ))))
        } else if (r.body.sourceAddress == r.body.destinationAddress) {
            Future.successful(BadRequest(Json.toJson(ResponseError(
                code = -102,
                message = s"Source and destination addresses are equals"
            ))))
        } else {
            val sourceKey = new TonApi.InputKey(
                new TonApi.Key(
                    gramRequest.sourceKey.publicKey,
                    gramRequest.sourceKey.password.bytes
                ),
                null
            )

            client.send(new TonApi.GenericSendGrams(
                sourceKey,
                new TonApi.AccountAddress(gramRequest.sourceAddress),
                new TonApi.AccountAddress(gramRequest.destinationAddress),
                gramRequest.amount,
                60,
                true,
                null
            )).map {

                case result: TonApi.SendGramsResult =>
                    Ok(Json.toJson(SendGramResponse(
                        bodyHash = AccountTransaction.convertBytesToHex(result.bodyHash),
                        sentUntil = result.sentUntil
                    )))

                case other =>
                    BadRequest(Json.toJson(ResponseError(
                        code = -100,
                        message = s"Unknown response class [${other.getClass.getName}]"
                    )))
            }.recover {

                case TonClientException(code, message) =>
                    BadRequest(Json.toJson(ResponseError(
                        code = code,
                        message = message
                    )))

                case t: Throwable =>
                    Logger.error("Client request error", t)
                    InternalServerError(Json.toJson(ResponseError(
                        code = -1000,
                        message = s"Get an error on send request [${t.getMessage}]"
                    )))
            }
        }
    }

    def initAccount: Action[InitAccountRequest] = Action.async(parse.json[InitAccountRequest]) { r =>

        val initRequest = r.body

        val inputKey = new TonApi.Key(
            initRequest.accountKey.publicKey,
            initRequest.accountKey.password.bytes
        )

        val initRequestFuture = initRequest.accountType match {

            case AccountAddressTypes.TestWallet =>
                client.send(new TonApi.TestWalletInit(new TonApi.InputKey(
                    inputKey,
                    null
                )))

            case AccountAddressTypes.Wallet =>
                client.send(new TonApi.WalletInit(new TonApi.InputKey(
                    inputKey,
                    null
                )))

            case other =>
                Future.failed(throw new Exception(s"Unsupported account address type [$other]"))

        }

        initRequestFuture.map {

            case _: TonApi.Ok =>
                Ok(Json.toJson(InitAccountResponse("OK")))

            case other =>
                BadRequest(Json.toJson(ResponseError(
                    code = -100,
                    message = s"Unknown response class [${other.getClass.getName}]"
                )))
        }.recover {

            case TonClientException(code, message) =>
                BadRequest(Json.toJson(ResponseError(
                    code = code,
                    message = message
                )))

            case t: Throwable =>
                Logger.error("Client request error", t)
                InternalServerError(Json.toJson(ResponseError(
                    code = -1000,
                    message = s"Get an error on send request [${t.getMessage}]"
                )))
        }
    }

    def transactionHistory: Action[TransactionHistoryRequest] = Action.async(parse.json[TransactionHistoryRequest]) { r =>

        client.send(new TonApi.RawGetTransactions(
            new TonApi.AccountAddress(
                r.body.address
            ),
            new TonApi.InternalTransactionId(
                r.body.lastTransaction.lt,
                AccountTransaction.convertHexToBytes(r.body.lastTransaction.hash)
            )
        )).map {

            case transactions: TonApi.RawTransactions =>
                val historyTransactions = transactions.transactions.map { transaction =>
                    TransactionHistoryTransaction(
                        fee = transaction.fee,
                        storageFee = transaction.storageFee,
                        otherFee = transaction.otherFee,
                        timestamp = transaction.utime,
                        data = if(r.body.withData) { Option(transaction.data).map(d => BinaryData(d)) } else { None },
                        transactionId = AccountTransaction(
                            hash = AccountTransaction.convertBytesToHex(transaction.transactionId.hash),
                            lt = transaction.transactionId.lt
                        ),
                        inMessage = Option(transaction.inMsg).map(TransactionHistoryMessage(_, r.body.withData)),
                        outMessages = Option(transaction.outMsgs).map(_.map(TransactionHistoryMessage(_, r.body.withData)))
                    )
                }
                val response = TransactionHistoryResponse(
                    previousTransaction = AccountTransaction(
                        hash = AccountTransaction.convertBytesToHex(transactions.previousTransactionId.hash),
                        lt = transactions.previousTransactionId.lt
                    ),
                    transactions = historyTransactions
                )

                Ok(Json.toJson(response))

            case other =>
                BadRequest(Json.toJson(ResponseError(
                    code = -100,
                    message = s"Unknown response class [${other.getClass.getName}]"
                )))
        }.recover {

            case TonClientException(code, message) =>
                BadRequest(Json.toJson(ResponseError(
                    code = code,
                    message = message
                )))

            case t: Throwable =>
                Logger.error("Client request error", t)
                InternalServerError(Json.toJson(ResponseError(
                    code = -1000,
                    message = s"Get an error on send request [${t.getMessage}]"
                )))
        }
    }
}
