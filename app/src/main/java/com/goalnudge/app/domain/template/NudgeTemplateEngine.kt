package com.goalnudge.app.domain.template

import com.goalnudge.app.domain.model.Goal
import com.goalnudge.app.domain.model.NudgeContent
import com.goalnudge.app.domain.model.Tone
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Isi nudge dari kata-kata user sendiri (judul + why), bukan quote generik — lihat PLAN.md §1.
 * Variabel: {sisa_hari}, {streak}, {hari_sejak_checkin}, {why}, {judul}.
 */
@Singleton
class NudgeTemplateEngine @Inject constructor() {

    private val bodyTemplates: Map<Tone, List<String>> = mapOf(
        Tone.LEMBUT to listOf(
            "Kamu menulis ini {hari_sejak_dibuat} hari lalu. Pelan-pelan juga nggak apa, yang penting jalan.",
            "Sisa {sisa_hari} hari lagi. Nggak perlu buru-buru, satu langkah kecil hari ini cukup.",
            "Udah {hari_sejak_checkin} hari sejak check-in terakhir. Kalau ada waktu sebentar, boleh lho.",
            "Streak kamu {streak} hari. Nggak masalah kalau mau istirahat, tapi kalau sempat, lanjutkan ya.",
            "Ingat kenapa kamu mulai: \"{why}\". Nggak perlu sempurna hari ini."
        ),
        Tone.NETRAL to listOf(
            "Sisa {sisa_hari} hari menuju target. Check-in terakhir {hari_sejak_checkin} hari lalu.",
            "Streak saat ini: {streak} hari. Kamu menulis goal ini {hari_sejak_dibuat} hari lalu.",
            "Alasanmu: \"{why}\". Tinggal {sisa_hari} hari lagi.",
            "Progress check: {hari_sejak_checkin} hari sejak terakhir check-in.",
            "Target: {sisa_hari} hari lagi. Jangan biarkan momentum turun."
        ),
        Tone.KERAS to listOf(
            "{sisa_hari} hari lagi dan kamu belum check-in {hari_sejak_checkin} hari. Serius mau ini kejadian?",
            "Kamu bilang alasannya: \"{why}\". Terus kenapa {hari_sejak_checkin} hari nggak ada progres?",
            "Streak {streak} hari itu percuma kalau berhenti sekarang. Sisa {sisa_hari} hari, jangan buang.",
            "{sisa_hari} hari tersisa. Kamu tulis ini {hari_sejak_dibuat} hari lalu — masih niat atau cuma wacana?",
            "Berhenti scroll. \"{why}\" itu kata-katamu sendiri. {sisa_hari} hari lagi, gerak."
        )
    )

    fun render(goal: Goal, tone: Tone): NudgeContent {
        val vars = buildVariables(goal)
        val templates = bodyTemplates.getValue(tone)
        val seed = goal.id * 31 + LocalDate.now().toEpochDay()
        val chosen = templates[Random(seed).nextInt(templates.size)]
        return NudgeContent(
            goalId = goal.id,
            title = substitute("\"{judul}\"", vars),
            body = substitute(chosen, vars),
            why = goal.why,
            checkInLine = substitute("Check-in terakhir: {hari_sejak_checkin} hari lalu.", vars)
        )
    }

    private fun buildVariables(goal: Goal): Map<String, String> = mapOf(
        "sisa_hari" to goal.daysUntilTarget.coerceAtLeast(0).toString(),
        "streak" to goal.currentStreak.toString(),
        "hari_sejak_checkin" to (goal.daysSinceCheckIn?.toString() ?: "belum pernah"),
        "hari_sejak_dibuat" to goal.daysSinceCreated.toString(),
        "why" to goal.why,
        "judul" to goal.title
    )

    private fun substitute(template: String, vars: Map<String, String>): String {
        var result = template
        for ((key, value) in vars) {
            result = result.replace("{$key}", value)
        }
        return result
    }
}
