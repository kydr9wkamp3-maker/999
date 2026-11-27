package org.adamant.lab

/** JNI entry points for the versioned, lossless RGBA prototype container. */
object AdamantNative {
    init { System.loadLibrary("adamant") }

    external fun nativeEncode(rgba: ByteArray, width: Int, height: Int): ByteArray
    external fun nativeDecode(container: ByteArray): ByteArray

    /** Experimental tiled quantized base; exact=true adds a lossless XOR residual stream. */
    external fun nativeChimeraEncode(rgba: ByteArray, width: Int, height: Int, exact: Boolean, tolerance: Int): ByteArray
    external fun nativeChimeraDecode(container: ByteArray): ByteArray
}
