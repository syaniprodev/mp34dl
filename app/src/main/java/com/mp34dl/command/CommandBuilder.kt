package com.mp34dl.command

import com.mp34dl.model.MediaFormatOption
import com.yausername.youtubedl_android.YoutubeDLRequest

/**
 * Kontainer output yang didukung untuk proses remux video tanpa re-encode.
 */
enum class VideoContainer(val ext: String) {
    MP4("mp4"),
    MKV("mkv");

    companion object {
        fun fromExt(ext: String?): VideoContainer {
            return when (ext?.trim()?.lowercase()?.removePrefix(".")) {
                "mkv" -> MKV
                else -> MP4
            }
        }
    }
}

/**
 * Representasi argumen CLI terstruktur sebelum dimasukkan ke [YoutubeDLRequest],
 * memudahkan pengujian unit tanpa ketergantungan runtime Android.
 */
data class CommandOption(
    val flag: String,
    val value: String? = null
)

/**
 * Membangun [YoutubeDLRequest] untuk `youtubedl-android` (`yt-dlp` + `FFmpeg`)
 * berdasarkan opsi stream yang dipilih pengguna, lengkap dengan fallback aman.
 */
object CommandBuilder {

    private val convertibleAudioFormats = setOf("mp3", "aac", "flac")
    private val passthroughAudioFormats = setOf("best", "max", "original")

    /**
     * Membangun [YoutubeDLRequest] siap eksekusi dari pilihan pengguna.
     *
     * Mode yang didukung:
     * 1. Video sesuai resolusi/FPS/HDR + audio terbaik, di-remux ke `.mp4`/`.mkv`.
     * 2. Audio `best`/`max` tanpa re-encode.
     * 3. Audio konversi `.mp3`/`.aac`/`.flac` dengan bitrate terpilih (`128k`, `320k`, `max`).
     */
    fun buildRequest(
        url: String,
        selectedOption: MediaFormatOption?,
        targetContainer: VideoContainer = VideoContainer.MP4,
        outputAudioFormat: String? = null,
        outputBitrate: String? = null,
        outputTemplate: String? = null,
        noPlaylist: Boolean = true
    ): YoutubeDLRequest {
        val safeUrl = url.trim()
        require(safeUrl.isNotEmpty()) { "URL tidak boleh kosong." }

        val request = YoutubeDLRequest(safeUrl)
        val options = buildOptions(
            selectedOption = selectedOption,
            targetContainer = targetContainer,
            outputAudioFormat = outputAudioFormat,
            outputBitrate = outputBitrate,
            outputTemplate = outputTemplate,
            noPlaylist = noPlaylist
        )

        options.forEach { option ->
            if (option.value != null) {
                request.addOption(option.flag, option.value)
            } else {
                request.addOption(option.flag)
            }
        }
        return request
    }

    /**
     * Menghasilkan daftar [CommandOption] yang deterministik untuk inspeksi atau testing.
     */
    fun buildOptions(
        selectedOption: MediaFormatOption?,
        targetContainer: VideoContainer = VideoContainer.MP4,
        outputAudioFormat: String? = null,
        outputBitrate: String? = null,
        outputTemplate: String? = null,
        noPlaylist: Boolean = true
    ): List<CommandOption> {
        val options = mutableListOf<CommandOption>()

        if (noPlaylist) {
            options.add(CommandOption("--no-playlist"))
        }

        val normalizedAudioFormat = outputAudioFormat
            ?.trim()
            ?.lowercase()
            ?.removePrefix(".")
            ?.takeIf { it.isNotEmpty() }

        val isAudioMode = normalizedAudioFormat != null || (selectedOption?.isAudioOnly == true)

        if (isAudioMode) {
            appendAudioOptions(
                options = options,
                selectedOption = selectedOption,
                requestedFormat = normalizedAudioFormat,
                requestedBitrate = outputBitrate
            )
        } else {
            appendVideoOptions(
                options = options,
                selectedOption = selectedOption,
                targetContainer = targetContainer
            )
        }

        outputTemplate
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { template ->
                options.add(CommandOption("-o", template))
            }

        return options
    }

    private fun appendVideoOptions(
        options: MutableList<CommandOption>,
        selectedOption: MediaFormatOption?,
        targetContainer: VideoContainer
    ) {
        val formatSelector = buildVideoFormatSelector(selectedOption)
        options.add(CommandOption("-f", formatSelector))
        options.add(CommandOption("--merge-output-format", targetContainer.ext))
        options.add(CommandOption("--remux-video", targetContainer.ext))
    }

