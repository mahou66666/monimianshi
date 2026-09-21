package com.funasr.sv;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AsrWebSocketService {
  private static final Logger logger = LoggerFactory.getLogger(AsrWebSocketService.class);
  private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};
  private static final int MAX_RECOGNIZE_ATTEMPTS = 3;

  private final AsrProperties properties;
  private final ObjectMapper mapper;

  public AsrWebSocketService(AsrProperties properties, ObjectMapper mapper) {
    this.properties = properties;
    this.mapper = mapper;
  }

  public String recognize(byte[] audioBytes, String wavName) throws Exception {
    byte[] pcm = toPcm(audioBytes);
    Exception lastError = null;
    for (int attempt = 1; attempt <= MAX_RECOGNIZE_ATTEMPTS; attempt++) {
      try {
        return recognizeOnce(pcm, wavName, properties.getMode());
      } catch (IllegalArgumentException ex) {
        throw ex;
      } catch (Exception ex) {
        lastError = ex;
        logger.warn(
            "ASR websocket attempt {}/{} failed: {}",
            attempt,
            MAX_RECOGNIZE_ATTEMPTS,
            ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        if (attempt < MAX_RECOGNIZE_ATTEMPTS) {
          Thread.sleep(Math.min(2000L, 400L * attempt));
        }
      }
    }
    if (lastError != null) {
      throw lastError;
    }
    throw new IllegalStateException("ASR recognize failed with unknown reason");
  }

  private String recognizeOnce(byte[] pcm, String wavName, String mode) throws Exception {
    int chunkBytes = calculateChunkBytes();
    long sleepMillis = calculateSleepMillis(chunkBytes);
    long audioDurationMs = calculateAudioDurationMillis(pcm.length);
    long adaptiveTimeoutMs = calculateAdaptiveTimeoutMillis(audioDurationMs);
    AsrMessageCollector collector = new AsrMessageCollector(mapper);
    HttpClient httpClient = buildHttpClient();

    WebSocket ws =
        httpClient
            .newWebSocketBuilder()
            .subprotocols("binary")
            .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
            .buildAsync(URI.create(properties.getWsUri()), collector)
            .get(properties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS);

    try {
      String reqWavName =
          wavName == null || wavName.isBlank() ? properties.getWavName() : wavName.trim();
      String requestMode =
          mode == null || mode.isBlank() ? properties.getMode() : mode.trim().toLowerCase();

      Map<String, Object> startPayload = new LinkedHashMap<>();
      startPayload.put("mode", requestMode);
      startPayload.put("chunk_size", parseChunkSize(properties.getChunkSize()));
      startPayload.put("encoder_chunk_look_back", 4);
      startPayload.put("decoder_chunk_look_back", 0);
      startPayload.put("chunk_interval", properties.getChunkInterval());
      startPayload.put("wav_name", reqWavName);
      startPayload.put("wav_format", "pcm");
      startPayload.put("audio_fs", properties.getSampleRate());
      startPayload.put("hotwords", "");
      startPayload.put("itn", true);
      startPayload.put("is_speaking", true);
      ws.sendText(mapper.writeValueAsString(startPayload), true).join();

      boolean streamMode = !"offline".equalsIgnoreCase(requestMode);
      boolean shouldPace = properties.isRealtimePacing() || streamMode;
      int offset = 0;
      while (offset < pcm.length) {
        int len = Math.min(chunkBytes, pcm.length - offset);
        ws.sendBinary(ByteBuffer.wrap(pcm, offset, len), true).join();
        offset += len;
        if ("offline".equalsIgnoreCase(requestMode)) {
          Thread.sleep(1L);
        } else if (shouldPace) {
          Thread.sleep(sleepMillis);
        }
      }

      if (streamMode) {
        Thread.sleep(200L);
      }
      ws.sendText("{\"is_speaking\":false}", true).join();
      boolean finalReceived = collector.awaitFinal(Duration.ofMillis(adaptiveTimeoutMs));
      if (!finalReceived) {
        logger.warn(
            "ASR result timeout, ws={}, mode={}, audioMs={}, timeoutMs={}",
            properties.getWsUri(),
            requestMode,
            audioDurationMs,
            adaptiveTimeoutMs);
        String partialText = collector.getPartialText();
        if (partialText != null && !partialText.isBlank()) {
          logger.warn("ASR timeout but keep partial transcription, len={}", partialText.length());
          return partialText;
        }
        throw new TimeoutException("ASR result timeout after " + adaptiveTimeoutMs + "ms");
      }
      String finalText = collector.getFinalText();
      if (finalText == null || finalText.isBlank()) {
        String partialText = collector.getPartialText();
        if (partialText != null && !partialText.isBlank()) {
          logger.warn("ASR final text empty but keep partial transcription, len={}", partialText.length());
          return partialText;
        }
        throw new IllegalArgumentException(
            "No speech detected. Please check selected microphone input device.");
      }
      return finalText;
    } finally {
      // Do not actively send close frame here.
      // Some FunASR runtime builds may terminate server process after client close.
    }
  }

  private byte[] toPcm(byte[] audioBytes) {
    if (isWav(audioBytes)) {
      WavUtils.WavData wav = WavUtils.readWav(audioBytes);
      float[] pcm =
          WavUtils.resampleIfNeeded(wav.samples(), wav.sampleRate(), properties.getSampleRate());
      return floatToPcm16Le(pcm);
    }
    String compressedFormat = detectCompressedAudioFormat(audioBytes);
    if (compressedFormat != null) {
      throw new IllegalArgumentException(
          "Unsupported audio format: "
              + compressedFormat
              + ". Please upload 16kHz mono WAV audio.");
    }
    return audioBytes;
  }

  private static boolean isWav(byte[] bytes) {
    if (bytes.length < 12) {
      return false;
    }
    String riff = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
    String wave = new String(bytes, 8, 4, StandardCharsets.US_ASCII);
    return "RIFF".equals(riff) && "WAVE".equals(wave);
  }

  private static String detectCompressedAudioFormat(byte[] bytes) {
    if (bytes.length < 4) {
      return null;
    }
    if (startsWithAscii(bytes, "ID3")) {
      return "mp3";
    }
    if (startsWithAscii(bytes, "OggS")) {
      return "ogg";
    }
    if (startsWithBytes(bytes, (byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3)) {
      return "webm";
    }
    if (bytes.length >= 12 && startsWithAscii(bytes, 4, "ftyp")) {
      return "mp4/m4a";
    }
    return null;
  }

  private static boolean startsWithAscii(byte[] bytes, String expected) {
    return startsWithAscii(bytes, 0, expected);
  }

  private static boolean startsWithAscii(byte[] bytes, int offset, String expected) {
    if (bytes.length < offset + expected.length()) {
      return false;
    }
    for (int i = 0; i < expected.length(); i++) {
      if (bytes[offset + i] != (byte) expected.charAt(i)) {
        return false;
      }
    }
    return true;
  }

  private static boolean startsWithBytes(byte[] bytes, byte... expected) {
    if (bytes.length < expected.length) {
      return false;
    }
    for (int i = 0; i < expected.length; i++) {
      if (bytes[i] != expected[i]) {
        return false;
      }
    }
    return true;
  }

  private static byte[] floatToPcm16Le(float[] pcm) {
    byte[] out = new byte[pcm.length * 2];
    int idx = 0;
    for (float v : pcm) {
      float clamped = Math.max(-1.0f, Math.min(1.0f, v));
      short s = (short) Math.round(clamped * 32767.0f);
      out[idx++] = (byte) (s & 0xff);
      out[idx++] = (byte) ((s >> 8) & 0xff);
    }
    return out;
  }

  private int calculateChunkBytes() {
    String[] parts = properties.getChunkSize().split(",");
    if (parts.length < 2) {
      throw new IllegalArgumentException("chunk-size must be like 5,10,5");
    }
    int middle = Integer.parseInt(parts[1].trim());
    int intChunkSize = 60 * middle / properties.getChunkInterval();
    int framesPerChunk = properties.getSampleRate() / 1000 * intChunkSize;
    return framesPerChunk * properties.getBytesPerSample();
  }

  private long calculateSleepMillis(int chunkBytes) {
    int bytesPerSecond = properties.getSampleRate() * properties.getBytesPerSample();
    return Math.max(1L, (long) chunkBytes * 1000 / bytesPerSecond);
  }

  private long calculateAudioDurationMillis(int pcmBytes) {
    int bytesPerSecond = properties.getSampleRate() * properties.getBytesPerSample();
    if (bytesPerSecond <= 0) {
      return 0L;
    }
    return Math.max(1L, ((long) pcmBytes * 1000L) / bytesPerSecond);
  }

  private long calculateAdaptiveTimeoutMillis(long audioDurationMs) {
    long configured = Math.max(10_000L, properties.getResultTimeoutMs());
    long estimated = audioDurationMs * 3L + 15_000L;
    long upperBound = Math.max(configured, 600_000L);
    return Math.min(Math.max(configured, estimated), upperBound);
  }

  private static List<Integer> parseChunkSize(String chunkSize) {
    String[] parts = chunkSize.split(",");
    List<Integer> out = new ArrayList<>(parts.length);
    for (String part : parts) {
      out.add(Integer.parseInt(part.trim()));
    }
    return out;
  }

  private HttpClient buildHttpClient() {
    try {
      HttpClient.Builder builder = HttpClient.newBuilder();
      if (properties.isInsecureSkipTlsVerify()) {
        logger.warn("ASR TLS verification is disabled. Use only for development.");
        builder.sslContext(buildInsecureSslContext());
      }
      return builder.build();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to build ASR HttpClient", ex);
    }
  }

  private static SSLContext buildInsecureSslContext() throws Exception {
    TrustManager[] trustAll =
        new TrustManager[] {
          new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
          }
        };
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustAll, new SecureRandom());
    return sslContext;
  }

  private static final class AsrMessageCollector implements WebSocket.Listener {
    private final ObjectMapper mapper;
    private final StringBuilder frameBuffer = new StringBuilder();
    private final StringBuilder finalText = new StringBuilder();
    private volatile String partialText = "";
    private final CountDownLatch finalLatch = new CountDownLatch(1);
    private final AtomicReference<Throwable> parseError = new AtomicReference<>();

    private AsrMessageCollector(ObjectMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
      webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      frameBuffer.append(data);
      if (last) {
        handleMessage(frameBuffer.toString());
        frameBuffer.setLength(0);
      }
      webSocket.request(1);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      finalLatch.countDown();
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
      parseError.compareAndSet(null, error);
      finalLatch.countDown();
    }

    boolean awaitFinal(Duration timeout) throws Exception {
      boolean ok = finalLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
      Throwable err = parseError.get();
      if (err != null) {
        throw new IllegalStateException("ASR websocket failed", err);
      }
      return ok;
    }

    String getFinalText() {
      String finalized = finalText.toString().trim();
      if (!finalized.isBlank()) {
        return finalized;
      }
      return getPartialText();
    }

    String getPartialText() {
      return partialText == null ? "" : partialText.trim();
    }

    private void handleMessage(String msg) {
      try {
        Map<String, Object> json = mapper.readValue(msg, MAP_REF);
        Object mode = json.get("mode");
        String modeText = String.valueOf(mode);
        Object textObj = json.get("text");
        if (textObj != null) {
          String text = String.valueOf(textObj).trim();
          if (!text.isBlank()) {
            partialText = text;
          }
          if ("2pass-offline".equals(modeText)) {
            finalText.append(text);
          } else if ("offline".equals(modeText)) {
            finalText.setLength(0);
            finalText.append(text);
            if (!text.isBlank()) {
              finalLatch.countDown();
            }
          }
        }

        if (isTrue(json.get("is_final"))) {
          finalLatch.countDown();
        }
      } catch (Exception ex) {
        parseError.compareAndSet(null, ex);
        finalLatch.countDown();
      }
    }

    private static boolean isTrue(Object value) {
      if (value instanceof Boolean b) {
        return b;
      }
      return value != null && "true".equalsIgnoreCase(String.valueOf(value));
    }
  }
}
