package com.cdot.squirrel.crypto;

/**
 * "Old" reference implementation of AES. Since replaced by AES_Reference - kept so we can
 * decrypt old data, which due to bugs can't be decrypted otherwise.
 */
public class AES_Old extends Aes {
    /**
     * Encrypt a Uint8Array using AES in Counter mode.
     *
     * @param   plaintext Source to be encrypted.
     * @param   password The password to use to generate a key.
     * @param   nBits Number of bits to be used in the key;
     * 128 / 192 / 256.
     * @return Encrypted data
     */
    byte[] encrypt(byte[] plaintext, String password, int nBits) {

        // block size fixed at 16 bytes / 128 bits (Nb=4) for AES
        int blockSize = 16;

        // use AES itself to encrypt password to get cipher key (using
        // plain password as source for key expansion) - gives us well
        // encrypted key (though hashed key might be preferred for prod'n use)
        int nBytes = nBits / 8; // no bytes in key (16/24/32)
        byte[] pwBytes = new byte[nBytes];
        int i;

        // use 1st 16/24/32 chars of password for key, zero padded
        for (i = 0; i < nBytes; i++) {
            if (i < password.length())
                pwBytes[i] = (byte)(password.charAt(i) & 255);
            else
                pwBytes[i] = 0;
        }

        // get 16-byte key
        byte[] key16 = Aes.cipher(pwBytes, Aes.keyExpansion(pwBytes));

        // expand key to 16/24/32 bytes long
        byte[] key = new byte[nBytes];
        //key = key.concat(key.slice(0, nBytes - 16));
        System.arraycopy(key16, 0, key, 0, 16);
        System.arraycopy(key16, 0, key, 16, nBytes - 16);

        // initialise 1st 8 bytes of counter block with nonce
        // (NIST SP800-38A §B.2): [0-1] = millisec,
        // [2-3] = random, [4-7] = seconds, together giving full sub-millisec
        // uniqueness up to Feb 2106
        byte[] counterBlock = new byte[16];
        int size = 8;

        // timestamp: milliseconds since 1-Jan-1970
        long nonce;// = System.currentTimeMillis();
        long nonceMs;// = nonce % 1000;
        long nonceSec;// = nonce / 1000;
        long nonceRnd;// = (long)(Math.random() * 0xffff);*/
        // was supposed to be for debugging, but production code never cleared this
        nonce = nonceMs = nonceSec = nonceRnd = 0;

        for (i = 0; i < 2; i++)
            counterBlock[i] = (byte)((nonceMs >>> i * 8) & 0xff);
        for (i = 0; i < 2; i++)
            counterBlock[i + 2] = (byte)((nonceRnd >>> i * 8) & 0xff);
        for (i = 0; i < 4; i++)
            counterBlock[i + 4] = (byte)((nonceSec >>> i * 8) & 0xff);

        // generate key schedule - an expansion of the key into distinct
        // Key Rounds for each round
        byte[][] keySchedule = Aes.keyExpansion(key);

        int blockCount = (int)Math.ceil(1.0 * plaintext.length / blockSize);

        // Ciphertext as an array of Uint8Array
        byte[][] ciphertxt = new byte[blockCount][];
        int b;

        for (b = 0; b < blockCount; b++) {
            // set counter (block #) in last 8 bytes of counter block
            // (leaving nonce in 1st 8 bytes). Done in two stages for
            // 32-bit ops: using two words allows us to go past 2^32 blocks (68GB)
            int c;
            for (c = 0; c < 4; c++)
                counterBlock[15 - c] = (byte)((b >>> c * 8) & 0xff);
            for (c = 0; c < 4; c++)
                counterBlock[15 - c - 4] = (byte)((b / 0x100000000L >>> c * 8));

            // CC: For compatibility with buggy JS used in rev 1 stores, we have to break
            // this algorithm. In the JS, the counterBlock was declared as a Uint8Array(8), which
            // meant that the last 8 bytes were always undefined (though no error is thrown when
            // assigning to them)
            for (c = 8; c < 16; c++)
                counterBlock[c] = 0;

            // encrypt counter block
            byte[] cipherCntr = Aes.cipher(counterBlock, keySchedule);

            // block size is reduced on final block
            int blockLength = b < blockCount - 1 ?
                    blockSize : (plaintext.length - 1) % blockSize + 1;
            byte[] cipherChar = new byte[blockLength];
            size += blockLength;

            // xor plaintext with ciphered counter char-by-char
            for (i = 0; i < blockLength; i++) {
                cipherChar[i] = (byte)(cipherCntr[i] ^ plaintext[b * blockSize + i]);
            }
            ciphertxt[b] = cipherChar;
        }

        byte[] ct = new byte[size];
        //ct.set(counterBlock);
        System.arraycopy(counterBlock, 0, ct, 0, 8/*counterBlock.length*/);

        int offset = 8;//counterBlock.length;
        for (b = 0; b < blockCount; b++) {
            //ct.set(ciphertxt[b], offset);
            System.arraycopy(ciphertxt[b], 0, ct, offset, ciphertxt[b].length);
            offset += ciphertxt[b].length;
        }
        return ct;
    }

