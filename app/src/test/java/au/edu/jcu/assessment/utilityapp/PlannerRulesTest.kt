package au.edu.jcu.assessment.utilityapp

import au.edu.jcu.assessment.utilityapp.model.*
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class PlannerRulesTest {
    private val mondayClass = StudyCourse("math", "Maths", 1, 9 * 60, 10 * 60)
    private val monday = LocalDateTime.of(2026, 10, 5, 8, 30)

    @Test fun upcomingLessonUsesSameDay() {
        assertEquals(monday.withHour(9).withMinute(0), nextOccurrence(mondayClass, monday).start)
    }
    @Test fun ongoingLessonRemainsCurrent() {
        assertEquals(monday.withHour(9).withMinute(0),
            nextOccurrence(mondayClass, monday.withHour(9).withMinute(30)).start)
    }
    @Test fun lessonAtEndTimeMovesToNextWeek() {
        assertEquals(monday.plusWeeks(1).withHour(9).withMinute(0),
            nextOccurrence(mondayClass, monday.withHour(10).withMinute(0)).start)
    }
    @Test fun sundayToMondayRollsForwardOneDay() {
        val sunday = LocalDateTime.of(2026, 10, 11, 18, 0)
        assertEquals(sunday.plusDays(1).withHour(9), nextOccurrence(mondayClass, sunday).start)
    }
    @Test fun nextClassSortsByActualOccurrence() {
        val fridayClass = StudyCourse("fri", "Friday class", 5, 11 * 60, 12 * 60)
        val thursday = LocalDateTime.of(2026, 10, 8, 15, 0)
        assertEquals("fri", nextClass(listOf(mondayClass, fridayClass), thursday)?.course?.id)
        assertNull(nextClass(emptyList(), thursday))
    }
    @Test fun adjacentClassesDoNotOverlap() {
        val adjacent = StudyCourse("other", "Physics", 1, 10 * 60, 11 * 60)
        assertFalse(overlaps(mondayClass, adjacent))
        assertNull(validateCourse(adjacent, listOf(mondayClass)))
    }
    @Test fun overlappingClassesAreRejectedOnlyOnSameDay() {
        val overlap = StudyCourse("other", "Physics", 1, 9 * 60 + 30, 11 * 60)
        assertNotNull(validateCourse(overlap, listOf(mondayClass)))
        assertNull(validateCourse(overlap.copy(weekday = 2), listOf(mondayClass)))
    }
    @Test fun invalidTimesAndBlankTitlesAreRejected() {
        assertNotNull(validateCourse(mondayClass.copy(endMinute = 8 * 60), emptyList()))
        assertNotNull(validateCourse(mondayClass.copy(title = " "), emptyList()))
        assertNotNull(validateCourse(mondayClass.copy(weekday = 8), emptyList()))
        assertNotNull(validateCourse(mondayClass.copy(endMinute = 1440), emptyList()))
    }
    @Test fun reminderWindowExcludesSentAndStartedLessons() {
        val snapshot = PlannerSnapshot(courses = listOf(mondayClass),
            settings = PlannerSettings(remindersEnabled = true, reminderMinutes = 60))
        assertEquals(1, dueReminders(snapshot, monday, emptySet()).size)
        assertTrue(dueReminders(snapshot, monday, setOf("math|2026-10-05")).isEmpty())
        assertTrue(dueReminders(snapshot, monday.withHour(9).withMinute(0), emptySet()).isEmpty())
        assertTrue(dueReminders(snapshot, monday.withHour(7), emptySet()).isEmpty())
        assertTrue(dueReminders(snapshot.copy(settings = PlannerSettings()), monday, emptySet()).isEmpty())
    }
    @Test fun reminderWindowUsesSecondsWithoutEarlyRounding() {
        val snapshot = PlannerSnapshot(courses = listOf(mondayClass),
            settings = PlannerSettings(remindersEnabled = true, reminderMinutes = 30))
        assertTrue(dueReminders(snapshot, monday.minusSeconds(1), emptySet()).isEmpty())
        assertEquals(1, dueReminders(snapshot, monday, emptySet()).size)
    }
}
