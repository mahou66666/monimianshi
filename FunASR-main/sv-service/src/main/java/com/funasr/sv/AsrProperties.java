package com.funasr.sv;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "asr")
public class AsrProperties {
  private String wsUri = "ws://127.0.0.1:10095";
  private String mode = "2pass";
  private String chunkSize = "5,10,5";
  private int chunkInterval = 10;
  private int sampleRate = 16000;
  private int bytesPerSample = 2;
  private int connectTimeoutMs = 10000;
  private int resultTimeoutMs = 120000;
  private boolean realtimePacing = false;
  private boolean allowPartialOnTimeout = false;
  private String wavName = "sv-service";
  private boolean insecureSkipTlsVerify = false;

  public String getWsUri() {
    return wsUri;
  }

  public void setWsUri(String wsUri) {
    this.wsUri = wsUri;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getChunkSize() {
    return chunkSize;
  }

  public void setChunkSize(String chunkSize) {
    this.chunkSize = chunkSize;
  }

  public int getChunkInterval() {
    return chunkInterval;
  }

  public void setChunkInterval(int chunkInterval) {
    this.chunkInterval = chunkInterval;
  }

  public int getSampleRate() {
    return sampleRate;
  }

  public void setSampleRate(int sampleRate) {
    this.sampleRate = sampleRate;
  }

  public int getBytesPerSample() {
    return bytesPerSample;
  }

  public void setBytesPerSample(int bytesPerSample) {
    this.bytesPerSample = bytesPerSample;
  }

  public int getConnectTimeoutMs() {
    return connectTimeoutMs;
  }

  public void setConnectTimeoutMs(int connectTimeoutMs) {
    this.connectTimeoutMs = connectTimeoutMs;
  }

  public int getResultTimeoutMs() {
    return resultTimeoutMs;
  }

  public void setResultTimeoutMs(int resultTimeoutMs) {
    this.resultTimeoutMs = resultTimeoutMs;
  }

  public boolean isRealtimePacing() {
    return realtimePacing;
  }

  public void setRealtimePacing(boolean realtimePacing) {
    this.realtimePacing = realtimePacing;
  }

  public boolean isAllowPartialOnTimeout() {
    return allowPartialOnTimeout;
  }

  public void setAllowPartialOnTimeout(boolean allowPartialOnTimeout) {
    this.allowPartialOnTimeout = allowPartialOnTimeout;
  }

  public String getWavName() {
    return wavName;
  }

  public void setWavName(String wavName) {
    this.wavName = wavName;
  }

  public boolean isInsecureSkipTlsVerify() {
    return insecureSkipTlsVerify;
  }

  public void setInsecureSkipTlsVerify(boolean insecureSkipTlsVerify) {
    this.insecureSkipTlsVerify = insecureSkipTlsVerify;
  }
}
