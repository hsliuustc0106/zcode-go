package com.zcodego.app.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpInsertTest {
    @Test
    fun quotesTextWithoutLettingItBreakTheScript() {
        val hostile = "\"; alert(1); //\n</script><img>"
        val literal = FollowUpInsert.jsStringLiteral(hostile)
        assertTrue(literal.startsWith("\""))
        assertTrue(literal.endsWith("\""))
        assertFalse(literal.contains("</script>"))
        assertFalse(literal.contains("\n"))
        val script = FollowUpInsert.script(hostile)
        assertTrue(script.contains("var text = $literal;"))
        assertFalse(script.contains("innerHTML"))
        assertFalse(script.contains("document.write"))
        assertTrue(script.contains("activeElement"))
        assertFalse(script.contains("click("))
    }

    @Test
    fun readsTheValueWebViewReturns() {
        assertEquals(InsertOutcome.INSERTED, FollowUpInsert.interpret("\"inserted\""))
        assertEquals(InsertOutcome.INSERTED, FollowUpInsert.interpret("inserted"))
        assertEquals(InsertOutcome.NO_TARGET, FollowUpInsert.interpret("\"none\""))
        assertEquals(InsertOutcome.NO_TARGET, FollowUpInsert.interpret(null))
        assertEquals(InsertOutcome.NO_TARGET, FollowUpInsert.interpret("null"))
        assertEquals(InsertOutcome.NO_TARGET, FollowUpInsert.interpret("\"INSERTED\""))
    }
}
