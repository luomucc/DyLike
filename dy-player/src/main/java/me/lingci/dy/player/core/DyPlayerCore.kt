package me.lingci.dy.player.core

/**
 * Persisted dy-player core choice.
 *
 * Numeric values are storage ABI: do not reorder or reuse them. AUTO is reserved for a future
 * policy-based selector and currently resolves to Exo instead of being enabled as a separate UI path.
 */
enum class DyPlayerCore(val value: Int, val displayName: String) {
    EXO(0, "ExoPlayer"),
    MPV(1, "MPV"),
    AUTO(2, "Auto"),
    ;

    companion object {
        /** Unknown restored/future values fall back to Exo, the current safest default backend. */
        fun fromValue(value: Int): DyPlayerCore {
            return values().firstOrNull { it.value == value } ?: EXO
        }

        /** Legacy videoPlayerExo=false meant "not Exo"; after IJK removal that maps to MPV. */
        fun fromLegacyExo(enabled: Boolean): DyPlayerCore {
            return if (enabled) EXO else MPV
        }
    }
}
