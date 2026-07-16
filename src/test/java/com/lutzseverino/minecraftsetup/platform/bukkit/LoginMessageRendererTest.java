package com.lutzseverino.minecraftsetup.platform.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class LoginMessageRendererTest {
  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final PlainTextComponentSerializer PLAIN_TEXT =
      PlainTextComponentSerializer.plainText();

  @Test
  void rendersMiniMessageAroundCodePlaceholder() {
    Component actual =
        LoginMessageRenderer.renderTemplate(
            "<yellow>Enter <bold>{code}</bold></yellow>", "ABCD-EFGH-JKMP-QRST");

    assertEquals(
        MINI_MESSAGE.deserialize("<yellow>Enter <bold>ABCD-EFGH-JKMP-QRST</bold></yellow>"),
        actual);
  }

  @Test
  void insertsCodeAsTextInsteadOfMiniMessageMarkup() {
    Component actual =
        LoginMessageRenderer.renderTemplate("Code: <green>{code}</green>", "<red>unsafe</red>");

    assertEquals("Code: <red>unsafe</red>", PLAIN_TEXT.serialize(actual));
  }
}
