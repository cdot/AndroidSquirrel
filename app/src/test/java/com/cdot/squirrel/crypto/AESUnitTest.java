package com.cdot.squirrel.crypto;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AESUnitTest {

    private static final String unicode_pass = "North △ West ◁ South ▽ East ▷";
    private static final String ascii_pass = "!£$%%&*)*_(_+)()*&}{:@<>?[];',./";
    private static final String long_pass = "abcedfghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123948576!£$%%&*)*_(_+)()*&}{:@<>?[];',./";

    private byte[] loadTestResource(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        // load from src/test/resources
        InputStream in = classLoader.getResourceAsStream(name);
        assertNotNull(in);
        ByteArrayOutputStream ouch = new ByteArrayOutputStream();
        int ch;
        try {
            while ((ch = in.read()) != -1)
                ouch.write(ch);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        return ouch.toByteArray();
    }

    private File writeTempFile(String name, byte[] data) {
        try {
            File tmpFile = File.createTempFile(name, "tmp");
            tmpFile.deleteOnExit();
            OutputStream fos = new FileOutputStream(tmpFile);
            fos.write(data);
            fos.close();
            return tmpFile;
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    private byte[] readTempFile(File file) {
        ByteArrayOutputStream ouch = new ByteArrayOutputStream();
        try {
            InputStream in = new FileInputStream(file);
            int ch;
            while ((ch = in.read()) != -1)
                ouch.write(ch);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
        return ouch.toByteArray();
    }

    private void encrypt_decrypt_bytes(Aes enc, Aes dec, String pass, int nBits) {
        byte[] ab = new byte[256];
        for (int i = 0; i < 256; i++)
            ab[i] = (byte) i;
        byte[] cipher = enc.encrypt(ab, pass, nBits);
        byte[] decipher = dec.decrypt(cipher, pass, nBits);
        assertEquals(ab.length, decipher.length);
        for (int i = 0; i < ab.length; i++)
            assertEquals(ab[i], decipher[i]);
    }

    private void encrypt_decrypt_string(Aes enc, Aes dec, String pass, int nBits) {
        String plain = unicode_pass;
        String cipher = enc.encrypt(plain, pass, nBits);
        String decipher = dec.decrypt(cipher, pass, nBits);
        assertEquals(plain, decipher);
    }

    private void encrypt_decrypt_large(Aes enc, Aes dec, String pass, int nBits) {
        byte[] plaintext = loadTestResource("large.json");

        byte[] ciphertext = enc.encrypt(plaintext, pass, nBits);
        File tf = writeTempFile("large", ciphertext);

        ciphertext = readTempFile(tf);
        byte[] nplaintext = dec.decrypt(ciphertext, pass, nBits);

        assertEquals(plaintext.length, nplaintext.length);
        for (int i = 0; i < nplaintext.length; i++)
            assertEquals("At " + i, plaintext[i], nplaintext[i]);
    }

    private void run_tests(Aes enc, Aes dec, String pass, int nBits) {
        encrypt_decrypt_bytes(enc, dec, pass, nBits);
        encrypt_decrypt_string(enc, dec, pass, nBits);
        encrypt_decrypt_large(enc, dec, pass, nBits);
    }

    @Test
    public void Reference_Reference() {
        run_tests(new AES_Reference(), new AES_Reference(), ascii_pass, 128);
        run_tests(new AES_Reference(), new AES_Reference(), unicode_pass, 192);
        run_tests(new AES_Reference(), new AES_Reference(), long_pass, 256);
    }

    @Test
    public void Old_Old() {
        // Old is not expected to be interoperable with Java or Reference, as it has several bugs
        // in the code. But it should round-trip
        run_tests(new AES_Old(), new AES_Old(), ascii_pass, 128);
        run_tests(new AES_Old(), new AES_Old(), long_pass, 192);
        run_tests(new AES_Old(), new AES_Old(), unicode_pass, 256);
    }

    @Test
    public void Java_Java() {
        run_tests(new AES_Java(), new AES_Java(), long_pass, 128);
        run_tests(new AES_Java(), new AES_Java(), unicode_pass, 192);
        run_tests(new AES_Java(), new AES_Java(), ascii_pass, 256);
    }

    @Test
    public void Java_Reference() {
        run_tests(new AES_Java(), new AES_Reference(), unicode_pass, 128);
        run_tests(new AES_Java(), new AES_Reference(), long_pass, 192);
        run_tests(new AES_Java(), new AES_Reference(), ascii_pass, 256);
    }

    @Test
    public void Reference_Java() {
        run_tests(new AES_Reference(), new AES_Java(), unicode_pass, 128);
        run_tests(new AES_Reference(), new AES_Java(), ascii_pass, 128);
        run_tests(new AES_Reference(), new AES_Java(), long_pass, 256);
    }
}
