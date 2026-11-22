package com.mp34dl.parser

import com.mp34dl.model.AvailableMediaOptions
import com.mp34dl.model.MediaFormatOption
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parser ketat untuk metadata JSON `yt-dlp` (`--dump-json`).
 * Hanya memasukkan opsi yang benar-benar ada pada data JSON; tidak ada placeholder.
 */
object FormatFilterParser {

    private val audioExtensions = setOf("mp3", "aac", "flac", "m4a")
    private val audioBitrateLabels = setOf("128k", "320k", "max")

    fun parse(json: String): AvailableMediaOptions {
        if (json.isBlank()) return AvailableMediaOptions()
        return try {
            parse(JSONObject(json))
        } catch (e: Exception) {
            AvailableMediaOptions()
        }
    }

    fun parse(root: JSONObject): AvailableMediaOptions {
        val formats = root.optJSONArray("formats") ?: JSONArray()
        val videoOptions = linkedSetOf<MediaFormatOption>()
        val audioOptions = linkedSetOf<MediaFormatOption>()

        for (i in 0 until formats.length()) {
            val f = formats.optJSONObject(i) ?: continue
            val formatId = f.optString("format_id").takeIf { it.isNotBlank() } ?: continue

            val vcodec = f.optString("vcodec").takeIf { it.isNotBlank() && it != "none" }
            val acodec = f.optString("acodec").takeIf { it.isNotBlank() && it != "none" }
            val height = f.optIntOrNull("height")
            val fps = f.optIntOrNull("fps")
            val ext = f.optString("ext").takeIf { it.isNotBlank() }
            val filesize = f.optLongOrNull("filesize") ?: f.optLongOrNull("filesize_approx")
            val tbr = f.optDoubleOrNull("tbr")
            val abr = f.optDoubleOrNull("abr")
            val dynamicRange = f.optString("dynamic_range").takeIf { it.isNotBlank() }
            val isHdr = dynamicRange?.contains("HDR", ignoreCase = true) == true ||
                f.optBoolean("is_hdr", false)

            val hasVideo = vcodec != null && height != null && height > 0
            val hasAudio = acodec != null

            if (hasVideo) {
                videoOptions.add(
                    MediaFormatOption(
                        formatId = formatId,
                        height = height,
                        fps = fps,
                        dynamicRange = dynamicRange,
                        isHdr = isHdr,
                        vcodec = vcodec,
                        acodec = acodec,
                        filesizeApprox = filesize,
                        ext = ext,
                        hasAudio = hasAudio
                    )
                )
            }

            if (!hasVideo && hasAudio) {
                // Audio-only entry: filter to supported ext + bitrate labels that actually exist.
                val audioExt = ext?.lowercase()
                val bitrateKbps = abr?.toInt() ?: tbr?.toInt()
                val bitrateLabel = bitrateKbps?.let { mapBitrateLabel(it) }

                val extSupported = audioExt != null && audioExt in audioExtensions
                val labelSupported = bitrateLabel != null && bitrateLabel in audioBitrateLabels
                val isBest = formatId.equals("bestaudio", ignoreCase = true) ||
                    f.optString("format").equals("bestaudio", ignoreCase = true) ||
                    f.optString("quality").equals("best", ignoreCase = true)

                if (extSupported || isBest || labelSupported) {
                    val lossless = audioExt in setOf("flac", "m4a") && (bitrateKbps == null || bitrateKbps >= 320)
                    audioOptions.add(
                        MediaFormatOption(
                            formatId = formatId,
                            acodec = acodec,
                            filesizeApprox = filesize,
                            ext = audioExt ?: ext,
                            bitrateKbps = bitrateKbps,
                            bitrateLabel = bitrateLabel ?: if (isBest) "best" else null,
                            isLosslessOriginal = lossless,
                            isAudioOnly = true
                        )
                    )
                }
            }
        }

        // Deduplicate video by (height, fps, isHdr) keeping first occurrence.
        val uniqueVideo = videoOptions.distinctBy { Triple(it.height, it.fps, it.isHdr) }
        // Deduplicate audio by (ext, bitrateLabel) keeping first occurrence.
        val uniqueAudio = audioOptions.distinctBy { Pair(it.ext, it.bitrateLabel ?: it.bitrateKbps) }

        return AvailableMediaOptions(
            videoOptions = uniqueVideo,
            audioOptions = uniqueAudio,
            title = root.optString("title").takeIf { it.isNotBlank() },
            durationSeconds = root.optLongOrNull("duration"),
            sourceUrl = root.optString("webpage_url").takeIf { it.isNotBlank() }
                ?: root.optString("original_url").takeIf { it.isNotBlank() }
                ?: root.optString("url").takeIf { it.isNotBlank() }
        )
    }

    private fun mapBitrateLabel(kbps: Int): String? = when {
        kbps >= 300 -> "320k"
        kbps >= 100 -> "128k"
        else -> null
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val v = opt(key)
        return when (v) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull()
            else -> null
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        val v = opt(key)
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull()
            else -> null
        }
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = opt(key)
        return when (v) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }
}
