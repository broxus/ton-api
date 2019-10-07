# TON API

## Overview
This is a simple wallet http api for telegram open network client. Base on original [native-lib.cpp](https://github.com/ton-blockchain/ton/blob/master/example/android/native-lib.cpp), [Play Framework](https://github.com/playframework/playframework) and [TON client java wrapper](https://github.com/broxus/ton-client).

## Supported OS

* <b>Linux</b> - build on Ubuntu 18.04 LTS and Java 8.

* <b>MacOS</b> - build on Sierra and Java 8.

Supported features
-----

* Keys, create, import and export as seed phrase.

* Accounts, create wallet, send grams, get statuses.

* Transactions, account transaction history, messages and binary data.

Docker
----
You can use all in one docker to run api in your environment. [Dockerfile](Dockerfile) contains multistage build (min docker version 17.05).
Builder container install sbt and compile api in stage mode. Then pre-build api copy to runtime container based on openjdk:8-jre-slim docker image.

```bash
# build ton-api docker image
docker build --tag ton-api -f Dockerfile .

# run ton-api docker
docker run --rm -ti --name ton-api -p 9000:9000 --mount type=bind,source="$(pwd)/keystore",target=/app/keystore ton-api -J-Xmx2G -J-Xms2G

# run ton-api docker with configuration override
docker run --rm -ti --name ton-api -p 9000:9000 --mount type=bind,source="$(pwd)/keystore",target=/app/keystore -v $(pwd)/conf/ton.conf:/app/conf/ton.conf ton-api -J-Xmx2G -J-Xms2G

# stop ton-api docker
docker kill ton-api
```

Configuration
----

Example configuration [ton.conf](conf/ton.conf)

```hocon

ton {
  
  // https://test.ton.org/ton-lite-client-test1.config.json
  lite-client = """{
      "liteservers": [
        {
          "ip": 1137658550,
          "port": 4924,
          "id": {
            "@type": "pub.ed25519",
            "key": "peJTw/arlRfssgTuf9BMypJzqOi7SXEqSPSWiEw2U1M="
          }
        }
      ],
      "validator": {
        "@type": "validator.config.global",
        "zero_state": {
          "workchain": -1,
          "shard": -9223372036854775808,
          "seqno": 0,
          "root_hash": "VCSXxDHhTALFxReyTZRd8E4Ya3ySOmpOWAS4rBX9XBY=",
          "file_hash": "eh9yveSz1qMdJ7mOsO+I+H77jkLr9NpAuEkoJuseXBo="
        }
      }
    }"""
    
  // keystore path
  keystore = "keystore"
  
  use-network-callback = false
  verbosity-level = 0
}
```

Swagger
----
Source swagger schema example can be accessible [here](public/swagger_ton_api.yaml). For interactive example run application using docker or sbt and open [http://localhost:9000/](http://localhost:9000)

Examples
----

<b>Create new key</b>

```bash
curl -X POST -H "Content-Type: application/json" -d '{}' http://localhost:9000/ton/v1/createNewKey
```

```json
{
  "publicKey":"PuZSaVYlFFB6Or92_EmNwgm2t4pem62k9s3WDuC9H3QeF1Ue",
  "password":"o+rEXFnd9dDrEiYza5qVIhyX40W2I96KA1ojUZC6YJs=",
  "seed":["assist","crunch","parade","entry","cost","random","pizza","organ","maximum","beauty","wait","tent","buyer","mom","erosion","media","reward","barely","fitness","skill","pave","zoo","sight","moral"]
}
```

<br>

<b>Request account address</b> (account types supported: wallet and testWallet)

```bash
curl -X POST -H "Content-Type: application/json" -d '{"publicKey":"PuZSaVYlFFB6Or92_EmNwgm2t4pem62k9s3WDuC9H3QeF1Ue", "accountType": "testWallet"}' http://localhost:9000/ton/v1/accountAddress
```

```json
{
  "address":"EQALQWtEElfqoIP2ftiiugUmQ5J4JDERedFLyvXhjHGJK2xl",
  "unpacked":{
    "address":"0B416B441257EAA083F67ED8A2BA052643927824311179D14BCAF5E18C71892B",
    "workchainId":0,
    "bounceable":true,
    "testnet":false
  }
}
```

<br>

<b>Request account status</b> (balance, last transaction, seqno and state)

```bash
curl -X POST -H "Content-Type: application/json" -d '{"address": "EQALQWtEElfqoIP2ftiiugUmQ5J4JDERedFLyvXhjHGJK2xl"}' http://localhost:9000/ton/v1/accountStatus
```

```json
{
  "balance":-1,
  "timestamp":1570035498,
  "accountType":"uninited",
  "lastTransaction":{
    "hash":"0000000000000000000000000000000000000000000000000000000000000000",
    "lt":0
  }
}
```

<br>

<b>Init account</b> (create wallet, account types supported: wallet and testWallet)

```bash
curl -X POST -H "Content-Type: application/json" -d '{"accountKey": {"publicKey":"PuZSaVYlFFB6Or92_EmNwgm2t4pem62k9s3WDuC9H3QeF1Ue","password":"o+rEXFnd9dDrEiYza5qVIhyX40W2I96KA1ojUZC6YJs="}, "accountType": "testWallet"}' http://localhost:9000/ton/v1/initAccount
```

```json
{"status": "OK"}
```

<br>

<b>Send grams</b>

```bash
curl -X POST -H "Content-Type: application/json" -d '{ "sourceKey": {"publicKey":"PuZSaVYlFFB6Or92_EmNwgm2t4pem62k9s3WDuC9H3QeF1Ue","password":"o+rEXFnd9dDrEiYza5qVIhyX40W2I96KA1ojUZC6YJs="}, "sourceAccountType": "testWallet", "sourceSequence": 1, "destinationAddress": "EQDVdEsJ6mgOaYToip2Q_xBdnCHDxj0Ypqt3oPCU-Hmv1kX4", "amount": 1000000000 }' http://localhost:9000/ton/v1/sendGrams
```

```json
{"status": "OK"}
```

<br>

<b>Import key</b>

```bash
curl -X POST -H "Content-Type: application/json" -d '{ "seed": "seed":["assist","crunch","parade","entry","cost","random","pizza","organ","maximum","beauty","wait","tent","buyer","mom","erosion","media","reward","barely","fitness","skill","pave","zoo","sight","moral"] }' http://localhost:9000/ton/v1/importKey
```

```json
{
  "publicKey":"PuZSaVYlFFB6Or92_EmNwgm2t4pem62k9s3WDuC9H3QeF1Ue",
  "password":"o+rEXFnd9dDrEiYza5qVIhyX40W2I96KA1ojUZC6YJs="
}
```

<br>

<b>Transaction history</b>

```bash
curl -X POST -H "Content-Type: application/json" -d '{ "address": "EQALQWtEElfqoIP2ftiiugUmQ5J4JDERedFLyvXhjHGJK2xl", "lastTransaction":{"hash":"0000000000000000000000000000000000000000000000000000000000000000","lt":0}, "withData": false }' http://localhost:9000/ton/v1/transactionHistory
```

```json
{
  "previousTransaction":{
    "hash":"0000000000000000000000000000000000000000000000000000000000000000",
    "lt":0
  },
  "transactions":[]
}
```

## License

```
Copyright 2019 FINEX FUTURE LTD

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at [apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
