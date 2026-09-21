package com.funasr.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FunasrClient implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(FunasrClient.class);
  private static final int WAV_HEADER_BYTES = 44;

  private final FunasrClientConfig config;
  private final FunasrWebSocketClient wsClient;

  public FunasrClient(FunasrClientConfig config, FunasrMessageListener listener) {
    this.config = Objects.requireNonNull(config, "config");
    this.wsClient = new FunasrWebSocketClient(config.toWebsocketUri(), config, listener);
  }

  public FunasrClientConfig config() {
    return config;
  }

  public void connect() throws InterruptedException, TimeoutException {
    wsClient.connect();
    boolean opened = wsClient.awaitOpen(config.connectTimeout());
    if (!opened) {
      wsClient.close();
      throw new TimeoutException("Timeout waiting for WebSocket open: " + config.toWebsocketUri());
    }
  }

  public void streamFile(Path audioPath) throws IOException, InterruptedException {
    String extension = getExtension(audioPath);
    String wavFormat = extension.equals("wav") ? "pcm" : extension;
    String wavName = config.wavName().isBlank() ? audioPath.getFileName().toString() : config.wavName();

    startStream(wavName, wavFormat);
    int chunkBytes = getChunkBytes();
    long sleepMillis = AudioChunker.calculateSleepMillis(chunkBytes, config.sampleRate(), config.bytesPerSample());

    try (InputStream inputStream = Files.newInputStream(audioPath)) {
      if (extension.equals("wav")) {
        long skipped = inputStream.skip(WAV_HEADER_BYTES);
        if (skipped < WAV_HEADER_BYTES) {
          logger.warn("WAV header is smaller than expected, skipped {} bytes", skipped);
        }
      }
      byte[] buffer = new byte[chunkBytes];
      int readSize = inputStream.read(buffer, 0, chunkBytes);
      while (readSize > 0) {
        if (readSize == chunkBytes) {
          sendAudio(buffer, buffer.length);
        } else {
          sendAudio(buffer, readSize);
        }
        if (!config.isOffline()) {
          Thread.sleep(sleepMillis);
        }
        readSize = inputStream.read(buffer, 0, chunkBytes);
      }
    }

    finishStream();
  }

  public void sendEof() {
    JSONObject obj = new JSONObject();
    obj.put("is_speaking", Boolean.FALSE);
    wsClient.send(obj.toJSONString());
    wsClient.markEofSent();
  }

  public boolean awaitClose(Duration timeout) throws InterruptedException {
    return wsClient.awaitClose(timeout);
  }

  @Override
  public void close() {
    wsClient.close();
  }

  public int getChunkBytes() {
    return AudioChunker.calculateChunkBytes(
        config.chunkSize(), config.chunkInterval(), config.sampleRate(), config.bytesPerSample());
  }

  public void startStream(String wavName, String wavFormat) {
    sendStartJson(
        config.mode(),
        config.chunkSize(),
        config.chunkInterval(),
        wavName,
        true,
        wavFormat,
        config.hotwords());
  }

  public void sendAudio(byte[] bytes, int length) {
    if (length <= 0) {
      return;
    }
    if (length == bytes.length) {
      wsClient.send(bytes);
      return;
    }
    byte[] tmpBytes = new byte[length];
    System.arraycopy(bytes, 0, tmpBytes, 0, length);
    wsClient.send(tmpBytes);
  }

  public void finishStream() throws InterruptedException {
    if (!config.isOffline()) {
      Thread.sleep(2000);
    }
    sendEof();
  }

  private void sendStartJson(
      String mode,
      String strChunkSize,
      int chunkInterval,
      String wavName,
      boolean isSpeaking,
      String suffix,
      String hotwords) {
    JSONObject obj = new JSONObject();
    obj.put("mode", mode);
    JSONArray array = new JSONArray();
    String[] chunkList = strChunkSize.split(",");
    for (String chunk : chunkList) {
      array.add(Integer.valueOf(chunk.trim()));
    }
    obj.put("chunk_size", array);
    obj.put("chunk_interval", chunkInterval);
    obj.put("wav_name", wavName);

    Map<String, Integer> hotwordMap = HotwordParser.parse(hotwords);
    if (!hotwordMap.isEmpty()) {
      JSONObject jsonitems = new JSONObject();
      for (Map.Entry<String, Integer> entry : hotwordMap.entrySet()) {
        jsonitems.put(entry.getKey(), entry.getValue());
      }
      obj.put("hotwords", jsonitems.toJSONString());
    }

    obj.put("wav_format", suffix);
    obj.put("is_speaking", isSpeaking);

    wsClient.send(obj.toJSONString());
  }

  private static String getExtension(Path path) {
    String fileName = path.getFileName().toString();
    int index = fileName.lastIndexOf('.');
    if (index == -1 || index == fileName.length() - 1) {
      return "pcm";
    }
    return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
  }
}
