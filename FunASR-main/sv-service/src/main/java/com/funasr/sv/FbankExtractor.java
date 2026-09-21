package com.funasr.sv;

import java.util.Arrays;

public class FbankExtractor {
  private final int sampleRate;
  private final int nMels;
  private final int frameLength;
  private final int frameShift;
  private final double fmin;
  private final double fmax;
  private final double preEmphasis;
  private final int fftSize;
  private final double[] window;
  private final double[][] melFilter;

  public FbankExtractor(SvProperties properties) {
    this.sampleRate = properties.getSampleRate();
    this.nMels = properties.getnMels();
    this.frameLength = sampleRate * properties.getFrameLengthMs() / 1000;
    this.frameShift = sampleRate * properties.getFrameShiftMs() / 1000;
    this.fmin = properties.getFmin();
    this.fmax = properties.getFmax();
    this.preEmphasis = properties.getPreEmphasis();
    this.fftSize = nextPowerOfTwo(frameLength);
    this.window = hammingWindow(frameLength);
    this.melFilter = buildMelFilter();
  }

  public float[][] extract(float[] pcm, int sr) {
    if (sr != sampleRate) {
      throw new IllegalArgumentException("Unexpected sample rate: " + sr);
    }
    if (pcm.length < frameLength) {
      return new float[0][0];
    }

    float[] emphasized = applyPreEmphasis(pcm);
    int frames = 1 + (emphasized.length - frameLength) / frameShift;
    float[][] feats = new float[frames][nMels];

    double[] real = new double[fftSize];
    double[] imag = new double[fftSize];
    double[] power = new double[fftSize / 2 + 1];

    for (int i = 0; i < frames; i++) {
      int offset = i * frameShift;
      Arrays.fill(real, 0);
      Arrays.fill(imag, 0);
      for (int j = 0; j < frameLength; j++) {
        real[j] = emphasized[offset + j] * window[j];
      }
      Fft.fft(real, imag);
      for (int k = 0; k < power.length; k++) {
        power[k] = real[k] * real[k] + imag[k] * imag[k];
      }
      for (int m = 0; m < nMels; m++) {
        double energy = 0.0;
        for (int k = 0; k < power.length; k++) {
          energy += power[k] * melFilter[m][k];
        }
        feats[i][m] = (float) Math.log(Math.max(energy, 1e-10));
      }
    }

    applyCmn(feats);
    return feats;
  }

  private float[] applyPreEmphasis(float[] pcm) {
    if (preEmphasis <= 0) {
      return pcm;
    }
    float[] out = new float[pcm.length];
    out[0] = pcm[0];
    for (int i = 1; i < pcm.length; i++) {
      out[i] = (float) (pcm[i] - preEmphasis * pcm[i - 1]);
    }
    return out;
  }

  private void applyCmn(float[][] feats) {
    int frames = feats.length;
    if (frames == 0) {
      return;
    }
    double[] mean = new double[nMels];
    for (float[] frame : feats) {
      for (int m = 0; m < nMels; m++) {
        mean[m] += frame[m];
      }
    }
    for (int m = 0; m < nMels; m++) {
      mean[m] /= frames;
    }
    for (float[] frame : feats) {
      for (int m = 0; m < nMels; m++) {
        frame[m] -= mean[m];
      }
    }
  }

  private double[][] buildMelFilter() {
    int nfreq = fftSize / 2 + 1;
    double lowMel = hzToMel(fmin);
    double highMel = hzToMel(fmax);
    double[] melPoints = linspace(lowMel, highMel, nMels + 2);
    double[] hzPoints = new double[melPoints.length];
    int[] bins = new int[melPoints.length];
    for (int i = 0; i < melPoints.length; i++) {
      hzPoints[i] = melToHz(melPoints[i]);
      bins[i] = (int) Math.floor((fftSize + 1) * hzPoints[i] / sampleRate);
    }

    double[][] filter = new double[nMels][nfreq];
    for (int m = 1; m <= nMels; m++) {
      int left = bins[m - 1];
      int center = bins[m];
      int right = bins[m + 1];
      for (int k = left; k < center && k < nfreq; k++) {
        filter[m - 1][k] = (k - left) / (double) Math.max(center - left, 1);
      }
      for (int k = center; k < right && k < nfreq; k++) {
        filter[m - 1][k] = (right - k) / (double) Math.max(right - center, 1);
      }
    }
    return filter;
  }

  private static double[] linspace(double start, double end, int count) {
    double[] out = new double[count];
    double step = (end - start) / (count - 1);
    for (int i = 0; i < count; i++) {
      out[i] = start + step * i;
    }
    return out;
  }

  private static double hzToMel(double hz) {
    return 2595.0 * Math.log10(1.0 + hz / 700.0);
  }

  private static double melToHz(double mel) {
    return 700.0 * (Math.pow(10.0, mel / 2595.0) - 1.0);
  }

  private static int nextPowerOfTwo(int value) {
    int n = 1;
    while (n < value) {
      n <<= 1;
    }
    return n;
  }

  private static double[] hammingWindow(int length) {
    double[] win = new double[length];
    for (int i = 0; i < length; i++) {
      win[i] = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * i / (length - 1));
    }
    return win;
  }
}
