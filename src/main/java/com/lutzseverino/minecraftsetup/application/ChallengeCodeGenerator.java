package com.lutzseverino.minecraftsetup.application;

import com.lutzseverino.minecraftsetup.domain.ChallengeCode;

public interface ChallengeCodeGenerator {
    ChallengeCode next();
}
