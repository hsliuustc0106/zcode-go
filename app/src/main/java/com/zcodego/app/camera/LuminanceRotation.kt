package com.zcodego.app.camera

internal data class RotatedLuminance(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

internal object LuminanceRotation {
    fun rotate(src: ByteArray, width: Int, height: Int, degrees: Int): RotatedLuminance {
        require(width > 0 && height > 0 && src.size == width * height) {
            "luminance size does not match dimensions"
        }
        val normalized = ((degrees % 360) + 360) % 360
        return when (normalized) {
            0 -> RotatedLuminance(src, width, height)
            90 -> rotate90(src, width, height)
            180 -> rotate180(src, width, height)
            270 -> rotate270(src, width, height)
            else -> RotatedLuminance(src, width, height)
        }
    }

    private fun rotate90(src: ByteArray, width: Int, height: Int): RotatedLuminance {
        val newWidth = height
        val dst = ByteArray(src.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val nx = height - 1 - y
                val ny = x
                dst[ny * newWidth + nx] = src[y * width + x]
            }
        }
        return RotatedLuminance(dst, newWidth, width)
    }

    private fun rotate180(src: ByteArray, width: Int, height: Int): RotatedLuminance {
        val dst = ByteArray(src.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val nx = width - 1 - x
                val ny = height - 1 - y
                dst[ny * width + nx] = src[y * width + x]
            }
        }
        return RotatedLuminance(dst, width, height)
    }

    private fun rotate270(src: ByteArray, width: Int, height: Int): RotatedLuminance {
        val newWidth = height
        val dst = ByteArray(src.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val nx = y
                val ny = width - 1 - x
                dst[ny * newWidth + nx] = src[y * width + x]
            }
        }
        return RotatedLuminance(dst, newWidth, width)
    }
}
