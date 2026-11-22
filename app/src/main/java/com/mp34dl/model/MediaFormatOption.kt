package com.mp34dl.model

/**
 * Merepresentasikan satu opsi stream video atau audio yang benar-benar tersedia
 * dari hasil parsing JSON `yt-dlp` untuk URL saat ini.
 */
data class MediaFormatOption(
    // ID unik format dari yt-dlp (mis. "137", "251", "bestaudio").
    val formatId: String,
    // Resolusi tinggi video dalam piksel (mis. 720, 1080, 2160); null untuk opsi audio.
    val height: Int? = null,
    // Frame rate video dalam FPS (mis. 30, 60); null jika tidak tersedia atau opsi audio.
    val fps: Int? = null,
    // Rentang dinamis video (mis. "SDR", "HDR10", "HLG", "DV"); null jika tidak diketahui.
    val dynamicRange: String? = null,
    // Penanda eksplisit apakah stream video mendukung HDR.
    val isHdr: Boolean = false,
    // Codec video (mis. "avc1", "vp9", "av01"); null untuk opsi khusus audio.
    val vcodec: String? = null,
    // Codec audio (mis. "mp4a", "opus", "flac"); null jika stream tidak memiliki audio.
    val acodec: String? = null,
    // Perkiraan ukuran file dalam byte; null jika yt-dlp tidak menyertakan estimasi ukuran.
    val filesizeApprox: Long? = null,
    // Ekstensi kontainer atau format output (mis. "mp4", "webm", "mp3", "aac", "flac", "best").
    val ext: String? = null,
    // Bitrate audio dalam kbps (mis. 128, 320, atau bitrate maksimal sumber); null jika tidak berlaku.
    val bitrateKbps: Int? = null,
    // Label ringkas bitrate audio yang tersedia (mis. "128k", "320k", "max"); null untuk video.
    val bitrateLabel: String? = null,
    // Bernilai true jika opsi merupakan audio original/lossless terbaik tanpa transcode lossy.
    val isLosslessOriginal: Boolean = false,
    // Bernilai true jika opsi ini adalah stream khusus audio (audio-only).
    val isAudioOnly: Boolean = false,
    // Bernilai true jika stream video sudah membawa track audio bawaan (muxed).
    val hasAudio: Boolean = false
)
