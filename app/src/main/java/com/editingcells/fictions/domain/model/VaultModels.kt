package com.editingcells.fictions.domain.model

import android.net.Uri
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

enum class VaultFileType(
    val extension: String,
    val label: String,
    val mimeTypes: Array<String>
) {
    PDF("pdf", "PDF", arrayOf("application/pdf")),
    CSV("csv", "CSV", arrayOf("text/csv", "text/comma-separated-values", "application/csv")),
    TXT("txt", "TXT", arrayOf("text/plain", "*/*"));

    companion object {
        fun fromFileNameOrMime(fileName: String, mimeType: String?): VaultFileType {
            val lower = fileName.lowercase(Locale.ROOT)
            return when {
                lower.endsWith(".pdf") || mimeType == "application/pdf" -> PDF
                lower.endsWith(".csv") ||
                    lower.endsWith(".tsv") ||
                    mimeType?.contains("csv", ignoreCase = true) == true ||
                    mimeType?.contains("comma-separated", ignoreCase = true) == true -> CSV
                else -> TXT
            }
        }
    }
}

data class PendingImportItem(
    val uri: Uri,
    val originalName: String,
    val customName: String,
    val detectedType: VaultFileType,
    val sizeBytes: Long
)

enum class PastelTheme(
    val displayName: String,
    val subtitle: String,
    val previewPrimaryHex: Long,
    val previewSecondaryHex: Long
) {
    LAVENDER("Lavender", "Soft iris & violet mist", 0xFF7C3AED, 0xFFDDD6FE),
    PEACH("Peach", "Warm coral & apricot glow", 0xFFEA580C, 0xFFFED7AA),
    MINT("Mint", "Fresh sage & emerald breeze", 0xFF059669, 0xFFA7F3D0),
    SKY("Sky", "Cerulean air & arctic frost", 0xFF0284C7, 0xFFBAE6FD),
    ROSE("Rose", "Blush petal & velvet berry", 0xFFE11D48, 0xFFFECDD3),
    SAND("Sand", "Dune gold & warm parchment", 0xFFD97706, 0xFFFDE68A)
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class SortOption(val label: String) {
    DATE("Date Added"),
    NAME("Name"),
    SIZE("Size"),
    TYPE("File Type")
}

enum class ViewMode(val label: String) {
    GRID("Grid"),
    LIST("List")
}

enum class FolderColor(val label: String, val hex: Long) {
    VIOLET("Violet", 0xFF8B5CF6),
    CORAL("Coral", 0xFFF43F5E),
    EMERALD("Emerald", 0xFF10B981),
    OCEAN("Ocean", 0xFF0EA5E9),
    AMBER("Amber", 0xFFF59E0B),
    INDIGO("Indigo", 0xFF6366F1),
    TEAL("Teal", 0xFF14B8A6),
    MAGENTA("Magenta", 0xFFEC4899);

    companion object {
        fun fromHex(hex: Long): FolderColor =
            entries.firstOrNull { it.hex == hex } ?: VIOLET
    }
}

data class VaultFolder(
    val id: Long = 0L,
    val name: String,
    val colorHex: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val fileCount: Int = 0,
    val totalSizeBytes: Long = 0L
)

data class VaultFile(
    val id: Long = 0L,
    val name: String,
    val type: VaultFileType,
    val sizeBytes: Long,
    val dateAdded: Long,
    val lastOpened: Long?,
    val folderId: Long?,
    val folderName: String? = null,
    val folderColorHex: Long? = null,
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false,
    val filePath: String
) {
    val formattedSize: String
        get() = formatBytes(sizeBytes)

    val formattedDate: String
        get() = formatTimestamp(dateAdded)

    val formattedLastOpened: String
        get() = lastOpened?.let { formatTimestamp(it) } ?: "Never opened"
}

data class StorageSummary(
    val totalBytes: Long = 0L,
    val quotaBytes: Long = 100L * 1024L * 1024L,
    val pdfCount: Int = 0,
    val csvCount: Int = 0,
    val txtCount: Int = 0,
    val pdfBytes: Long = 0L,
    val csvBytes: Long = 0L,
    val txtBytes: Long = 0L
) {
    val totalCount: Int
        get() = pdfCount + csvCount + txtCount

    val usedFraction: Float
        get() = if (totalCount == 0) 0f else (totalBytes.toFloat() / quotaBytes.toFloat()).coerceIn(0.06f, 1f)
}

data class UserPreferences(
    val theme: PastelTheme = PastelTheme.LAVENDER,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultViewMode: ViewMode = ViewMode.GRID,
    val defaultSortOption: SortOption = SortOption.DATE,
    val sortAscending: Boolean = false,
    val readerFontSizeSp: Float = 16f
)

data class CsvTableData(
    val headers: List<String>,
    val rows: List<List<String>>
)

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return DecimalFormat("#,##0.#").format(value) + " " + units[digitGroups]
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDetailedTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
