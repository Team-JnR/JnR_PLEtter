package com.jnr.pletter.data

/** 편지 상태. 예약(SCHEDULED) → 조건 충족 시 전달(DELIVERED). 전달은 편지당 1회만 일어납니다. */
enum class LetterStatus { SCHEDULED, DELIVERED }

/** 같은 차를 함께 쓰는 사람의 앱 내 프로필. PIN은 해시로만 저장합니다. */
data class Profile(
    val id: String,
    val name: String,
    val pinHash: String,
)

data class Letter(
    val id: String,
    val senderId: String,
    val recipientId: String,
    /** 원본 녹음 파일 경로 (앱 내부 저장소) */
    val audioPath: String,
    val transcript: String,
    val createdAt: Long,
    /** 예약한 시동 세션. 이보다 뒤의 세션에서만 전달됩니다. */
    val createdSession: Int,
    val status: LetterStatus,
    val deliveredAt: Long? = null,
)