    private fun appendAudioOptions(
        options: MutableList<CommandOption>,
        selectedOption: MediaFormatOption?,
        requestedFormat: String?,
        requestedBitrate: String?
    ) {
        val formatSelector = buildAudioFormatSelector(selectedOption)
        options.add(CommandOption("-f", formatSelector))
        options.add(CommandOption("-x"))

        val optionExt = selectedOption?.ext
            ?.trim()
            ?.lowercase()
            ?.removePrefix(".")
            ?.takeIf { it.isNotEmpty() }

        val effectiveTarget = when {
            requestedFormat != null -> requestedFormat
            selectedOption?.isLosslessOriginal == true -> "best"
            selectedOption?.bitrateLabel in passthroughAudioFormats -> "best"
            optionExt in convertibleAudioFormats -> optionExt!!
            else -> "best"
        }

        if (effectiveTarget in passthroughAudioFormats || effectiveTarget !in convertibleAudioFormats) {
            // Mode 2: Audio best/max tanpa re-encode.
            options.add(CommandOption("--audio-format", "best"))
            return
        }

        // Mode 3: Audio konversi ke .mp3 / .aac / .flac dengan bitrate terpilih.
        options.add(CommandOption("--audio-format", effectiveTarget))

        val qualityArg = if (effectiveTarget == "flac") {
            "0"
        } else {
            resolveAudioQuality(
                requestedBitrate = requestedBitrate,
                selectedOption = selectedOption
            )
        }
        options.add(CommandOption("--audio-quality", qualityArg))
    }

    /**
     * Membentuk selector `-f` untuk video dengan rantai fallback aman jika stream spesifik tidak tersedia.
     */
    internal fun buildVideoFormatSelector(selectedOption: MediaFormatOption?): String {
        if (selectedOption == null) {
            return "bestvideo+bestaudio/best"
        }

        val selectors = linkedSetOf<String>()
        val cleanFormatId = selectedOption.formatId.trim().takeIf { it.isNotEmpty() }

        if (cleanFormatId != null) {
            if (selectedOption.hasAudio) {
                selectors.add(cleanFormatId)
            } else {
                selectors.add("$cleanFormatId+bestaudio")
            }
        }

        val exactFilters = buildString {
            selectedOption.height?.takeIf { it > 0 }?.let { append("[height=$it]") }
            selectedOption.fps?.takeIf { it > 0 }?.let { append("[fps<=$it]") }
            if (selectedOption.isHdr) {
                append("[dynamic_range!=SDR]")
            }
        }
        if (exactFilters.isNotEmpty()) {
            selectors.add("bestvideo$exactFilters+bestaudio")
        }

        val relaxedFilters = buildString {
            selectedOption.height?.takeIf { it > 0 }?.let { append("[height<=$it]") }
        }
        if (relaxedFilters.isNotEmpty()) {
            selectors.add("bestvideo$relaxedFilters+bestaudio")
            selectors.add("best$relaxedFilters")
        }

        selectors.add("bestvideo+bestaudio")
        selectors.add("best")

        return selectors.joinToString("/")
    }

    /**
     * Membentuk selector `-f` untuk audio dengan fallback aman ke `bestaudio/best`.
     */
    internal fun buildAudioFormatSelector(selectedOption: MediaFormatOption?): String {
        val selectors = linkedSetOf<String>()
        val cleanFormatId = selectedOption?.formatId
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("best", ignoreCase = true) && !it.equals("max", ignoreCase = true) }

        if (cleanFormatId != null) {
            selectors.add(cleanFormatId)
        }
        selectors.add("bestaudio")
        selectors.add("best")
        return selectors.joinToString("/")
    }

    private fun resolveAudioQuality(
        requestedBitrate: String?,
        selectedOption: MediaFormatOption?
    ): String {
        val raw = requestedBitrate
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }
            ?: selectedOption?.bitrateLabel?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            ?: selectedOption?.bitrateKbps?.toString()

        return when (raw) {
            "128k", "128" -> "128K"
            "320k", "320" -> "320K"
            "max", "best", null -> "0"
            else -> {
                val numeric = raw.removeSuffix("k").toIntOrNull()
                when {
                    numeric == null || numeric <= 0 -> "0"
                    numeric >= 320 -> "320K"
                    else -> "${numeric.coerceAtLeast(64)}K"
                }
            }
        }
    }
}
