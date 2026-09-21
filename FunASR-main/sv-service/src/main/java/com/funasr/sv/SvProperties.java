package com.funasr.sv;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sv")
public class SvProperties {
  private Path modelPath;
  private Path embeddingPath;
  private Path embeddingsDir;
  private double threshold = 0.7;
  private int sampleRate = 16000;
  private int nMels = 80;
  private int frameLengthMs = 25;
  private int frameShiftMs = 10;
  private double fmin = 20.0;
  private double fmax = 7600.0;
  private double preEmphasis = 0.97;

  public Path getModelPath() {
    return modelPath;
  }

  public void setModelPath(Path modelPath) {
    this.modelPath = modelPath;
  }

  public Path getEmbeddingPath() {
    return embeddingPath;
  }

  public void setEmbeddingPath(Path embeddingPath) {
    this.embeddingPath = embeddingPath;
  }

  public Path getEmbeddingsDir() {
    return embeddingsDir;
  }

  public void setEmbeddingsDir(Path embeddingsDir) {
    this.embeddingsDir = embeddingsDir;
  }

  public double getThreshold() {
    return threshold;
  }

  public void setThreshold(double threshold) {
    this.threshold = threshold;
  }

  public int getSampleRate() {
    return sampleRate;
  }

  public void setSampleRate(int sampleRate) {
    this.sampleRate = sampleRate;
  }

  public int getnMels() {
    return nMels;
  }

  public void setnMels(int nMels) {
    this.nMels = nMels;
  }

  public int getFrameLengthMs() {
    return frameLengthMs;
  }

  public void setFrameLengthMs(int frameLengthMs) {
    this.frameLengthMs = frameLengthMs;
  }

  public int getFrameShiftMs() {
    return frameShiftMs;
  }

  public void setFrameShiftMs(int frameShiftMs) {
    this.frameShiftMs = frameShiftMs;
  }

  public double getFmin() {
    return fmin;
  }

  public void setFmin(double fmin) {
    this.fmin = fmin;
  }

  public double getFmax() {
    return fmax;
  }

  public void setFmax(double fmax) {
    this.fmax = fmax;
  }

  public double getPreEmphasis() {
    return preEmphasis;
  }

  public void setPreEmphasis(double preEmphasis) {
    this.preEmphasis = preEmphasis;
  }
}