    /**
     * Decrypt an array using AES in counter mode
     *
     * @param   ciphertext Source to be decrypted.
     * @param   password The password to use to generate a key.
     * @param   nBits Number of bits to be used in the key;
     * 128 / 192 / 256.
     * @return Decrypted data
     */
    byte[] decrypt(byte[] ciphertext, String password, int nBits) {
        int blockSize = 16;

        // use AES to encrypt password (mirroring encrypt routine)
        int nBytes = nBits / 8; // no bytes in key
        byte[] pwBytes = new byte[nBytes];
        int i;
        for (i = 0; i < nBytes; i++) {
            if (i < password.length())
                pwBytes[i] = (byte)(password.charAt(i) & 255);
            else
                pwBytes[i] = 0;
        }

        // get 16-byte key
        byte[] key16 = Aes.cipher(pwBytes, Aes.keyExpansion(pwBytes));

        // expand key to 16/24/32 bytes long
        byte[] key = new byte[nBytes];
        //key = key.concat(key.slice(0, nBytes - 16));
        System.arraycopy(key16, 0, key, 0, 16);
        System.arraycopy(key16, 0, key, 16, nBytes - 16);

        // recover nonce from 1st 8 bytes of ciphertext
        byte[] counterBlock = new byte[16];
        for (i = 0; i < 8; i++)
            counterBlock[i] = ciphertext[i];

        // generate key schedule
        byte[][] keySchedule = Aes.keyExpansion(key);

        // separate ciphertext into blocks (skipping past initial 8 bytes)
        int nBlocks = (int)(Math.ceil((ciphertext.length - 8.0) / blockSize));
        byte[][] ct = new byte[nBlocks][];
        int offset = 8;
        int b;
        for (b = 0; b < nBlocks; b++) {
            // ct[b] = ciphertext.subarray(offset, offset + blockSize);
            int avail = Math.min(blockSize, ciphertext.length - offset);
            ct[b] = new byte[avail];
            System.arraycopy(ciphertext, offset, ct[b], 0, avail);

            offset += blockSize;
        }
        // ct is now array of block-length Uint8Array

        // plaintext will get generated block-by-block into array of
        // block-length Uint8Arrays
        byte[][] plaintxt = new byte[nBlocks][];
        int size = 0;

        for (b = 0; b < nBlocks; b++) {
            // set counter (block #) in last 8 bytes of counter block
            // (leaving nonce in 1st 8 bytes)
            int c;
            for (c = 0; c < 4; c++)
                counterBlock[15 - c] = (byte)(((b) >>> c * 8) & 0xff);
            for (c = 0; c < 4; c++) {
                counterBlock[15 - c - 4] =
                        (byte)((((b + 1) / 0x100000000L - 1) >>> c * 8) & 0xff);
            }

            // CC: For compatibility with buggy JS used in rev 1 stores, we have to break
            // this algorithm. In the JS, the counterBlock was declared as a Uint8Array(8), which
            // meant that the last 8 bytes were always undefined (though no error is thrown when
            // assigning to them)
            for (c = 8; c < 16; c++)
                counterBlock[c] = 0;

            // encrypt counter block
            byte[] cipherCntr = Aes.cipher(counterBlock, keySchedule);
            int blen = ct[b].length;
            byte[] plaintxtByte = new byte[blen];
            for (i = 0; i < blen; i++) {
                // xor plaintxt with ciphered counter byte-by-byte
                plaintxtByte[i] = (byte)(cipherCntr[i] ^ ct[b][i]);
            }
            plaintxt[b] = plaintxtByte;
            size += blen;
        }

        byte[] pt = new byte[size];
        offset = 0;
        for (b = 0; b < nBlocks; b++) {
            //pt.set(plaintxt[b], offset);
            System.arraycopy(plaintxt[b], 0, pt, offset, plaintxt[b].length);
            offset += plaintxt[b].length;
        }
        return pt;
    }
}
