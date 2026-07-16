package com.lutzseverino.minecraftsetup.infrastructure.persistence;

import com.lutzseverino.minecraftsetup.application.ChallengeCodeGenerator;
import com.lutzseverino.minecraftsetup.domain.ChallengeCode;
import java.security.SecureRandom;

public final class SecureChallengeCodeGenerator implements ChallengeCodeGenerator {
  private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
  private final SecureRandom random = new SecureRandom();

  @Override
  public ChallengeCode next() {
    char[] code = new char[16];
    for (int index = 0; index < code.length; index++) {
      code[index] = ALPHABET[random.nextInt(ALPHABET.length)];
    }
    return new ChallengeCode(new String(code));
  }
}
