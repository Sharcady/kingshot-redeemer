package com.corgi.supremacy.kscorgiftcoderedeemer.domain

class ImageRetrievalException(
    cause: Throwable? = null,
) : RuntimeException("Image retrieval failed", cause)

class GiftCodesRetrievalException(
    cause: Throwable? = null,
) : RuntimeException("GiftCode retrieval failed", cause)

class GiftCodeRedeemingException(
    message: String = "GiftCode redeeming failed",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
