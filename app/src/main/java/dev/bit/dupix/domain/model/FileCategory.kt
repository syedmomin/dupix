package dev.bit.dupix.domain.model

/** The kinds of files Dupix scans for duplicates / large files. */
enum class FileCategory(val label: String) {
    PHOTO("Photos"),
    VIDEO("Videos"),
    AUDIO("Audio"),
    DOCUMENT("Documents"),
    APK("APKs"),
    LARGE_FILE("Large Files");
}
