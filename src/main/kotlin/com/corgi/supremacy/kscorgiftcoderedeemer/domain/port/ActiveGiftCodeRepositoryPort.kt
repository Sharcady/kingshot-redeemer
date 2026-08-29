package com.corgi.supremacy.kscorgiftcoderedeemer.domain.port

interface ActiveGiftCodeRepositoryPort {
    /** Returns null when no active-code snapshot has been saved yet. */
    fun findActiveGiftCodes(): List<String>?

    fun replaceActiveGiftCodes(giftCodes: List<String>)
}
