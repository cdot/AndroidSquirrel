package com.cdot.squirrel.crypto;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implementation of AES encryption using system libraries.
 */
public class AES_SYS extends Aes {

    @Override
    byte[] encrypt(byte[] plaintext, String pass, int nBits) {
        try {
            byte[] pwBytes = makeKey(pass, nBits);
            SecretKey key = new SecretKeySpec(pwBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            // Construct new IV
            byte[] iv = getIVBytes(cipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV());
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

            byte[] ciphertext = cipher.doFinal(plaintext);
            return makeFinal(iv, ciphertext);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException
                | NoSuchAlgorithmException | InvalidParameterSpecException | BadPaddingException
                | IllegalBlockSizeException e) {
            e.printStackTrace();
            //assertTrue(e.getMessage(), false);
            return null;
        }
    }

    @Override
    byte[] decrypt(byte[] ciphertext, String pass, int nBits) {
        try {
            byte[] pwBytes = makeKey(pass, nBits);
            SecretKey key = new SecretKeySpec(pwBytes, "AES");
            byte[] ivBytes = getIVBytes(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivBytes));
            return cipher.doFinal(getDataBytes(ciphertext));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException
                | NoSuchPaddingException
                | NoSuchAlgorithmException e) {
            e.printStackTrace();
            //assertTrue(e.getMessage(), false);
            return null;
        }
    }
}
