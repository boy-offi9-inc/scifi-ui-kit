package com.boyoffi9.scifikit.core

import kotlin.math.abs
import kotlin.math.max

internal class TextDecoder {

    var chars = CharArray(0)
        private set

    var cursorIndex = -1
        private set

    private var ranks = IntArray(0)
    private var ranksText = ""
    private var ranksOrder = DecodeOrder.LeftToRight
    private var ranksValid = false

    fun fill(text: String, config: DecodeConfig, startMs: Long, nowMs: Long, animate: Boolean): Int {
        val length = text.length
        if (chars.size < length) chars = CharArray(length)
        text.toCharArray(chars, 0, 0, length)
        cursorIndex = -1
        if (!animate || length == 0) return length

        val elapsed = nowMs - startMs
        val decoding = config.durationMs > 0L && elapsed < config.durationMs
        val shimmerLimit = (config.shimmer * SHIMMER_PER_MILLE).toInt()
        if (!decoding && shimmerLimit <= 0) return length

        ensureRanks(text, config.order)
        val charset = config.charset.ifEmpty { DecodeCharsets.Default }
        val bucket = (nowMs / max(1L, config.swapMs)).toInt()
        val resolved = if (decoding) (length * (elapsed.toFloat() / config.durationMs)).toInt() else length

        for (i in 0 until length) {
            if (chars[i] == ' ') continue
            if (ranks[i] >= resolved) {
                chars[i] = if (config.scramble) charset[hashOf(i, bucket) % charset.length] else ' '
            } else if (shimmerLimit > 0 && hashOf(i, bucket + SHIMMER_SALT) % 1000 < shimmerLimit) {
                chars[i] = charset[hashOf(bucket, i + SHIMMER_SALT) % charset.length]
            }
        }

        if (decoding && config.cursor && resolved < length) {
            for (i in 0 until length) {
                if (ranks[i] == resolved) {
                    cursorIndex = i
                    break
                }
            }
        }
        return length
    }

    private fun ensureRanks(text: String, order: DecodeOrder) {
        if (ranksValid && order == ranksOrder && text == ranksText) return
        val length = text.length
        if (ranks.size < length) ranks = IntArray(length)
        when (order) {
            DecodeOrder.LeftToRight -> for (i in 0 until length) ranks[i] = i
            DecodeOrder.RightToLeft -> for (i in 0 until length) ranks[i] = length - 1 - i
            DecodeOrder.CenterOut -> rankBy(length) { abs(2 * it - (length - 1)) }
            DecodeOrder.Random -> rankBy(length) { hashOf(it, RANDOM_SALT) }
        }
        ranksText = text
        ranksOrder = order
        ranksValid = true
    }

    private inline fun rankBy(length: Int, key: (Int) -> Int) {
        val keys = IntArray(length) { key(it) }
        for (i in 0 until length) {
            var rank = 0
            for (j in 0 until length) {
                if (keys[j] < keys[i] || (keys[j] == keys[i] && j < i)) rank++
            }
            ranks[i] = rank
        }
    }

    private companion object {
        const val SHIMMER_PER_MILLE = 40
        const val SHIMMER_SALT = 7919
        const val RANDOM_SALT = 4242
    }
}
