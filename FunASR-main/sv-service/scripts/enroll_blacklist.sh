#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -lt 2 ]; then
  echo "Usage: $0 <sv_base_url> <wav_dir>"
  echo "Example: $0 http://127.0.0.1:8082 ./blacklist_wavs"
  exit 1
fi

SV_URL="${1%/}"
WAV_DIR="$2"

if [ ! -d "$WAV_DIR" ]; then
  echo "Directory not found: $WAV_DIR" >&2
  exit 1
fi

shopt -s nullglob
wav_files=("$WAV_DIR"/*.wav "$WAV_DIR"/*.WAV)
shopt -u nullglob

if [ "${#wav_files[@]}" -eq 0 ]; then
  echo "No wav files found under: $WAV_DIR" >&2
  exit 1
fi

for wav_path in "${wav_files[@]}"; do
  name="$(basename "$wav_path")"
  speaker_id="${name%.*}"
  echo "Enrolling $speaker_id from $wav_path"
  curl -sS -X POST "$SV_URL/enroll" \
    -F "audio=@${wav_path}" \
    -F "speaker_id=${speaker_id}"
  echo
done

echo "Current enrolled speakers:"
curl -sS "$SV_URL/enrolled"
echo
