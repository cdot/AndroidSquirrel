package com.cdot.squirrel.crypto;

import java.util.Date;

/**
 * Reference implementation of counter-mode AES_Reference, based on
 * AES_Reference counter-mode (CTR) implementation in JavaScript (c) Chris Veness 2005-2019  MIT Licence
 * www.movable-type.co.uk/scripts/aes.html
 *
 * This directly mirrors the Javascript implementation, and is used in testing to ensure
 * interoperability with the Android system implementation (and hence with Javascript)
 */
public class AES_Reference extends Aes {

    /**
     * NIST SP 800-38A sets out recommendations for block cipher modes of operation in terms of byte
     * operations. This implements the §6.5 Counter Mode (CTR).
     * <p>
     * Oⱼ = CIPHₖ(Tⱼ)      for j = 1, 2 … n
     * Cⱼ = Pⱼ ⊕ Oⱼ        for j = 1, 2 … n-1
     * C*ₙ = P* ⊕ MSBᵤ(Oₙ) final (partial?) block
     * where CIPHₖ is the forward cipher function, O output blocks, P intext blocks, C
     * outtext blocks

     * @param intext    - Plaintext to be encrypted/ Ciphertext to be decrypted, as byte array.
     * @param key          - Key to be used to encrypt/decrypt.
     * @param counterBlock - Initial 16-byte CTR counter block (with nonce & 0 counter).
     * @return Ciphertext as byte array.
     */
    private static byte[] nist(byte[] intext, byte[] key, byte[] counterBlock) {
        // generate key schedule - an expansion of the key into distinct Key Rounds for each round
        byte[][] keySchedule = keyExpansion(key);

        int blockCount = (int) Math.ceil(1.0 * intext.length / BLOCK_SIZE);
        byte[] outtext = new byte[intext.length];

        for (int b = 0; b < blockCount; b++) {
            // ---- encrypt/decrypt counter block; Oⱼ = CIPHₖ(Tⱼ) ----
            byte[] cipherCntr = cipher(counterBlock, keySchedule);

            // block size is reduced on final block
            int blockLength = b < blockCount - 1 ? BLOCK_SIZE : (intext.length - 1) % BLOCK_SIZE + 1;

            // ---- xor plaintext with ciphered counter byte-by-byte; Cⱼ = Pⱼ ⊕ Oⱼ ----
            for (int i = 0; i < blockLength; i++) {
                outtext[b * BLOCK_SIZE + i] = (byte) (cipherCntr[i] ^ intext[b * BLOCK_SIZE + i]);
            }

            // increment counter block (counter in 2nd 8 bytes of counter block, big-endian)
            // &FF to convert unsigned byte to int
            int t = (counterBlock[BLOCK_SIZE - 1] & 0xFF) + 1;
            // and propagate carry digits
            for (int i = BLOCK_SIZE - 1; i >= 8; i--) {
                counterBlock[i] = (byte) (t & 0xFF);
                t = counterBlock[i - 1] + ((t >> 8) & 1);
            }
        }

        return outtext;
    }

    @Override
    byte[] encrypt(byte[] plaintextBytes, String password, int nBits) {
        byte[] key = makeKey(password, nBits);

        // initialise 1st 8 bytes of counter block with nonce
        // (NIST SP800-38A §B.2): [0-1] = millisec,
        // [2-3] = random, [4-7] = seconds, together giving full sub-millisec
        // timestamp: milliseconds since 1-Jan-1970
        long timestamp = new Date().getTime();
        long nonceMs = timestamp % 1000;
        long nonceSec = (long) Math.floor(timestamp / 1000.0);
        long nonceRnd = (long) (Math.random() * 0xffff);
        // DEBUGGING ONLY!
        //nonceMs = nonceSec = nonceRnd = 0x55555555;

        byte[] iv = new byte[]{
                // 16-byte array; blocksize is fixed at 16 for AES_Reference
                (byte) (nonceMs & 0xff), (byte) (nonceMs >>> 8 & 0xff),
                (byte) (nonceRnd & 0xff), (byte) ((nonceRnd >>> 8) & 0xff),
                (byte) (nonceSec & 0xff), (byte) ((nonceSec >>> 8) & 0xff),
                (byte) ((nonceSec >>> 16) & 0xff), (byte) ((nonceSec >>> 24) & 0xff),
                0, 0, 0, 0, 0, 0, 0, 0
        };

        byte[] ciphertextBytes = nist(plaintextBytes, key, iv);
        return makeFinal(iv, ciphertextBytes);
    }

    @Override
    byte[] decrypt(byte[] ciphertextBytes, String password, int nBits) {
        byte[] key = makeKey(password, nBits);
        byte[] counterBlock = getIVBytes(ciphertextBytes);
        ciphertextBytes = getDataBytes(ciphertextBytes);
        return nist(ciphertextBytes, key, counterBlock);
    }
}

