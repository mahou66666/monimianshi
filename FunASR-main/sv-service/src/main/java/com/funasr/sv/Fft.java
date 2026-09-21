package com.funasr.sv;

public final class Fft {
  private Fft() {}

  public static void fft(double[] real, double[] imag) {
    int n = real.length;
    if (n != imag.length) {
      throw new IllegalArgumentException("Mismatched lengths");
    }
    if (Integer.bitCount(n) != 1) {
      throw new IllegalArgumentException("Length is not power of 2");
    }

    int levels = 31 - Integer.numberOfLeadingZeros(n);
    for (int i = 0; i < n; i++) {
      int j = Integer.reverse(i) >>> (32 - levels);
      if (j > i) {
        double tr = real[i];
        double ti = imag[i];
        real[i] = real[j];
        imag[i] = imag[j];
        real[j] = tr;
        imag[j] = ti;
      }
    }

    for (int size = 2; size <= n; size <<= 1) {
      int half = size >> 1;
      double theta = -2.0 * Math.PI / size;
      for (int i = 0; i < n; i += size) {
        for (int j = 0; j < half; j++) {
          int k = i + j;
          int l = k + half;
          double angle = theta * j;
          double cos = Math.cos(angle);
          double sin = Math.sin(angle);
          double tre = real[l] * cos - imag[l] * sin;
          double tim = real[l] * sin + imag[l] * cos;
          real[l] = real[k] - tre;
          imag[l] = imag[k] - tim;
          real[k] += tre;
          imag[k] += tim;
        }
      }
    }
  }
}
