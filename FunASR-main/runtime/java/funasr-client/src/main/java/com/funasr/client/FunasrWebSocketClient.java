package com.funasr.client;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FunasrWebSocketClient extends WebSocketClient {
  private static final Logger logger = LoggerFactory.getLogger(FunasrWebSocketClient.class);

  private final FunasrClientConfig config;
  private final FunasrMessageListener listener;
  private final CountDownLatch openLatch = new CountDownLatch(1);
  private final CountDownLatch closeLatch = new CountDownLatch(1);
  private final AtomicBoolean eofSent = new AtomicBoolean(false);
  private final JSONParser jsonParser = new JSONParser();

  FunasrWebSocketClient(URI serverUri, FunasrClientConfig config, FunasrMessageListener listener) {
    super(serverUri);
    this.config = Objects.requireNonNull(config, "config");
    this.listener = listener == null ? new FunasrMessageListener() {} : listener;
  }

  boolean awaitOpen(Duration timeout) throws InterruptedException {
    return openLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
  }

  boolean awaitClose(Duration timeout) throws InterruptedException {
    return closeLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
  }

  void markEofSent() {
    eofSent.set(true);
  }

  @Override
  public void onOpen(ServerHandshake handshakedata) {
    openLatch.countDown();
    listener.onOpen();
  }

  @Override
  public void onMessage(String message) {
    listener.onMessage(message);
    if (config.isOffline() && eofSent.get()) {
      JSONObject jsonObject = parseJson(message);
      if (shouldCloseOffline(jsonObject)) {
        close();
      }
    }
  }

  @Override
  public void onClose(int code, String reason, boolean remote) {
    closeLatch.countDown();
    listener.onClosed(code, reason, remote);
  }

  @Override
  public void onError(Exception ex) {
    listener.onError(ex);
    logger.debug("WebSocket error", ex);
  }

  private JSONObject parseJson(String message) {
    try {
      return (JSONObject) jsonParser.parse(message);
    } catch (ParseException ex) {
      logger.debug("Failed to parse message: {}", message, ex);
      return null;
    }
  }

  private boolean shouldCloseOffline(JSONObject jsonObject) {
    if (jsonObject == null) {
      return false;
    }
    Object isFinal = jsonObject.get("is_final");
    if (isFinal == null) {
      return true;
    }
    if (isFinal instanceof Boolean) {
      return (Boolean) isFinal;
    }
    if (isFinal instanceof String) {
      String value = ((String) isFinal).trim().toLowerCase();
      if ("true".equals(value)) {
        return true;
      }
      if ("false".equals(value)) {
        return config.legacyCloseOnFalse();
      }
    }
    return false;
  }
}
