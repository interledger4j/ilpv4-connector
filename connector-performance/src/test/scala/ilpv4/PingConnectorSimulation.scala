package ilpv4

import java.io.ByteArrayInputStream

import com.google.common.io.BaseEncoding
import io.gatling.core.Predef._
import io.gatling.core.body.ByteArrayBody
import io.gatling.http.Predef._
import org.interledger.codecs.ilp.InterledgerCodecContextFactory
import io.gatling.http.response
import org.interledger.core.{InterledgerFulfillPacket, InterledgerPreparePacket, InterledgerResponsePacket}
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

/**
 * This test pings a Connector using an ingress account, asserting a fulfill response.
 */
class PingConnectorSimulation extends Simulation {

  val logger = LoggerFactory.getLogger(classOf[PingConnectorSimulation])

  ////////////
  // Variables
  ////////////

  // The HTTP URL of the Connector being tested.
  val connectorUrl = "https://jc.ilpv4.dev"
  // The account that all incoming packets ingress into the Connector on.
  val ingressAccount = "lt-ingress"

  val httpConf = http.baseUrl(connectorUrl)
  val ping = scenario("Ping Connector").exec(
    http("Ping")
      .post("/accounts/lt-ingress/ilp")
      .header("accept", "application/octet-stream")
      .header("content-type", "application/octet-stream")
      .header("Authorization", "Bearer shh")
      .body(ByteArrayBody(BaseEncoding.base16()
        .decode("0C520000000000000001323032313031313130303531323936353766687AADF862BD776C8FC18B8E9F8E20089714856EE233B3902A591D0D5F292517746573742E6A632E6C742D6C622D66756C66696C6C657200")))
      .check(status.is(200))
      .check(bodyBytes.exists)
      .check(bodyBytes.transform((byteArray, session) => {
        val context = InterledgerCodecContextFactory.oer()
        val bas = new ByteArrayInputStream(byteArray)
        val response:InterledgerFulfillPacket  = context.read(classOf[InterledgerFulfillPacket], bas)
        response
      })
        .saveAs("lastResponse"))
  )
    .exec(session=>{
      val packet:InterledgerFulfillPacket = session("lastResponse")
      //Analyse theResponse...

      check(packet.getFulfillment().equals(null)).isTrue()

      //... and make sure to return the session
      session
    })

  //val inputStream = new ByteArrayInputStream(bodyBytes.)


//val packet =


  logger.info("Body: " + bodyBytes);

  setUp(ping.inject(constantUsersPerSec(1) during (1 seconds))
    .protocols(httpConf))

  //    .assertions(
  //      global.successfulRequests.percent.is(100),
  //      global.responseTime.max.lt(1000),
  //      global.responseTime.mean.lt(750),
  //      global.requestsPerSec.lt(10)
  //    )
}
