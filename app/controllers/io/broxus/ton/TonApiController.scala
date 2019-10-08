package controllers.io.broxus.ton

import com.google.inject.{Inject, Singleton}
import io.broxus.ton.TonApi
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{Format, Json, Reads, Writes}
import play.api.mvc._
import services.io.broxus.ton.TonClientGlobal

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
        )).flatMap { case key: TonApi.Key =>
            client.send(new TonApi.ExportKey(new TonApi.InputKey(
                key,
                null
            ))).map { case exportedKey: TonApi.ExportedKey =>
                Ok(Json.toJson(CreateNewKeyResponse(
                    publicKey = key.publicKey,
                    password = BinaryData(key.secret),
                    seed = exportedKey.wordList
                )))
            }
        }
    }

    def accountAddress: Action[AccountAddressRequest] = Action.async(parse.json[AccountAddressRequest]) { r =>

        val addressRequest = r.body

        addressRequest.accountType match {

            case AccountAddressTypes.TestWallet =>
                client.send(new TonApi.TestWalletGetAccountAddress(new TonApi.TestWalletInitialAccountState(
                    r.body.publicKey
                ))).flatMap { case address: TonApi.AccountAddress =>
                    client.send(new TonApi.UnpackAccountAddress(address.accountAddress)).flatMap { case unpacked: TonApi.UnpackedAccountAddress =>
                        client.send(new TonApi.PackAccountAddress(new TonApi.UnpackedAccountAddress(
                            unpacked.workchainId,
                            false,
                            unpacked.testnet,
                            unpacked.addr
                        ))).map { case initAddress: TonApi.AccountAddress =>
                            Ok(Json.toJson(AccountAddressResponse(
                                address = address.accountAddress,
                                initAddress = initAddress.accountAddress,
                                unpacked = UnpackedAccountAddress(unpacked)
                            )))
                        }
                    }
                }

            case AccountAddressTypes.Wallet =>
                client.send(new TonApi.WalletGetAccountAddress(new TonApi.WalletInitialAccountState(
                    r.body.publicKey
                ))).flatMap { case address: TonApi.AccountAddress =>
                    client.send(new TonApi.UnpackAccountAddress(address.accountAddress)).flatMap { case unpacked: TonApi.UnpackedAccountAddress =>
                        client.send(new TonApi.PackAccountAddress(new TonApi.UnpackedAccountAddress(
                            unpacked.workchainId,
                            false,
                            unpacked.testnet,
                            unpacked.addr
                        ))).map { case initAddress: TonApi.AccountAddress =>
                            Ok(Json.toJson(AccountAddressResponse(
                                address = address.accountAddress,
                                initAddress = initAddress.accountAddress,
                                unpacked = UnpackedAccountAddress(unpacked)
                            )))
                        }
                    }
                }

            case other => Future.successful(BadRequest)
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
        }
    }

    def importKey: Action[ImportKeyRequest] = Action.async(parse.json[ImportKeyRequest]) { r =>

        client.send(new TonApi.ImportKey(
            null,
            null,
            new TonApi.ExportedKey(
                Array(r.body.seed:_*)
            )
        )).map { case key: TonApi.Key =>
            Ok(Json.toJson(ImportKeyResponse(
                publicKey = key.publicKey,
                password = BinaryData(key.secret)
            )))
        }
    }

    def sendGrams: Action[SendGramsRequest] = Action.async(parse.json[SendGramsRequest]) { r =>

        val gramRequest = r.body

        gramRequest.sourceAccountType match {

            case AccountAddressTypes.TestWallet =>
                gramRequest.sourceKey.map { key =>
                    client.send(new TonApi.TestWalletSendGrams(
                        new TonApi.InputKey(
                            new TonApi.Key(
                                key.publicKey,
                                key.password.bytes
                            ),
                            null
                        ),
                        new TonApi.AccountAddress(gramRequest.destinationAddress),
                        gramRequest.sourceSequence,
                        gramRequest.amount,
                        null
                    )).map { case result: TonApi.SendGramsResult =>
                        Ok(Json.toJson(SendGramResponse(
                            hash = AccountTransaction.convertBytesToHex(result.bodyHash),
                            sentUntil = result.sentUntil
                        )))
                    }
                } getOrElse {
                    Future.successful(BadRequest)
                }

            case AccountAddressTypes.Wallet =>
                gramRequest.sourceKey.map { key =>
                    client.send(new TonApi.WalletSendGrams(
                        new TonApi.InputKey(
                            new TonApi.Key(
                                key.publicKey,
                                key.password.bytes
                            ),
                            null
                        ),
                        new TonApi.AccountAddress(gramRequest.destinationAddress),
                        gramRequest.sourceSequence,
                        System.currentTimeMillis() / 1000L + 5L * 3600L,
                        gramRequest.amount,
                        null
                    )).map { case result: TonApi.SendGramsResult =>
                        Ok(Json.toJson(SendGramResponse(
                            hash = AccountTransaction.convertBytesToHex(result.bodyHash),
                            sentUntil = result.sentUntil
                        )))
                    }
                } getOrElse {
                    Future.successful(BadRequest)
                }

            case AccountAddressTypes.Giver =>
                client.send(new TonApi.TestGiverSendGrams(
                    new TonApi.AccountAddress(gramRequest.destinationAddress),
                    gramRequest.sourceSequence,
                    gramRequest.amount,
                    null
                )).map { case result: TonApi.SendGramsResult =>
                    Ok(Json.toJson(SendGramResponse(
                        hash = AccountTransaction.convertBytesToHex(result.bodyHash),
                        sentUntil = result.sentUntil
                    )))
                }

            case other => Future.successful(BadRequest)

        }
    }

    def initAccount: Action[InitAccountRequest] = Action.async(parse.json[InitAccountRequest]) { r =>

        val initRequest = r.body

        initRequest.accountType match {

            case AccountAddressTypes.TestWallet =>
                client.send(new TonApi.TestWalletInit(new TonApi.InputKey(
                    new TonApi.Key(
                        initRequest.accountKey.publicKey,
                        initRequest.accountKey.password.bytes
                    ),
                    null
                ))).map { case _: TonApi.Ok =>
                    Ok(Json.toJson(InitAccountResponse("OK")))
                }

            case AccountAddressTypes.Wallet =>
                client.send(new TonApi.WalletInit(new TonApi.InputKey(
                    new TonApi.Key(
                        initRequest.accountKey.publicKey,
                        initRequest.accountKey.password.bytes
                    ),
                    null
                ))).map { case _: TonApi.Ok =>
                    Ok(Json.toJson(InitAccountResponse("OK")))
                }

            case other => Future.successful(BadRequest)
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
        )).map { case transactions: TonApi.RawTransactions =>
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
        }
    }

}

