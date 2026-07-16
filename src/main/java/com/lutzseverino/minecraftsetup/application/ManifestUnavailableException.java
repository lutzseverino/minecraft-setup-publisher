package com.lutzseverino.minecraftsetup.application;

public final class ManifestUnavailableException extends Exception {
  private static final long serialVersionUID = 1L;

  public ManifestUnavailableException(String message) {
    super(message);
  }

  public ManifestUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
