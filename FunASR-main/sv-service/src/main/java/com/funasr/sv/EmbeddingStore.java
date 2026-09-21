package com.funasr.sv;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EmbeddingStore {
  private EmbeddingStore() {}

  public static void save(Path path, float[] embedding) throws IOException {
    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }
    try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(path))) {
      out.writeInt(embedding.length);
      for (float v : embedding) {
        out.writeFloat(v);
      }
    }
  }

  public static float[] load(Path path) throws IOException {
    try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
      int len = in.readInt();
      float[] embedding = new float[len];
      for (int i = 0; i < len; i++) {
        embedding[i] = in.readFloat();
      }
      return embedding;
    }
  }
}
