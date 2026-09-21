# FunASR Speaker Verification Service

This service provides blacklist speaker filtering for ASR pipelines.

## Build and run
```bash
mvn -f sv-service/pom.xml -DskipTests package
java -jar sv-service/target/sv-service-0.1.0.jar
```

## APIs
- `POST /enroll` enroll one voiceprint from a wav file.
- `POST /verify` verify one wav file and return if it should be blocked.
- `GET /enrolled` list currently enrolled speaker ids.
- Unified Java controller APIs:
  - `POST /api/voice/enroll`
  - `POST /api/voice/verify`
  - `POST /api/voice/recognize`
  - `POST /api/voice/recognize_filter`

### Enroll one speaker
```bash
curl -X POST "http://127.0.0.1:8082/enroll" \
  -F "audio=@/path/to/speaker.wav" \
  -F "speaker_id=speaker_001"
```

### Verify one audio
```bash
curl -X POST "http://127.0.0.1:8082/verify" \
  -F "audio=@/path/to/test.wav"
```

Response example:
```json
{
  "score": 0.81,
  "threshold": 0.7,
  "blocked": true,
  "matched_speaker_id": "speaker_001",
  "enrolled_count": 3
}
```

## Batch enroll prepared wav files
```bash
./sv-service/scripts/enroll_blacklist.sh http://127.0.0.1:8082 /path/to/blacklist_wavs
```

The script uses wav file names (without extension) as `speaker_id`.

## Config
`sv-service/src/main/resources/application.yml`

- `sv.model-path` model file path
- `sv.threshold` cosine threshold for blocking
- `sv.embedding-path` legacy single-embedding path (`speaker_id=default`)
- `sv.embeddings-dir` multi-speaker embedding directory (`*.emb`)
- `asr.ws-uri` FunASR websocket service url, e.g. `ws://127.0.0.1:10095`
- `asr.*` chunk/timeout settings for websocket ASR calls from controller

### Unified controller examples
```bash
curl -X POST "http://127.0.0.1:8082/api/voice/enroll" \
  -F "audio=@/path/to/blacklist_user.wav" \
  -F "speaker_id=blacklist_user"
```

```bash
curl -X POST "http://127.0.0.1:8082/api/voice/verify" \
  -F "audio=@/path/to/test.wav"
```

```bash
curl -X POST "http://127.0.0.1:8082/api/voice/recognize_filter" \
  -F "audio=@/path/to/test.wav"
```
