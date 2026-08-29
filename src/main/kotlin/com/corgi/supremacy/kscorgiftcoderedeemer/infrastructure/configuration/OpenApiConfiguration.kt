package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.configuration

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
    info = Info(
        title = "Kingshot Gift Code Redeemer API",
        version = "v1",
        description = "Registers Kingshot players, retrieves active gift codes, and redeems them through the official redemption site.",
    ),
)
class OpenApiConfiguration
