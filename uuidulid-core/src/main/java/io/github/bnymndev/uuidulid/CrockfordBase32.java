package io.github.bnymndev.uuidulid;

/**
 * Crockford Base32 codec for the 128-bit ULID value space.
 *
 * <p>The alphabet is {@code 0123456789ABCDEFGHJKMNPQRSTVWXYZ}: the digits plus the uppercase
 * letters with {@code I}, {@code L}, {@code O} and {@code U} removed. Because the characters
 * are in ascending ASCII order, the lexicographic order of an encoded ULID matches the
 * numeric order of the underlying unsigned 128-bit value.
 *
 * <p>Decoding is lenient in the way the Crockford specification suggests: lowercase letters are
 * accepted, {@code I}/{@code i}/{@code L}/{@code l} decode as {@code 1} and {@code O}/{@code o}
 * decodes as {@code 0}. Encoding always produces the canonical uppercase form.
 */
final class CrockfordBase32 {

    /** Canonical encoding alphabet. */
    private static final char[] ENCODE = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /** Lookup table mapping a US-ASCII character to its 5-bit value, or {@code -1} if invalid. */
    private static final byte[] DECODE = new byte[128];

    static {
        java.util.Arrays.fill(DECODE, (byte) -1);
        for (int i = 0; i < ENCODE.length; i++) {
            char c = ENCODE[i];
            DECODE[c] = (byte) i;
            DECODE[Character.toLowerCase(c)] = (byte) i;
        }
        // Crockford's confusable-character aliases.
        DECODE['I'] = DECODE['i'] = DECODE['L'] = DECODE['l'] = DECODE['1'];
        DECODE['O'] = DECODE['o'] = DECODE['0'];
    }

    private CrockfordBase32() {
    }

    /**
     * Encodes a 128-bit value as 26 Crockford Base32 characters.
     *
     * <p>26 characters carry 130 bits, so the leading character only encodes the three most
     * significant bits of the value and never exceeds {@code '7'}.
     *
     * @param msb the most significant 64 bits
     * @param lsb the least significant 64 bits
     * @return the canonical 26-character uppercase form
     */
    static String encode(long msb, long lsb) {
        char[] out = new char[Ulid.LENGTH];

        out[0] = ENCODE[(int) ((msb >>> 61) & 0x07L)];
        for (int i = 1; i <= 12; i++) {
            out[i] = ENCODE[(int) ((msb >>> (61 - 5 * i)) & 0x1FL)];
        }
        // Character 13 straddles the boundary: 1 bit from msb, 4 bits from lsb.
        out[13] = ENCODE[(int) (((msb & 0x01L) << 4) | (lsb >>> 60))];
        for (int i = 14; i <= 25; i++) {
            out[i] = ENCODE[(int) ((lsb >>> (5 * (25 - i))) & 0x1FL)];
        }
        return new String(out);
    }

    /**
     * Decodes a single character to its 5-bit value.
     *
     * @param c the character
     * @return the value in {@code [0, 31]}, or {@code -1} if the character is not part of the
     *         Crockford alphabet
     */
    static int decodeChar(char c) {
        return c < 128 ? DECODE[c] : -1;
    }

    /**
     * Decodes 26 Crockford Base32 characters into the two halves of a 128-bit value.
     *
     * @param text the text to decode
     * @param out  a two-element array receiving {@code [msb, lsb]}
     * @return {@code true} if {@code text} is a valid ULID string and {@code out} was written
     */
    static boolean decode(CharSequence text, long[] out) {
        if (text == null || text.length() != Ulid.LENGTH) {
            return false;
        }
        // 26 characters hold 130 bits; the top 2 bits must be zero for the value to fit in 128.
        int first = decodeChar(text.charAt(0));
        if (first < 0 || first > 7) {
            return false;
        }

        long msb = (long) first << 61;
        for (int i = 1; i <= 12; i++) {
            int v = decodeChar(text.charAt(i));
            if (v < 0) {
                return false;
            }
            msb |= (long) v << (61 - 5 * i);
        }

        int mid = decodeChar(text.charAt(13));
        if (mid < 0) {
            return false;
        }
        msb |= (long) (mid >>> 4);
        long lsb = (long) (mid & 0x0F) << 60;

        for (int i = 14; i <= 25; i++) {
            int v = decodeChar(text.charAt(i));
            if (v < 0) {
                return false;
            }
            lsb |= (long) v << (5 * (25 - i));
        }

        out[0] = msb;
        out[1] = lsb;
        return true;
    }
}
