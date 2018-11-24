package com.sappenin.interledger.ilpv4.connector.logging;

/**
 * A class for assisting with the emission of BTP and ILP errors.
 */
public class IlpLogUtils {

//  private final CodecContext btpCodecContext;
//  private final CodecContext ilpCodecContext;
//
//  public IlpLogUtils(final CodecContext btpCodecContext, final CodecContext ilpCodecContext) {
//    this.btpCodecContext = Objects.requireNonNull(btpCodecContext);
//    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
//  }
//
//  /**
//   * Emit a BtpPacket for logging purposes by unwrapping all of its data paylaods.
//   *
//   * @param logger
//   * @param btpPacket
//   */
//  public void emit(final Logger logger, final Level level, final BtpPacket... btpPacket) {
//    this.emit(logger, level, null, btpPacket);
//  }
//
//  /**
//   * Emit a BtpPacket for logging purposes by unwrapping all of its data paylaods.
//   *
//   * @param logger
//   * @param btpPacket
//   */
//  public void emit(final Logger logger, final Level level, String message, Object... objects) {
//
//
//    // If any of the objects are BtpPackets, then we need to unwrap their data...
//    if()
//
//
//    // Unwrap the subprotocol data...
//    btpPacket.getSubProtocols().stream().forEach((btpSubProtocol -> {
//      try {
//        switch (btpSubProtocol.getContentType()) {
//          case MIME_TEXT_PLAIN_UTF8: {
//            logger.error("Unhandled BtpError ContentType: " + MIME_TEXT_PLAIN_UTF8);
//            break;
//          }
//
//          case MIME_APPLICATION_JSON: {
//            logger.error("Unhandled BtpError ContentType: " + MIME_APPLICATION_JSON);
//            break;
//          }
//
//          case MIME_APPLICATION_OCTET_STREAM:
//          default: {
//            if (BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_ILP.equals(btpSubProtocol.getProtocolName())) {
//              final ByteArrayInputStream bais = new ByteArrayInputStream(btpSubProtocol.getData());
//              final InterledgerPacket interledgerPacket = ilpCodecContext.read(InterledgerPacket.class, bais);
//              objectList.add(interledgerPacket);
//            } else {
//              logger.error("Unhandled BtpError Subprotocol Type: " + btpSubProtocol.getProtocolName());
//            }
//            break;
//          }
//        }
//      } catch (IOException e) {
//        throw new RuntimeException(e);
//      }
//    }));
//
//    emit(logger, level, message, objectList.toArray());
//
//  }
//
//  private void emit(final Logger logger, Level level, String message, Object... objects) {
//    Objects.requireNonNull(logger);
//    Objects.requireNonNull(level);
//
//    switch (level.toInt()) {
//      case EventConstants.WARN_INT: {
//        logger.warn(message, objects);
//      }
//      case EventConstants.ERROR_INT: {
//        logger.error(message, objects);
//      }
//      case EventConstants.INFO_INT: {
//        logger.info(message, objects);
//      }
//      default:
//      case EventConstants.DEBUG_INT: {
//        logger.debug(message, objects);
//      }
//    }
//  }
}