case object AccountAddressTypes {

    val Uninited = "uninited"
    val TestWallet = "testWallet"
    val Wallet = "wallet"
    val Giver = "giver"
}

case class BinaryData(bytes: Array[Byte])

object BinaryData {

    implicit val format: Format[BinaryData] = Format[BinaryData](
        Reads.of[String].map(s => BinaryData(Base64.decodeBase64(s))),
        Writes.of[BinaryData](a => Writes.of[String].writes(Base64.encodeBase64String(a.bytes)))
    )
}

case class CreateNewKeyResponse(publicKey: String, password: BinaryData, seed: Seq[String])

object CreateNewKeyResponse {

    implicit val format: Format[CreateNewKeyResponse] = Json.format[CreateNewKeyResponse]
}

case class AccountAddressRequest(publicKey: String, accountType: String)

object AccountAddressRequest {

    implicit val format: Format[AccountAddressRequest] = Json.format[AccountAddressRequest]
}

case class UnpackedAccountAddress(address: String,
                                  workchainId: Int,
                                  bounceable: Boolean,
                                  testnet: Boolean)

object UnpackedAccountAddress {

    def apply(unpacked: TonApi.UnpackedAccountAddress): UnpackedAccountAddress =
        new UnpackedAccountAddress(
            address = AccountTransaction.convertBytesToHex(unpacked.addr),
            workchainId = unpacked.workchainId,
            bounceable = unpacked.bounceable,
            testnet = unpacked.testnet
        )

    implicit val format: Format[UnpackedAccountAddress] = Json.format[UnpackedAccountAddress]
}

case class AccountAddressResponse(address: String,
                                  initAddress: String,
                                  unpacked: UnpackedAccountAddress)

object AccountAddressResponse {

    implicit val format: Format[AccountAddressResponse] = Json.format[AccountAddressResponse]
}

case class AccountStatusRequest(address: String)

object AccountStatusRequest {

    implicit val format: Format[AccountStatusRequest] = Json.format[AccountStatusRequest]
}

