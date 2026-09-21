# FunASR Java Client (Maven, Java 17)

## Prerequisites
- JDK 17+
- Maven 3.8+

## Build
```shell
cd runtime/java
mvn -pl funasr-cli -am package
```

## Run CLI
```shell
java -jar funasr-cli/target/funasr-cli-0.1.0-all.jar \
  --host localhost \
  --port 10095 \
  --audio_in ./asr_example.wav \
  --num_threads 1 \
  --mode online
```

## Run CLI with microphone
```shell
java -jar funasr-cli/target/funasr-cli-0.1.0-all.jar \
  --host localhost \
  --port 10095 \
  --mic \
  --mode online \
  --chunk_size 5,10,5
```
Note: Microphone capture uses 16kHz, 16-bit, mono PCM by default.

## Run CLI with speaker verification
```shell
java -jar funasr-cli/target/funasr-cli-0.1.0-all.jar \
  --host localhost \
  --port 10095 \
  --audio_in ./asr_example.wav \
  --mode online \
  --sv_url http://localhost:8082
```

## Run CLI with realtime speaker exclusion (two-person dialogue)
Use this when microphone audio may contain two speakers and you want to drop one enrolled blacklist speaker in realtime.
```shell
java -jar funasr-cli/target/funasr-cli-0.1.0-all.jar \
  --host localhost \
  --port 10095 \
  --mic \
  --mode online \
  --sv_url http://localhost:8082 \
  --sv_realtime_filter true \
  --sv_stream_window_ms 1600 \
  --sv_stream_min_ms 800 \
  --sv_stream_check_ms 700 \
  --sv_stream_hold_ms 900
```
Note: if two speakers overlap in the same time slice, filtering may still leak mixed speech.

## Enroll blacklist voiceprints (prepared audio)
1. Start the speaker verification service:
```shell
./run-sv-service.sh
```
2. Enroll one prepared voiceprint:
```shell
curl -X POST "http://127.0.0.1:8082/enroll" \
  -F "audio=@/path/to/blacklist_user.wav" \
  -F "speaker_id=blacklist_user"
```
3. Enroll multiple prepared voiceprints in a folder:
```shell
./sv-service/scripts/enroll_blacklist.sh http://127.0.0.1:8082 /path/to/blacklist_wavs
```
4. Check current enrolled speaker ids:
```shell
curl "http://127.0.0.1:8082/enrolled"
```

`/verify` now returns the best matched `matched_speaker_id` and `enrolled_count` with `blocked`.

### CLI Options (compatible with legacy flags)
- `--host` server ip
- `--port` server port
- `--audio_in` wav/pcm file path
- `--mic` capture audio from microphone
- `--mic_duration_sec` record duration in seconds, `0=until ENTER`
- `--num_threads` number of clients
- `--chunk_size` chunk size list, e.g. `5,10,5`
- `--chunk_interval` chunk interval
- `--sample_rate` audio sample rate, default `16000`
- `--bytes_per_sample` bytes per sample, default `2`
- `--mode` `offline` / `online` / `2pass`
- `--hotwords` hotwords with weights, e.g. `hello 30 nihao 40`
- `--scheme` `ws` or `wss`
- `--ws_path` websocket path
- `--connect_timeout_ms` connect timeout in ms
- `--close_timeout_ms` close timeout in ms
- `--wav_name` wav_name for request
- `--sv_url` speaker verification service base url
- `--sv_check_seconds` mic precheck seconds for SV
- `--sv_timeout_ms` sv request timeout
- `--sv_realtime_filter` enable in-stream SV filtering for microphone mode
- `--sv_stream_window_ms` realtime SV window size (ms)
- `--sv_stream_min_ms` minimum audio length used by each realtime SV check (ms)
- `--sv_stream_check_ms` interval between realtime SV checks (ms)
- `--sv_stream_hold_ms` drop duration after one blocked SV decision (ms)

## SDK Usage (example)
```java
FunasrClientConfig config = FunasrClientConfig.builder()
    .host("127.0.0.1")
    .port(10095)
    .mode("online")
    .chunkSize("5,10,5")
    .chunkInterval(10)
    .build();

try (FunasrClient client = new FunasrClient(config, new FunasrMessageListener() {})) {
  client.connect();
  client.streamFile(Paths.get("asr_example.wav"));
  client.awaitClose(Duration.ofSeconds(10));
}
```

## Legacy
The original single-file example `runtime/java/FunasrWsClient.java` and `runtime/java/Makefile`
are kept for reference only and are not maintained in the Maven build.

## Acknowledge
1. This project is maintained by FunASR community.
2. We acknowledge zhaoming for contributing the java websocket client example.
