package com.jnr.pletter.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * MVP용 온디바이스 저장소. 프로필·편지 메타데이터를 앱 내부 저장소의 JSON 파일 하나에 둡니다.
 * (암호화·계정별 분리 저장은 MVP 범위 밖 — 이후 Room + 암호화로 교체할 자리)
 */
class LetterStore(context: Context) {

    private val file = File(context.filesDir, "pletter.json")

    val profiles = mutableStateListOf<Profile>()
    val letters = mutableStateListOf<Letter>()

    /** 현재 시동 세션 번호 */
    var sessionId by mutableIntStateOf(0)
        private set

    init {
        load()
    }

    // ---------- 시동 세션 ----------

    fun startNewSession() {
        sessionId += 1
        save()
    }

    // ---------- 프로필 ----------

    fun addProfile(name: String, pin: String): Profile {
        val id = UUID.randomUUID().toString()
        val profile = Profile(id = id, name = name.trim(), pinHash = hashPin(id, pin))
        profiles.add(profile)
        save()
        return profile
    }

    fun verifyPin(profile: Profile, pin: String): Boolean =
        profile.pinHash == hashPin(profile.id, pin)

    fun profileName(id: String): String =
        profiles.firstOrNull { it.id == id }?.name ?: "알 수 없음"

    // ---------- 편지 ----------

    fun schedule(senderId: String, recipientId: String, audioPath: String, transcript: String): Letter {
        val letter = Letter(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            recipientId = recipientId,
            audioPath = audioPath,
            transcript = transcript.trim(),
            createdAt = System.currentTimeMillis(),
            createdSession = sessionId,
            status = LetterStatus.SCHEDULED,
        )
        letters.add(letter)
        save()
        return letter
    }

    /**
     * 전달 조건 판정(유형 ① 같은 차).
     * 본인 확인을 마친 운전자에게, 이전 시동 세션에서 예약된 편지만 전달합니다.
     * 이미 전달된 편지는 다시 처리하지 않습니다(편지당 1회).
     * @return 이번에 새로 전달된 편지 수
     */
    fun deliverPendingFor(profileId: String): Int {
        var delivered = 0
        val now = System.currentTimeMillis()
        for (i in letters.indices) {
            val letter = letters[i]
            if (letter.status == LetterStatus.SCHEDULED &&
                letter.recipientId == profileId &&
                letter.createdSession < sessionId
            ) {
                letters[i] = letter.copy(status = LetterStatus.DELIVERED, deliveredAt = now)
                delivered += 1
            }
        }
        if (delivered > 0) save()
        return delivered
    }

    /** 발송 전 취소 또는 수신자의 삭제. 녹음 파일도 함께 지웁니다. */
    fun deleteLetter(letterId: String) {
        val letter = letters.firstOrNull { it.id == letterId } ?: return
        letters.remove(letter)
        discardAudio(letter.audioPath)
        save()
    }

    /** 예약하지 않고 버린 임시 녹음을 지웁니다. */
    fun discardAudio(path: String) {
        runCatching { File(path).delete() }
    }

    fun inboxOf(profileId: String): List<Letter> =
        letters.filter { it.recipientId == profileId && it.status == LetterStatus.DELIVERED }
            .sortedByDescending { it.deliveredAt ?: it.createdAt }

    fun outboxOf(profileId: String): List<Letter> =
        letters.filter { it.senderId == profileId }.sortedByDescending { it.createdAt }

    // ---------- 저장 / 불러오기 ----------

    private fun hashPin(salt: String, pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$salt:$pin".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun load() {
        if (!file.exists()) return
        runCatching {
            val root = JSONObject(file.readText())
            sessionId = root.optInt("sessionId", 0)

            val profileArray = root.optJSONArray("profiles") ?: JSONArray()
            for (i in 0 until profileArray.length()) {
                val o = profileArray.getJSONObject(i)
                profiles.add(
                    Profile(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        pinHash = o.getString("pinHash"),
                    )
                )
            }

            val letterArray = root.optJSONArray("letters") ?: JSONArray()
            for (i in 0 until letterArray.length()) {
                val o = letterArray.getJSONObject(i)
                letters.add(
                    Letter(
                        id = o.getString("id"),
                        senderId = o.getString("senderId"),
                        recipientId = o.getString("recipientId"),
                        audioPath = o.getString("audioPath"),
                        transcript = o.optString("transcript", ""),
                        createdAt = o.getLong("createdAt"),
                        createdSession = o.getInt("createdSession"),
                        status = LetterStatus.valueOf(o.getString("status")),
                        deliveredAt = if (o.has("deliveredAt")) o.getLong("deliveredAt") else null,
                    )
                )
            }
        }
    }

    private fun save() {
        val root = JSONObject()
        root.put("sessionId", sessionId)

        val profileArray = JSONArray()
        profiles.forEach { p ->
            profileArray.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("pinHash", p.pinHash)
            )
        }
        root.put("profiles", profileArray)

        val letterArray = JSONArray()
        letters.forEach { l ->
            val o = JSONObject()
                .put("id", l.id)
                .put("senderId", l.senderId)
                .put("recipientId", l.recipientId)
                .put("audioPath", l.audioPath)
                .put("transcript", l.transcript)
                .put("createdAt", l.createdAt)
                .put("createdSession", l.createdSession)
                .put("status", l.status.name)
            l.deliveredAt?.let { o.put("deliveredAt", it) }
            letterArray.put(o)
        }
        root.put("letters", letterArray)

        runCatching { file.writeText(root.toString()) }
    }
}
