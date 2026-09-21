package com.funasr.client;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

public final class FunasrClientConfig {
  private final String host;
  private final int port;
  private final String scheme;
  private final String path;
  private final String mode;
  private final String chunkSize;
  private final int chunkInterval;
  private final String wavName;
  private final String hotwords;
  private final int sampleRate;
  private final int bytesPerSample;
  private final Duration connectTimeout;
  private final Duration closeTimeout;
  private final boolean legacyCloseOnFalse;

  private FunasrClientConfig(Builder builder) {
    this.host = builder.host;
    this.port = builder.port;
    this.scheme = builder.scheme;
    this.path = builder.path;
    this.mode = builder.mode;
    this.chunkSize = builder.chunkSize;
    this.chunkInterval = builder.chunkInterval;
    this.wavName = builder.wavName;
    this.hotwords = builder.hotwords;
    this.sampleRate = builder.sampleRate;
    this.bytesPerSample = builder.bytesPerSample;
    this.connectTimeout = builder.connectTimeout;
    this.closeTimeout = builder.closeTimeout;
    this.legacyCloseOnFalse = builder.legacyCloseOnFalse;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String host() {
    return host;
  }

  public int port() {
    return port;
  }

  public String scheme() {
    return scheme;
  }

  public String path() {
    return path;
  }

  public String mode() {
    return mode;
  }

  public String chunkSize() {
    return chunkSize;
  }

  public int chunkInterval() {
    return chunkInterval;
  }

  public String wavName() {
    return wavName;
  }

  public String hotwords() {
    return hotwords;
  }

  public int sampleRate() {
    return sampleRate;
  }

  public int bytesPerSample() {
    return bytesPerSample;
  }

  public Duration connectTimeout() {
    return connectTimeout;
  }

  public Duration closeTimeout() {
    return closeTimeout;
  }

  public boolean legacyCloseOnFalse() {
    return legacyCloseOnFalse;
  }

  public boolean isOffline() {
    return "offline".equals(mode);
  }

  public URI toWebsocketUri() {
    String normalizedPath = path;
    if (normalizedPath == null || normalizedPath.isBlank()) {
      normalizedPath = "";
    } else if (!normalizedPath.startsWith("/")) {
      normalizedPath = "/" + normalizedPath;
    }
    return URI.create(scheme + "://" + host + ":" + port + normalizedPath);
  }

  public static final class Builder {
    private String host = "127.0.0.1";
    private int port = 8889;
    private String scheme = "ws";
    private String path = "";
    private String mode = "offline";
    private String chunkSize = "5,10,5";
    private int chunkInterval = 10;
    private String wavName = "javatest";
    private String hotwords = "";
    private int sampleRate = 16000;
    private int bytesPerSample = 2;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration closeTimeout = Duration.ofSeconds(10);
    private boolean legacyCloseOnFalse = true;

    private Builder() {}

    public Builder host(String host) {
      this.host = Objects.requireNonNull(host, "host").trim();
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder scheme(String scheme) {
      this.scheme = Objects.requireNonNull(scheme, "scheme").trim();
      return this;
    }

    public Builder path(String path) {
      this.path = Objects.requireNonNullElse(path, "").trim();
      return this;
    }

    public Builder mode(String mode) {
      String normalized = Objects.requireNonNull(mode, "mode").trim().toLowerCase();
      this.mode = normalized;
      return this;
    }

    public Builder chunkSize(String chunkSize) {
      this.chunkSize = Objects.requireNonNull(chunkSize, "chunkSize").replace(" ", "").trim();
      return this;
    }

    public Builder chunkInterval(int chunkInterval) {
      this.chunkInterval = chunkInterval;
      return this;
    }

    public Builder wavName(String wavName) {
      this.wavName = Objects.requireNonNullElse(wavName, "").trim();
      return this;
    }

    public Builder hotwords(String hotwords) {
      this.hotwords = Objects.requireNonNullElse(hotwords, "").trim();
      return this;
    }

    public Builder sampleRate(int sampleRate) {
      this.sampleRate = sampleRate;
      return this;
    }

    public Builder bytesPerSample(int bytesPerSample) {
      this.bytesPerSample = bytesPerSample;
      return this;
    }

    public Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
      return this;
    }

    public Builder closeTimeout(Duration closeTimeout) {
      this.closeTimeout = Objects.requireNonNull(closeTimeout, "closeTimeout");
      return this;
    }

    public Builder legacyCloseOnFalse(boolean legacyCloseOnFalse) {
      this.legacyCloseOnFalse = legacyCloseOnFalse;
      return this;
    }

    public FunasrClientConfig build() {
      return new FunasrClientConfig(this);
    }
  }
}
