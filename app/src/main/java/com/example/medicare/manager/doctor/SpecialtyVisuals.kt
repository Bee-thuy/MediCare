package com.example.medicare.manager.doctor

import com.example.medicare.R
import com.example.medicare.model.Specialty

/** Built-in images are stable across devices and need no public image hosting. */
object SpecialtyVisuals {
    data class Option(val key: String, val label: String, val drawable: Int, val symbol: String)

    val options = listOf(
        Option("general", "Tổng quát", R.drawable.ic_manager_specialty, "🩺"),
        Option("heart", "Tim mạch", R.drawable.ic_specialty_heart, "❤️"),
        Option("child", "Nhi khoa", R.drawable.ic_specialty_child, "🧸"),
        Option("bone", "Cơ xương", R.drawable.ic_specialty_bone, "🦴"),
        Option("ear", "Tai mũi họng", R.drawable.ic_specialty_ear, "👂"),
    )

    fun forKey(key: String): Option = options.firstOrNull { it.key == key } ?: options.first()

    fun forSpecialty(specialty: Specialty): Option {
        if (specialty.iconKey.isNotBlank()) return forKey(specialty.iconKey)
        val name = specialty.name.lowercase()
        return when {
            "tim" in name -> forKey("heart")
            "nhi" in name -> forKey("child")
            "xương" in name || "khớp" in name -> forKey("bone")
            "tai" in name || "mũi" in name || "họng" in name -> forKey("ear")
            else -> forKey("general")
        }
    }
}
