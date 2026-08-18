package com.goalnudge.app.domain.model

/** Slot nudge terjadwal dalam sehari. Default 3 slot = 2-4 nudge/hari sesuai PLAN.md §4. */
enum class NudgeWindow(val startHour: Int, val endHour: Int) {
    PAGI(startHour = 5, endHour = 10),
    SIANG(startHour = 11, endHour = 15),
    MALAM(startHour = 19, endHour = 23);

    companion object {
        fun current(hourOfDay: Int): NudgeWindow? =
            entries.firstOrNull { hourOfDay in it.startHour until it.endHour }
    }
}
