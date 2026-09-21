package com.funasr.cli;

import com.funasr.client.FunasrClient;
import com.funasr.client.FunasrClientConfig;
import com.funasr.client.FunasrMessageListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "funasr-cli", mixinStandardHelpOptions = true, version = "funasr-cli 0.1.0")
public class FunasrCli implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(FunasrCli.class);

  @Option(names = "--host", description = "server ip", defaultValue = "127.0.0.1")
  private String host;

  @Option(names = "--port", description = "server port", defaultValue = "10095")
  private int port;

  @Option(names = "--audio_in", description = "wav/pcm file path", defaultValue = "asr_example.wav")
  private String audioIn;

  @Option(names = "--mic", description = "capture audio from microphone", defaultValue = "false")
  private boolean mic;

  @Option(
      names = "--mic_duration_sec",
      description = "record duration in seconds, 0=until ENTER",
      defaultValue = "0")
  private int micDurationSec;

  @Option(names = "--num_threads", description = "number of client threads", defaultValue = "1")
  private int numThreads;

  @Option(names = "--chunk_size", description = "chunk size list", defaultValue = "5,10,5")
  private String chunkSize;

  @Option(names = "--chunk_interval", description = "chunk interval", defaultValue = "10")
  private int chunkInterval;

  @Option(names = "--sample_rate", description = "audio sample rate", defaultValue = "16000")
  private int sampleRate;

  @Option(names = "--bytes_per_sample", description = "bytes per sample", defaultValue = "2")
  private int bytesPerSample;

  @Option(names = "--mode", description = "offline/online/2pass", defaultValue = "online")
  private String mode;

  @Option(names = "--hotwords", description = "hotwords with weights, e.g. 'hello 30 nihao 40'", defaultValue = "")
  private String hotwords;

  @Option(names = "--scheme", description = "ws or wss", defaultValue = "ws")
  private String scheme;

  @Option(names = "--ws_path", description = "websocket path", defaultValue = "")
  private String wsPath;

  @Option(names = "--connect_timeout_ms", description = "connect timeout in ms", defaultValue = "10000")
  private long connectTimeoutMs;

  @Option(names = "--close_timeout_ms", description = "close timeout in ms", defaultValue = "10000")
  private long closeTimeoutMs;

  @Option(names = "--wav_name", description = "wav_name for request", defaultValue = "")
  private String wavName;

  @Option(
      names = "--sv_url",
      description = "speaker verification service base url, e.g. http://localhost:8082",
      defaultValue = "")
  private String svUrl;

  @Option(
      names = "--sv_check_seconds",
      description = "seconds for mic precheck (speaker verification)",
      defaultValue = "3")
  private int svCheckSeconds;

  @Option(
      names = "--sv_timeout_ms",
      description = "speaker verification request timeout in ms",
      defaultValue = "5000")
  private long svTimeoutMs;

  @Option(
      names = "--sv_realtime_filter",
      description = "enable in-stream speaker filtering for microphone mode",
      defaultValue = "false")
  private boolean svRealtimeFilter;

  @Option(
      names = "--sv_stream_window_ms",
      description = "sv realtime filter window size in ms",
      defaultValue = "1600")
  private int svStreamWindowMs;

  @Option(
      names = "--sv_stream_min_ms",
      description = "minimum audio length for each sv realtime check in ms",
      defaultValue = "800")
  private int svStreamMinMs;

  @Option(
      names = "--sv_stream_check_ms",
      description = "interval between sv realtime checks in ms",
      defaultValue = "700")
  private int svStreamCheckMs;

  @Option(
      names = "--sv_stream_hold_ms",
      description = "drop duration after one sv realtime blocked decision in ms",
      defaultValue = "900")
  private int svStreamHoldMs;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new FunasrCli()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    Path audioPath = null;
    if (!mic) {
      audioPath = Paths.get(audioIn);
      if (!Files.exists(audioPath)) {
        logger.error("audio_in file not found: {}", audioPath);
        return 2;
      }
    } else if (numThreads > 1) {
      logger.warn("microphone mode forces num_threads=1");
    }

    int threads = mic ? 1 : Math.max(1, numThreads);
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    List<Future<?>> futures = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      int threadIndex = i + 1;
      Path finalAudioPath = audioPath;
      futures.add(executor.submit(() -> runClient(threadIndex, finalAudioPath, mic)));
    }

    int failures = 0;
    for (Future<?> future : futures) {
      try {
        future.get();
      } catch (Exception ex) {
        failures++;
        logger.error("client task failed", ex);
      }
    }

    executor.shutdown();
    return failures == 0 ? 0 : 1;
  }

  private void runClient(int threadIndex, Path audioPath, boolean useMic) {
    String normalizedMode = mode.trim().toLowerCase(Locale.ROOT);
    String defaultWavName = useMic ? "microphone" : audioPath.getFileName().toString();
    String baseWavName = wavName == null || wavName.isBlank() ? defaultWavName : wavName.trim();
    int effectiveThreads = useMic ? 1 : Math.max(1, numThreads);
    String actualWavName = effectiveThreads > 1 ? baseWavName + "-" + threadIndex : baseWavName;

    FunasrClientConfig config =
        FunasrClientConfig.builder()
            .host(host)
            .port(port)
            .scheme(scheme)
            .path(wsPath)
            .mode(normalizedMode)
            .chunkSize(chunkSize)
            .chunkInterval(chunkInterval)
            .wavName(actualWavName)
            .hotwords(hotwords)
            .sampleRate(sampleRate)
            .bytesPerSample(bytesPerSample)
            .connectTimeout(Duration.ofMillis(connectTimeoutMs))
            .closeTimeout(Duration.ofMillis(closeTimeoutMs))
            .build();

    FunasrMessageListener listener = new LoggingListener();

    try (FunasrClient client = new FunasrClient(config, listener)) {
      if (!useMic && isSvEnabled()) {
        SvResult svResult = SvHttpClient.verifyFile(svUrl, audioPath, svTimeoutMs);
        if (svResult.blocked) {
          logger.warn("speaker verification blocked, score={}", svResult.score);
          return;
        }
      }

      logger.info("connecting to {}", config.toWebsocketUri());
      client.connect();
      if (useMic) {
        streamMicrophone(client, config, actualWavName, svUrl, svCheckSeconds, svTimeoutMs);
      } else {
        client.streamFile(audioPath);
      }
      if (!config.isOffline()) {
        Thread.sleep(3000);
        client.close();
      }
      boolean closed = client.awaitClose(config.closeTimeout());
      if (!closed) {
        client.close();
      }
    } catch (TimeoutException ex) {
      logger.error("connect timeout: {}", ex.getMessage());
    } catch (Exception ex) {
      logger.error("client error", ex);
    }
  }

  private void streamMicrophone(
      FunasrClient client,
      FunasrClientConfig config,
      String wavName,
      String svUrl,
      int svSeconds,
      long svTimeoutMs)
      throws Exception {
    if (config.isOffline()) {
      logger.warn("microphone mode is usually used with online/2pass");
    }

    AudioFormat format =
        new AudioFormat(
            config.sampleRate(),
            config.bytesPerSample() * 8,
            1,
            true,
            false);
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    if (!AudioSystem.isLineSupported(info)) {
      logger.error(
          "microphone does not support format: {} Hz, {}-bit, mono",
          config.sampleRate(),
          config.bytesPerSample() * 8);
      logAvailableMixers(format);
      return;
    }

    int chunkBytes = client.getChunkBytes();
    byte[] buffer = new byte[chunkBytes];
    AtomicBoolean stopSignal = new AtomicBoolean(false);

    int durationSec = micDurationSec;
    boolean waitForEnter = durationSec <= 0;
    if (waitForEnter && System.console() == null) {
      durationSec = 10;
      waitForEnter = false;
      logger.warn("no console detected, record for {} seconds", durationSec);
    }

    if (waitForEnter) {
      logger.info("press ENTER to stop recording");
      Thread stopThread =
          new Thread(
              () -> {
                try {
                  new BufferedReader(new InputStreamReader(System.in)).readLine();
                } catch (Exception ex) {
                  logger.debug("stdin closed", ex);
                }
                stopSignal.set(true);
              });
      stopThread.setDaemon(true);
      stopThread.start();
    }

    long endAt =
        durationSec > 0
            ? System.nanoTime() + Duration.ofSeconds(durationSec).toNanos()
            : Long.MAX_VALUE;

    try (TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info)) {
      line.open(format);
      line.start();
      logger.info("recording from microphone...");

      boolean useSv = isSvEnabled();
      boolean useSvRealtimeFilter = useSv && svRealtimeFilter;
      ByteArrayOutputStream precheck = null;
      int targetBytes = 0;
      if (useSv) {
        int seconds = Math.max(1, svSeconds);
        targetBytes = seconds * config.sampleRate() * config.bytesPerSample();
        precheck = new ByteArrayOutputStream(targetBytes);
        while (precheck.size() < targetBytes && !stopSignal.get() && System.nanoTime() < endAt) {
          int read = line.read(buffer, 0, buffer.length);
          precheck.write(buffer, 0, read);
        }
        if (precheck.size() == 0) {
          logger.warn("speaker verification skipped: no audio captured");
        } else {
          byte[] wavBytes = WavUtil.pcmToWav(precheck.toByteArray(), config.sampleRate(), config.bytesPerSample(), 1);
          SvResult svResult = SvHttpClient.verifyBytes(svUrl, wavBytes, svTimeoutMs);
          if (svResult.blocked) {
            logger.warn("speaker verification blocked, score={}", svResult.score);
            line.stop();
            return;
          }
        }
      }

      StreamSpeakerFilter streamSpeakerFilter = null;
      if (useSvRealtimeFilter) {
        streamSpeakerFilter =
            new StreamSpeakerFilter(
                svUrl,
                config.sampleRate(),
                config.bytesPerSample(),
                svTimeoutMs,
                svStreamWindowMs,
                svStreamMinMs,
                svStreamCheckMs,
                svStreamHoldMs);
        logger.info(
            "sv realtime filter enabled: window={}ms min={}ms check={}ms hold={}ms",
            svStreamWindowMs,
            svStreamMinMs,
            svStreamCheckMs,
            svStreamHoldMs);
      }

      long droppedBytes = 0L;
      client.startStream(wavName, "pcm");
      try {
        if (precheck != null && precheck.size() > 0) {
          byte[] preBytes = precheck.toByteArray();
          if (streamSpeakerFilter != null) {
            streamSpeakerFilter.accept(preBytes, preBytes.length);
            if (streamSpeakerFilter.shouldDrop()) {
              droppedBytes += preBytes.length;
            } else {
              client.sendAudio(preBytes, preBytes.length);
            }
          } else {
            client.sendAudio(preBytes, preBytes.length);
          }
        }

        while (!stopSignal.get() && System.nanoTime() < endAt) {
          int read = line.read(buffer, 0, buffer.length);
          if (streamSpeakerFilter != null) {
            streamSpeakerFilter.accept(buffer, read);
            if (streamSpeakerFilter.shouldDrop()) {
              droppedBytes += read;
              continue;
            }
          }
          client.sendAudio(buffer, read);
        }
        line.stop();
      } finally {
        if (streamSpeakerFilter != null) {
          streamSpeakerFilter.close();
          if (droppedBytes > 0) {
            double droppedSec =
                droppedBytes / (double) (config.sampleRate() * config.bytesPerSample());
            logger.info("sv realtime filter dropped {}s audio", String.format("%.2f", droppedSec));
          }
        }
      }
    } catch (LineUnavailableException ex) {
      logger.error("microphone unavailable", ex);
      return;
    }

    client.finishStream();
  }

  private boolean isSvEnabled() {
    return svUrl != null && !svUrl.isBlank();
  }

  private void logAvailableMixers(AudioFormat format) {
    Mixer.Info[] mixers = AudioSystem.getMixerInfo();
    if (mixers.length == 0) {
      logger.info("no audio mixers found");
      return;
    }
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    for (Mixer.Info mixerInfo : mixers) {
      Mixer mixer = AudioSystem.getMixer(mixerInfo);
      boolean supported = mixer.isLineSupported(info);
      logger.info("mixer: {} ({})", mixerInfo.getName(), supported ? "supported" : "not supported");
    }
  }

  private static final class LoggingListener implements FunasrMessageListener {
    private final Logger logger = LoggerFactory.getLogger(LoggingListener.class);
    private final JSONParser parser = new JSONParser();

    @Override
    public void onMessage(String message) {
      logger.info("received: {}", message);
      try {
        JSONObject jsonObject = (JSONObject) parser.parse(message);
        Object text = jsonObject.get("text");
        if (text != null) {
          logger.info("text: {}", text);
        }
        Object timestamp = jsonObject.get("timestamp");
        if (timestamp != null) {
          logger.info("timestamp: {}", timestamp);
        }
      } catch (ParseException ex) {
        logger.debug("failed to parse json", ex);
      }
    }

    @Override
    public void onClosed(int code, String reason, boolean remote) {
      logger.info(
          "connection closed by {} Code: {} Reason: {}",
          remote ? "remote" : "local",
          code,
          reason);
    }

    @Override
    public void onError(Exception ex) {
      logger.warn("websocket error", ex);
    }
  }

  private static final class SvHttpClient {
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();

    private SvHttpClient() {}

    static SvResult verifyFile(String baseUrl, Path audioPath, long timeoutMs) throws Exception {
      byte[] bytes = Files.readAllBytes(audioPath);
      return verifyBytes(baseUrl, bytes, timeoutMs);
    }

    static SvResult verifyBytes(String baseUrl, byte[] wavBytes, long timeoutMs) throws Exception {
      String url = normalizeUrl(baseUrl) + "/verify";
      String boundary = "----FunasrBoundary" + UUID.randomUUID();
      byte[] payload = buildMultipart(boundary, wavBytes);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(Duration.ofMillis(timeoutMs))
              .header("Content-Type", "multipart/form-data; boundary=" + boundary)
              .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
              .build();

      HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException("SV service error: " + response.statusCode() + " " + response.body());
      }
      return parseResult(response.body());
    }

    private static String normalizeUrl(String base) {
      if (base.endsWith("/")) {
        return base.substring(0, base.length() - 1);
      }
      return base;
    }

    private static SvResult parseResult(String body) throws Exception {
      JSONObject json = (JSONObject) new JSONParser().parse(body);
      double score = Double.parseDouble(String.valueOf(json.get("score")));
      boolean blocked = Boolean.parseBoolean(String.valueOf(json.get("blocked")));
      return new SvResult(score, blocked);
    }

    private static byte[] buildMultipart(String boundary, byte[] wavBytes) throws Exception {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      String header =
          "--" + boundary + "\r\n"
              + "Content-Disposition: form-data; name=\"audio\"; filename=\"audio.wav\"\r\n"
              + "Content-Type: audio/wav\r\n\r\n";
      out.write(header.getBytes(StandardCharsets.UTF_8));
      out.write(wavBytes);
      out.write("\r\n".getBytes(StandardCharsets.UTF_8));
      String footer = "--" + boundary + "--\r\n";
      out.write(footer.getBytes(StandardCharsets.UTF_8));
      return out.toByteArray();
    }
  }

  private static final class SvResult {
    private final double score;
    private final boolean blocked;

    private SvResult(double score, boolean blocked) {
      this.score = score;
      this.blocked = blocked;
    }
  }

  private static final class StreamSpeakerFilter implements AutoCloseable {
    private final String svUrl;
    private final int sampleRate;
    private final int bytesPerSample;
    private final long timeoutMs;
    private final long checkIntervalNanos;
    private final long holdNanos;
    private final int minBytes;
    private final int windowBytes;
    private final ExecutorService executor;
    private final Object lock = new Object();
    private final ByteArrayOutputStream rolling;
    private volatile long lastCheckNanos;
    private volatile long blockedUntilNanos;
    private volatile boolean inFlight;

    private StreamSpeakerFilter(
        String svUrl,
        int sampleRate,
        int bytesPerSample,
        long timeoutMs,
        int windowMs,
        int minMs,
        int checkMs,
        int holdMs) {
      this.svUrl = svUrl;
      this.sampleRate = sampleRate;
      this.bytesPerSample = bytesPerSample;
      this.timeoutMs = timeoutMs;
      this.windowBytes = msToBytes(windowMs, sampleRate, bytesPerSample);
      this.minBytes = Math.min(windowBytes, msToBytes(minMs, sampleRate, bytesPerSample));
      this.checkIntervalNanos = Duration.ofMillis(Math.max(100, checkMs)).toNanos();
      this.holdNanos = Duration.ofMillis(Math.max(100, holdMs)).toNanos();
      this.rolling = new ByteArrayOutputStream(windowBytes);
      this.executor = Executors.newSingleThreadExecutor();
    }

    void accept(byte[] data, int length) {
      byte[] snapshot = null;
      long now = System.nanoTime();
      synchronized (lock) {
        rolling.write(data, 0, length);
        trimToWindowLocked();
        if (!inFlight
            && rolling.size() >= minBytes
            && (lastCheckNanos == 0 || now - lastCheckNanos >= checkIntervalNanos)) {
          snapshot = rolling.toByteArray();
          lastCheckNanos = now;
          inFlight = true;
        }
      }
      if (snapshot != null) {
        byte[] pcmSnapshot = snapshot;
        executor.submit(
            () -> {
              try {
                byte[] wavBytes = WavUtil.pcmToWav(pcmSnapshot, sampleRate, bytesPerSample, 1);
                SvResult svResult = SvHttpClient.verifyBytes(svUrl, wavBytes, timeoutMs);
                if (svResult.blocked) {
                  blockedUntilNanos = System.nanoTime() + holdNanos;
                }
              } catch (Exception ex) {
                logger.warn("sv realtime verify failed: {}", ex.getMessage());
              } finally {
                inFlight = false;
              }
            });
      }
    }

    boolean shouldDrop() {
      return System.nanoTime() < blockedUntilNanos;
    }

    @Override
    public void close() {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(200, TimeUnit.MILLISECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        executor.shutdownNow();
      }
    }

    private void trimToWindowLocked() {
      if (rolling.size() <= windowBytes) {
        return;
      }
      byte[] all = rolling.toByteArray();
      byte[] tail = Arrays.copyOfRange(all, all.length - windowBytes, all.length);
      rolling.reset();
      rolling.write(tail, 0, tail.length);
    }

    private static int msToBytes(int ms, int sampleRate, int bytesPerSample) {
      int safeMs = Math.max(100, ms);
      return safeMs * sampleRate * bytesPerSample / 1000;
    }
  }

  private static final class WavUtil {
    private static byte[] pcmToWav(byte[] pcm, int sampleRate, int bytesPerSample, int channels)
        throws Exception {
      int bitsPerSample = bytesPerSample * 8;
      int byteRate = sampleRate * channels * bytesPerSample;
      int blockAlign = channels * bytesPerSample;
      int dataSize = pcm.length;
      int chunkSize = 36 + dataSize;

      ByteArrayOutputStream out = new ByteArrayOutputStream(44 + dataSize);
      out.write("RIFF".getBytes(StandardCharsets.US_ASCII));
      writeIntLE(out, chunkSize);
      out.write("WAVE".getBytes(StandardCharsets.US_ASCII));
      out.write("fmt ".getBytes(StandardCharsets.US_ASCII));
      writeIntLE(out, 16);
      writeShortLE(out, (short) 1);
      writeShortLE(out, (short) channels);
      writeIntLE(out, sampleRate);
      writeIntLE(out, byteRate);
      writeShortLE(out, (short) blockAlign);
      writeShortLE(out, (short) bitsPerSample);
      out.write("data".getBytes(StandardCharsets.US_ASCII));
      writeIntLE(out, dataSize);
      out.write(pcm);
      return out.toByteArray();
    }

    private static void writeIntLE(ByteArrayOutputStream out, int value) {
      out.write(value & 0xff);
      out.write((value >> 8) & 0xff);
      out.write((value >> 16) & 0xff);
      out.write((value >> 24) & 0xff);
    }

    private static void writeShortLE(ByteArrayOutputStream out, short value) {
      out.write(value & 0xff);
      out.write((value >> 8) & 0xff);
    }
  }
}
