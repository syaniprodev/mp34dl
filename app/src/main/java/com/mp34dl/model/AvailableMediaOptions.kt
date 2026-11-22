package com.mp34dl.model

/**
 * Kontainer opsi media yang valid dan tersedia untuk diunduh,
 * diisi secara ketat hanya dari entri JSON `yt-dlp` yang ada dan tidak kosong.
 */
data class AvailableMediaOptions(
    // Daftar opsi stream video yang tersedia (resolusi, FPS, dan flag HDR sesuai sumber).
    val videoOptions: List<MediaFormatOption> = emptyList(),
    // Daftar opsi format/bitrate audio yang tersedia (mis. .mp3, .aac, .flac, best).
    val audioOptions: List<MediaFormatOption> = emptyList(),
    // Judul media hasil ekstraksi metadata yt-dlp; null jika tidak tersedia.
    val title: String? = null,
    // Durasi total media dalam detik; null untuk live stream atau jika tidak diketahui.
    val durationSeconds: Long? = null,
    // URL sumber yang diparsing untuk menghasilkan daftar opsi ini.
    val sourceUrl: String? = null
) {
    // Menandakan apakah tersedia minimal satu opsi video.
    val hasVideoOptions: Boolean
        get() = videoOptions.isNotEmpty()

    // Menandakan apakah tersedia minimal satu opsi audio.
    val hasAudioOptions: Boolean
        get() = audioOptions.isNotEmpty()

    // Menandakan apakah seluruh daftar opsi video dan audio kosong.
    val isEmpty: Boolean
        get() = videoOptions.isEmpty() && audioOptions.isEmpty()
}
