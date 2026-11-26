package com.syntaxbhavesh.rudra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RudraCommandRulesTest {
    @Test fun recognisesWakePhraseVariants() {
        assertTrue(RudraCommandRules.isWakePhrase("Hey RUDRA"))
        assertTrue(RudraCommandRules.isWakePhrase("hey roodra, are you there?"))
        assertTrue(RudraCommandRules.isWakePhrase("HEY RUDRA!"))
    }

    @Test fun rejectsWakePhraseWithoutHeyPrefix() {
        assertFalse(RudraCommandRules.isWakePhrase("RUDRA"))
        assertFalse(RudraCommandRules.isWakePhrase("hello assistant"))
    }

    @Test fun convertsReminderDurationsSafely() {
        assertEquals(10L, RudraCommandRules.reminderDelaySeconds(10, "seconds"))
        assertEquals(600L, RudraCommandRules.reminderDelaySeconds(10, "minutes"))
        assertEquals(3600L, RudraCommandRules.reminderDelaySeconds(1, "hour"))
        assertEquals(604800L, RudraCommandRules.reminderDelaySeconds(168, "hours"))
    }

    @Test fun rejectsInvalidOrOutOfRangeReminderDurations() {
        assertEquals(null, RudraCommandRules.reminderDelaySeconds(9, "seconds"))
        assertEquals(null, RudraCommandRules.reminderDelaySeconds(169, "hours"))
        assertEquals(null, RudraCommandRules.reminderDelaySeconds(Long.MAX_VALUE, "hours"))
        assertEquals(null, RudraCommandRules.reminderDelaySeconds(10, "days"))
        assertEquals(null, RudraCommandRules.reminderDelaySeconds(0, "minutes"))
    }
}
