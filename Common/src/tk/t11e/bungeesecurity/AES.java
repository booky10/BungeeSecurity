package tk.t11e.bungeesecurity;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public final class AES {

    private static SecretKeySpec secretKey;

    private void setKey(String secret) {
        MessageDigest sha;
        try {
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException exception) {
            exception.printStackTrace();
        }
    }

    public final String encrypt(String text, String key) {
        try {
            setKey(key);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (InvalidKeyException | NoSuchAlgorithmException
                | NoSuchPaddingException | IllegalBlockSizeException
                | BadPaddingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public final String decrypt(String decrypted, String key) {
        try {
            setKey(key);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(decrypted)));
        } catch (InvalidKeyException | NoSuchAlgorithmException
                | NoSuchPaddingException | IllegalBlockSizeException
                | BadPaddingException exception) {
            throw new IllegalStateException(exception);
        }
    }
}