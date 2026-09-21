"""
  Copyright FunASR (https://github.com/alibaba-damo-academy/FunASR). All Rights
  Reserved. MIT License  (https://opensource.org/licenses/MIT)
  
  2023-2024 by zhaomingwork@qq.com  
"""

# pip install websocket-client
# apt install ffmpeg 

import threading
import traceback
import json
import time
import numpy as np
import os
import uuid
from urllib import request
from funasr_stream import FunasrStream
from funasr_tools import FunasrTools
from funasr_core import FunasrCore
# class for recognizer in websocket
class FunasrApi:
    """
    python asr recognizer lib

    """

    def __init__(
        self,
        uri="wss://www.funasr.com:10096/",
        timeout=1000,
        msg_callback=None,
        sv_url=None,
        gateway_url=None,
        sv_timeout=5.0,
        
    ):
        """
        uri: ws or wss server uri
        msg_callback: for message received
        timeout: timeout for get result
        sv_url: speaker verification service base url, e.g. http://127.0.0.1:8082
        gateway_url: unified java controller base url, e.g. http://127.0.0.1:8082
        sv_timeout: sv request timeout in seconds
        """
        try:
             
            
            self.uri=uri
            self.timeout=timeout
            self.msg_callback=msg_callback
            self.funasr_core=None
            # keep sv_url for backward compatibility; new flows should use gateway_url.
            self.sv_url = self._normalize_base_url(sv_url)
            if gateway_url is None:
               gateway_url = sv_url
            self.gateway_url = self._normalize_base_url(gateway_url)
            self.sv_timeout = sv_timeout
            
        except Exception as e:
            print("Exception:", e)
            traceback.print_exc()
    def create_stream(self,msg_callback=None):
        if self.funasr_core is not None:
            self.funasr_core.close()
        funasr_core=self.new_core(msg_callback=msg_callback)
        return FunasrStream(funasr_core)
         
            
            
        
    def new_core(self,msg_callback=None):
     try:
         if self.funasr_core is not None:
            self.funasr_core.close()
            
         if msg_callback==None:
            msg_callback=self.msg_callback
         funasr_core=FunasrCore(self.uri,msg_callback=msg_callback,timeout=self.timeout)
         funasr_core.new_connection()
         self.funasr_core=funasr_core
         return funasr_core
         
     except Exception as e:
            print("init_core",e)
            exit(0)
    
    # rec buffer, set ffmpeg_decode=True if audio is not PCM or WAV type
    def rec_buf(self,audio_buf,ffmpeg_decode=False):
       try:
           funasr_core=self.new_core()
           funasr_core.rec_buf(audio_buf,ffmpeg_decode=ffmpeg_decode)
           return funasr_core.get_result()
       except  Exception  as e:
            print("rec_file",e)
            return   
    # rec file 
    def rec_file(self,file_path):
       try:
           funasr_core=self.new_core()
           funasr_core.rec_file(file_path)
           return funasr_core.get_result()
       except  Exception  as e:
            print("rec_file",e)
            return 

    # configure unified java controller service
    def set_gateway_service(self, gateway_url, timeout=5.0):
       self.gateway_url = self._normalize_base_url(gateway_url)
       self.sv_timeout = timeout

    # backward-compatible method name
    def set_sv_service(self, sv_url, sv_timeout=5.0):
       self.sv_url = self._normalize_base_url(sv_url)
       self.gateway_url = self._normalize_base_url(sv_url)
       self.sv_timeout = sv_timeout

    # enroll blacklist speaker with prepared file via backend interface
    def enroll_blacklist_file(self, file_path, speaker_id="default"):
       try:
           audio_bytes = self._read_audio_file(file_path)
           wav_bytes = self._to_wav_if_needed(file_path, audio_bytes)
           return self.enroll_blacklist_buf(wav_bytes, speaker_id=speaker_id, is_wav=True)
       except Exception as e:
           print("enroll_blacklist_file", e)
           return None

    # enroll blacklist speaker with audio buffer via backend interface
    def enroll_blacklist_buf(self, audio_buf, speaker_id="default", ffmpeg_decode=False, is_wav=False):
       try:
           wav_bytes = audio_buf
           if ffmpeg_decode:
              wav_bytes = FunasrTools.audio2wav(audio_buf)
           elif not is_wav:
              # if caller can not guarantee WAV/PCM, set ffmpeg_decode=True
              wav_bytes = audio_buf
           if wav_bytes is None:
              print("enroll_blacklist_buf audio decode failed")
              return None

           fields = {"speaker_id": speaker_id}
           files = {
               "audio": ("audio.wav", wav_bytes, "audio/wav")
           }
           return self._gateway_post("/api/voice/enroll", fields=fields, files=files)
       except Exception as e:
           print("enroll_blacklist_buf", e)
           return None

    # verify speaker with prepared file via backend interface
    def verify_speaker_file(self, file_path):
       try:
           audio_bytes = self._read_audio_file(file_path)
           wav_bytes = self._to_wav_if_needed(file_path, audio_bytes)
           return self.verify_speaker_buf(wav_bytes, is_wav=True)
       except Exception as e:
           print("verify_speaker_file", e)
           return None

    # verify speaker with audio buffer via backend interface
    def verify_speaker_buf(self, audio_buf, ffmpeg_decode=False, is_wav=False):
       try:
           wav_bytes = audio_buf
           if ffmpeg_decode:
              wav_bytes = FunasrTools.audio2wav(audio_buf)
           elif not is_wav:
              wav_bytes = audio_buf
           if wav_bytes is None:
              print("verify_speaker_buf audio decode failed")
              return None
           files = {
               "audio": ("audio.wav", wav_bytes, "audio/wav")
           }
           return self._gateway_post("/api/voice/verify", fields=None, files=files)
       except Exception as e:
           print("verify_speaker_buf", e)
           return None

    # first verify by speaker service, then run asr only for non-blocked speaker
    def rec_file_with_sv_filter(self, file_path):
       try:
           audio_bytes = self._read_audio_file(file_path)
           wav_bytes = self._to_wav_if_needed(file_path, audio_bytes)
           files = {
               "audio": ("audio.wav", wav_bytes, "audio/wav")
           }
           return self._gateway_post("/api/voice/recognize_filter", fields=None, files=files)
       except Exception as e:
            print("rec_file_with_sv_filter", e)
            return {"blocked": False, "text": "", "sv_result": None}

    # first verify by speaker service, then run asr only for non-blocked speaker
    def rec_buf_with_sv_filter(self, audio_buf, ffmpeg_decode=False):
       try:
           wav_bytes = audio_buf
           if ffmpeg_decode:
              wav_bytes = FunasrTools.audio2wav(audio_buf)
           if wav_bytes is None:
              print("rec_buf_with_sv_filter audio decode failed")
              return {"blocked": False, "text": "", "sv_result": None}
           files = {
               "audio": ("audio.wav", wav_bytes, "audio/wav")
           }
           return self._gateway_post("/api/voice/recognize_filter", fields=None, files=files)
       except Exception as e:
            print("rec_buf_with_sv_filter", e)
            return {"blocked": False, "text": "", "sv_result": None}

    def recognize_file_via_controller(self, file_path, wav_name=None):
       try:
           audio_bytes = self._read_audio_file(file_path)
           wav_bytes = self._to_wav_if_needed(file_path, audio_bytes)
           fields = {}
           if wav_name is not None and str(wav_name).strip() != "":
              fields["wav_name"] = str(wav_name).strip()
           files = {
               "audio": ("audio.wav", wav_bytes, "audio/wav")
           }
           return self._gateway_post("/api/voice/recognize", fields=fields, files=files)
       except Exception as e:
           print("recognize_file_via_controller", e)
           return None

    def _gateway_post(self, api_path, fields=None, files=None):
       if self.gateway_url is None or self.gateway_url == "":
          raise Exception("gateway_url is empty, call set_gateway_service first")
       boundary, body = self._build_multipart_form(fields=fields, files=files)
       req = request.Request(
           url=self.gateway_url + api_path,
           data=body,
           method="POST",
           headers={"Content-Type": "multipart/form-data; boundary=" + boundary},
       )
       with request.urlopen(req, timeout=self.sv_timeout) as resp:
           resp_bytes = resp.read()
       return json.loads(resp_bytes.decode("utf-8"))

    @staticmethod
    def _build_multipart_form(fields=None, files=None):
       boundary = "----FunasrBoundary" + uuid.uuid4().hex
       body = bytearray()
       if fields is not None:
          for key, value in fields.items():
               body.extend(("--" + boundary + "\r\n").encode("utf-8"))
               body.extend(('Content-Disposition: form-data; name="' + str(key) + '"\r\n\r\n').encode("utf-8"))
               body.extend(str(value).encode("utf-8"))
               body.extend(b"\r\n")
       if files is not None:
          for key, file_item in files.items():
               filename, content, content_type = file_item
               body.extend(("--" + boundary + "\r\n").encode("utf-8"))
               body.extend(('Content-Disposition: form-data; name="' + str(key) + '"; filename="' + str(filename) + '"\r\n').encode("utf-8"))
               body.extend(("Content-Type: " + str(content_type) + "\r\n\r\n").encode("utf-8"))
               body.extend(content)
               body.extend(b"\r\n")
       body.extend(("--" + boundary + "--\r\n").encode("utf-8"))
       return boundary, bytes(body)

    @staticmethod
    def _normalize_base_url(url):
       if url is None:
          return None
       url = str(url).strip()
       if len(url) == 0:
          return ""
       if url.endswith("/"):
          return url[:-1]
       return url

    @staticmethod
    def _read_audio_file(file_path):
       with open(file_path, "rb") as f:
            return f.read()

    @staticmethod
    def _to_wav_if_needed(file_path, audio_bytes):
       file_ext = os.path.splitext(file_path)[-1].upper().replace(".", "")
       if file_ext == "WAV":
           return audio_bytes
       if file_ext == "PCM":
           return FunasrApi._pcm_to_wav(audio_bytes)
       return FunasrTools.audio2wav(audio_bytes)

    @staticmethod
    def _pcm_to_wav(pcm_bytes, sample_rate=16000, bytes_per_sample=2, channels=1):
       bits_per_sample = bytes_per_sample * 8
       byte_rate = sample_rate * channels * bytes_per_sample
       block_align = channels * bytes_per_sample
       data_size = len(pcm_bytes)
       chunk_size = 36 + data_size
       head = bytearray()
       head.extend(b"RIFF")
       head.extend(int(chunk_size).to_bytes(4, "little", signed=False))
       head.extend(b"WAVE")
       head.extend(b"fmt ")
       head.extend(int(16).to_bytes(4, "little", signed=False))
       head.extend(int(1).to_bytes(2, "little", signed=False))
       head.extend(int(channels).to_bytes(2, "little", signed=False))
       head.extend(int(sample_rate).to_bytes(4, "little", signed=False))
       head.extend(int(byte_rate).to_bytes(4, "little", signed=False))
       head.extend(int(block_align).to_bytes(2, "little", signed=False))
       head.extend(int(bits_per_sample).to_bytes(2, "little", signed=False))
       head.extend(b"data")
       head.extend(int(data_size).to_bytes(4, "little", signed=False))
       head.extend(pcm_bytes)
       return bytes(head)
 
 

 
 
