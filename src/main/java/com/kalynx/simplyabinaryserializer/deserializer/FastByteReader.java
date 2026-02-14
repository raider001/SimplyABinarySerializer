package com.kalynx.simplyabinaryserializer.deserializer;

/**
 * Fast byte array reader that reads directly without stream overhead.
 * Avoids DataInputStream method call overhead for hot paths.
 */
public final class FastByteReader {
    private byte[] buf;
    private int pos;

    public final void setData(byte[] data) {
        this.buf = data;
        this.pos = 0;
    }

    public final byte readByte() {
        return buf[pos++];
    }

    public final int readInt() {
        return ((buf[pos++] & 0xFF) << 24) |
                ((buf[pos++] & 0xFF) << 16) |
                ((buf[pos++] & 0xFF) << 8) |
                (buf[pos++] & 0xFF);
    }

    public final long readLong() {
        return ((long)(buf[pos++] & 0xFF) << 56) |
                ((long)(buf[pos++] & 0xFF) << 48) |
                ((long)(buf[pos++] & 0xFF) << 40) |
                ((long)(buf[pos++] & 0xFF) << 32) |
                ((long)(buf[pos++] & 0xFF) << 24) |
                ((long)(buf[pos++] & 0xFF) << 16) |
                ((long)(buf[pos++] & 0xFF) << 8) |
                (buf[pos++] & 0xFF);
    }

    public final boolean readBoolean() {
        return buf[pos++] != 0;
    }

    public final double readDouble() {
        return Double.longBitsToDouble(readLong());
    }

    public final float readFloat() {
        return Float.intBitsToFloat(readInt());
    }

    public final short readShort() {
        return (short)(((buf[pos++] & 0xFF) << 8) | (buf[pos++] & 0xFF));
    }

    // Read varint (variable-length integer encoding)
    public final int readVarint() {
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

    public final void readFully(byte[] dest, int off, int len) {
        System.arraycopy(buf, pos, dest, off, len);
        pos += len;
    }

    public final byte[] readBytes(int len) {
        byte[] result = new byte[len];
        System.arraycopy(buf, pos, result, 0, len);
        pos += len;
        return result;
    }

    // Optimized method to read multiple integers at once - reduces method call overhead
    public final void readInts(int[] dest, int count) {
        for (int i = 0; i < count; i++) {
            dest[i] = ((buf[pos++] & 0xFF) << 24) |
                      ((buf[pos++] & 0xFF) << 16) |
                      ((buf[pos++] & 0xFF) << 8) |
                      (buf[pos++] & 0xFF);
        }
    }

    public final int getPosition() {
        return pos;
    }

    public final byte[] getBuffer() {
        return buf;
    }
}