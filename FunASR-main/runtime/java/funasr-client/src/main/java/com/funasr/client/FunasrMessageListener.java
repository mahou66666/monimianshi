package com.funasr.client;

public interface FunasrMessageListener {
  default void onOpen() {}

  default void onMessage(String message) {}

  default void onError(Exception ex) {}

  default void onClosed(int code, String reason, boolean remote) {}
}
