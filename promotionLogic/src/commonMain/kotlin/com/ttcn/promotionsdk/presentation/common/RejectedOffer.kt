package com.ttcn.promotionsdk.presentation.common

/**
 * Một ưu đãi bị `validateStackableDiscounts` từ chối, kèm **câu giải thích của server**.
 *
 * Ở `presentation/common` chứ không nằm hẳn trong `presentation/endow`: nó do
 * [EndowStore][com.ttcn.promotionsdk.presentation.endow.EndowStore] sinh ra nhưng
 * [ChoosePromotionStore][com.ttcn.promotionsdk.presentation.choosepromotion.ChoosePromotionStore]
 * mới là nơi tiêu thụ. Để ở đây thì hai package cùng nhìn xuống một chỗ, không package nào phải
 * biết package kia.
 *
 * [message] có thể **rỗng**: server trả `valid = false` mà không kèm `validationMessages` lẫn
 * `businessRuleViolations`. Native lùi về câu lỗi chung của mình khi rỗng — lõi không dựng sẵn chuỗi
 * tiếng Việt (xem `docs/common/ErrorHandling.md`).
 */
data class RejectedOffer(
    val objectId: String,
    val message: String,
)
