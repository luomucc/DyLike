package me.lingci.lib.archive

enum class ArchivePasswordVerifyResult {
    VALID,
    WRONG_PASSWORD,
    UNSUPPORTED_METHOD,
    ENTRY_NOT_FOUND,
    ERROR
}
