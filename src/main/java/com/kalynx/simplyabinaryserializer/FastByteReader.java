package com.kalynx.simplyabinaryserializer;

/**
 * Fast byte array reader that reads directly without stream overhead.
 * Avoids DataInputStream method call overhead for hot paths.
 */
class FastByteReader {
    private byte[] buf;
    private int pos;

    void setData(byte[] data) {
        this.buf = data;
        this.pos = 0;
    }

    byte readByte() {
        return buf[pos++];
    }

    int readInt() {
        return ((buf[pos++] & 0xFF) << 24) |
                ((buf[pos++] & 0xFF) << 16) |
                ((buf[pos++] & 0xFF) << 8) |
                (buf[pos++] & 0xFF);
    }

    long readLong() {
        return ((long)(buf[pos++] & 0xFF) << 56) |
                ((long)(buf[pos++] & 0xFF) << 48) |
                ((long)(buf[pos++] & 0xFF) << 40) |
                ((long)(buf[pos++] & 0xFF) << 32) |
                ((long)(buf[pos++] & 0xFF) << 24) |
                ((long)(buf[pos++] & 0xFF) << 16) |
                ((long)(buf[pos++] & 0xFF) << 8) |
                (buf[pos++] & 0xFF);
    }

    boolean readBoolean() {
        return buf[pos++] != 0;
    }

    double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    short readShort() {
        return (short)(((buf[pos++] & 0xFF) << 8) | (buf[pos++] & 0xFF));
    }

    // Read varint (variable-length integer encoding)
    int readVarint() {
        int value = 0;
        int shift = 0;
        byte b;

        do {
            b = buf[pos++];
            value |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);

        return value;
    }

    void readFully(byte[] dest, int off, int len) {
        System.arraycopy(buf, pos, dest, off, len);
        pos += len;
    }

    byte[] readBytes(int len) {
        byte[] result = new byte[len];
        System.arraycopy(buf, pos, result, 0, len);
        pos += len;
        return result;
    }
}