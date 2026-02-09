package com.kalynx.simplyabinaryserializer;

/**
 * Fast byte array writer that writes directly without stream overhead.
 * Avoids DataOutputStream method call overhead for hot paths.
 */
class FastByteWriter {
    private byte[] buf;
    private int pos;

    void setBuffer(byte[] buffer) {
        this.buf = buffer;
        this.pos = 0;
    }

    void setPosition(int position) {
        this.pos = position;
    }

    int getPosition() {
        return pos;
    }

    void writeByte(int v) {
        buf[pos++] = (byte) v;
    }

    // Inline varint writing to avoid method call overhead
    void writeVarint(int value) {
        if (value < 128) {
            buf[pos++] = (byte) value;
        } else if (value < 16384) {
            buf[pos++] = (byte) ((value & 0x7F) | 0x80);
            buf[pos++] = (byte) (value >>> 7);
        } else {
            while (value > 127) {
                buf[pos++] = (byte) ((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            buf[pos++] = (byte) value;
        }
    }

    void writeShort(int v) {
        buf[pos++] = (byte) (v >>> 8);
        buf[pos++] = (byte) v;
    }

    void writeInt(int v) {
        buf[pos++] = (byte) (v >>> 24);
        buf[pos++] = (byte) (v >>> 16);
        buf[pos++] = (byte) (v >>> 8);
        buf[pos++] = (byte) v;
    }

    void writeLong(long v) {
        buf[pos++] = (byte) (v >>> 56);
        buf[pos++] = (byte) (v >>> 48);
        buf[pos++] = (byte) (v >>> 40);
        buf[pos++] = (byte) (v >>> 32);
        buf[pos++] = (byte) (v >>> 24);
        buf[pos++] = (byte) (v >>> 16);
        buf[pos++] = (byte) (v >>> 8);
        buf[pos++] = (byte) v;
    }

    void writeBoolean(boolean v) {
        buf[pos++] = (byte) (v ? 1 : 0);
    }

    void writeDouble(double v) {
        writeLong(Double.doubleToLongBits(v));
    }

    void writeFloat(float v) {
        writeInt(Float.floatToIntBits(v));
    }

    void writeBytes(byte[] src, int len) {
        System.arraycopy(src, 0, buf, pos, len);
        pos += len;
    }

    void writeString(byte[] strBytes) {
        writeShort(strBytes.length);
        System.arraycopy(strBytes, 0, buf, pos, strBytes.length);
        pos += strBytes.length;
    }
}