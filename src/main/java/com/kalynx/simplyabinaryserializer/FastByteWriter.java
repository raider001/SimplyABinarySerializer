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

    void reset(int capacity) {
        if (buf == null || buf.length < capacity) {
            buf = new byte[capacity];
        }
        pos = 0;
    }

    byte[] toByteArray() {
        byte[] result = new byte[pos];
        System.arraycopy(buf, 0, result, 0, pos);
        return result;
    }

    void setPosition(int position) {
        this.pos = position;
    }

    int getPosition() {
        return pos;
    }

    byte[] getBuffer() {
        return buf;
    }

    void writeByte(int v) {
        ensureCapacity(1);
        buf[pos++] = (byte) v;
    }

    // Inline varint writing to avoid method call overhead
    void writeVarint(int value) {
        ensureCapacity(5); // Worst case: 5 bytes for int32
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
        ensureCapacity(2);
        buf[pos++] = (byte) (v >>> 8);
        buf[pos++] = (byte) v;
    }

    void writeShort(short v) {
        writeShort((int) v);
    }

    void writeInt(int v) {
        ensureCapacity(4);
        buf[pos++] = (byte) (v >>> 24);
        buf[pos++] = (byte) (v >>> 16);
        buf[pos++] = (byte) (v >>> 8);
        buf[pos++] = (byte) v;
    }

    void writeLong(long v) {
        ensureCapacity(8);
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
        ensureCapacity(1);
        buf[pos++] = (byte) (v ? 1 : 0);
    }

    void writeDouble(double v) {
        writeLong(Double.doubleToLongBits(v));
    }

    void writeFloat(float v) {
        writeInt(Float.floatToIntBits(v));
    }

    void writeBytes(byte[] src, int len) {
        ensureCapacity(len);
        System.arraycopy(src, 0, buf, pos, len);
        pos += len;
    }

    void writeBytes(byte[] src) {
        ensureCapacity(src.length);
        System.arraycopy(src, 0, buf, pos, src.length);
        pos += src.length;
    }

    private void ensureCapacity(int additional) {
        if (pos + additional > buf.length) {
            byte[] newBuf = new byte[Math.max(buf.length * 2, pos + additional)];
            System.arraycopy(buf, 0, newBuf, 0, pos);
            buf = newBuf;
        }
    }

    void writeString(byte[] strBytes) {
        writeShort(strBytes.length);
        System.arraycopy(strBytes, 0, buf, pos, strBytes.length);
        pos += strBytes.length;
    }
}