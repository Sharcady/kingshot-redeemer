package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest

import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodeRedeemingException
import com.corgi.supremacy.kscorgiftcoderedeemer.domain.GiftCodesRetrievalException
import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.rest.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class KingshotExceptionHandler {

    @ExceptionHandler(GiftCodeRedeemingException::class, GiftCodesRetrievalException::class)
    fun handleKingshotAutomationException(exception: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(
                ErrorResponse(
                    message = exception.message ?: "Kingshot automation failed",
                    cause = exception.cause?.message,
                )
            )
}
