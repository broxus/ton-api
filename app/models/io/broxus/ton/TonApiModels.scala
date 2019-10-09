package models.io.broxus.ton

import io.broxus.ton.TonApi
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{Format, Json, Reads, Writes}

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

case class SendGramsRequest(sourceKey: KeyWithPassword,
                            sourceAddress: String,
                            destinationAddress: String,
                            amount: Long)

object SendGramsRequest {

    implicit val format: Format[SendGramsRequest] = Json.format[SendGramsRequest]
}

case class SendGramResponse(bodyHash: String, sentUntil: Long)

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

case class ResponseError(code: Int, message: String)

object ResponseError {

    implicit val format: Format[ResponseError] = Json.format[ResponseError]
}