case class AccountTransaction(hash: String, lt: Long)

object AccountTransaction {

    //noinspection ScalaMalformedFormatString
    def convertBytesToHex(bytes: Array[Byte]): String = {
        val sb = new StringBuilder
        for (b <- bytes) {
            sb.append(String.format("%02x", Byte.box(b)))
        }
        sb.toString.toUpperCase
    }

    def convertHexToBytes(hex: String): Array[Byte] = {
        hex.sliding(2,2).toArray.map(Integer.parseInt(_, 16).toByte)
    }

    implicit val format: Format[AccountTransaction] = Json.format[AccountTransaction]
}

case class AccountStatusResponse(balance: Long,
                                 timestamp: Long,
                                 accountType: String,
                                 sequence: Option[Int] = None,
                                 lastTransaction: Option[AccountTransaction] = None)

object AccountStatusResponse {

    implicit val format: Format[AccountStatusResponse] = Json.format[AccountStatusResponse]
}

case class KeyWithPassword(publicKey: String, password: BinaryData)

object KeyWithPassword {

    implicit val format: Format[KeyWithPassword] = Json.format[KeyWithPassword]
}

case class ImportKeyRequest(seed: Seq[String])

object ImportKeyRequest {

    implicit val format: Format[ImportKeyRequest] = Json.format[ImportKeyRequest]
}

case class ImportKeyResponse(publicKey: String, password: BinaryData)

object ImportKeyResponse {

    implicit val format: Format[ImportKeyResponse] = Json.format[ImportKeyResponse]
}

case class SendGramsRequest(sourceAccountType: String,
                            sourceKey: Option[KeyWithPassword] = None,
                            sourceSequence: Int,
                            destinationAddress: String,
                            amount: Long)

object SendGramsRequest {

    implicit val format: Format[SendGramsRequest] = Json.format[SendGramsRequest]
}

case class SendGramResponse(hash: String, sentUntil: Long)

object SendGramResponse {

    implicit val format: Format[SendGramResponse] = Json.format[SendGramResponse]
}

case class InitAccountRequest(accountKey: KeyWithPassword,
                              accountType: String)

object InitAccountRequest {

    implicit val format: Format[InitAccountRequest] = Json.format[InitAccountRequest]
}

case class InitAccountResponse(status: String)

object InitAccountResponse {

    implicit val format: Format[InitAccountResponse] = Json.format[InitAccountResponse]
}

case class TransactionHistoryRequest(address: String, lastTransaction: AccountTransaction, withData: Boolean)

object TransactionHistoryRequest {

    implicit val format: Format[TransactionHistoryRequest] = Json.format[TransactionHistoryRequest]
}

case class TransactionHistoryMessage(source: String,
                                     destination: String,
                                     fwdFee: Long,
                                     ihrFee: Long,
                                     bodyHash: String,
                                     createdLt: Long,
                                     message: Option[BinaryData],
                                     value: Long)

object TransactionHistoryMessage {

    implicit val format: Format[TransactionHistoryMessage] = Json.format[TransactionHistoryMessage]

    def apply(message: TonApi.RawMessage, withData: Boolean): TransactionHistoryMessage = {
        TransactionHistoryMessage(
            source = message.source,
            destination = message.destination,
            fwdFee = message.fwdFee,
            ihrFee = message.ihrFee,
            bodyHash = AccountTransaction.convertBytesToHex(message.bodyHash),
            createdLt = message.createdLt,
            message = if (withData) { Option(message.message).map(d => BinaryData(d)) } else None,
            value = message.value
        )
    }
}

case class TransactionHistoryTransaction(fee: Long,
                                         storageFee: Long,
                                         otherFee: Long,
                                         timestamp: Long,
                                         data: Option[BinaryData],
                                         transactionId: AccountTransaction,
                                         inMessage: Option[TransactionHistoryMessage],
                                         outMessages: Option[Seq[TransactionHistoryMessage]])

object TransactionHistoryTransaction {

    implicit val format: Format[TransactionHistoryTransaction] = Json.format[TransactionHistoryTransaction]
}

case class TransactionHistoryResponse(previousTransaction: AccountTransaction,
                                      transactions: Seq[TransactionHistoryTransaction])

object TransactionHistoryResponse {

    implicit val format: Format[TransactionHistoryResponse] = Json.format[TransactionHistoryResponse]
}
