package com.corgi.supremacy.kscorgiftcoderedeemer.domain.port

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.Image

interface ImageRepositoryPort {
    fun findRandom(): Image?
}
