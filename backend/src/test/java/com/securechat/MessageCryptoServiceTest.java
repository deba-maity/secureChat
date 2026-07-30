package com.securechat;

import com.securechat.message.EncryptedPayload;
import com.securechat.message.MessageCryptoService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCryptoServiceTest {
    @Test
    void encryptsWithoutStoringPlaintextAndDecryptsBack() {
        MessageCryptoService cryptoService = new MessageCryptoService("unit-test-secret-with-enough-entropy");

        EncryptedPayload encrypted = cryptoService.encrypt("privacy first");

        assertThat(encrypted.cipherText()).doesNotContain("privacy first");
        assertThat(encrypted.iv()).isNotBlank();
        assertThat(cryptoService.decrypt(encrypted.iv(), encrypted.cipherText())).isEqualTo("privacy first");
    }
}
