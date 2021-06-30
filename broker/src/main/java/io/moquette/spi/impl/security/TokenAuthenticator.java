package io.moquette.spi.impl.security;

import io.moquette.laoliu.MD5Utils;
import io.moquette.spi.security.IAuthenticator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenAuthenticator implements IAuthenticator, ITokenGenerator {
    @Override
    public boolean checkValid(String clientId, String username, byte[] password) {
        String s = new String(Base64.getDecoder().decode(password), StandardCharsets.ISO_8859_1);
        if (MD5Utils.validPassword(s, username)) {
            return true;
        }
        return false;
    }

    @Override
    public String generateToken(String username) {
        return MD5Utils.getEncryptedPwd(username);
    }
}
