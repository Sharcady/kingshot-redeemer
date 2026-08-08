package com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.client

import com.corgi.supremacy.kscorgiftcoderedeemer.infrastructure.client.dto.GiftCodeRedeemRequest
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "kingshot", url = "\${application.kingshot.base-url}")
interface KingshotFeignClient {

    @GetMapping
    fun findPlayerById()

    @PostMapping(path = ["/gift-codes/redeem"])
    fun redeem(@RequestBody dto: GiftCodeRedeemRequest)

//    @GetMapping(path = ["/gift-codes"])
//    fun findGiftCodes() : List<GiftCodeResponse>
}
