package com.benzenelabs.hydra.host.data.vector

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VectorSearchResultTest {

    @Test
    fun `constructs correctly`() {
        val record = VectorRecord("id", floatArrayOf(1f), null)
        val score = 0.95f

        val result = VectorSearchResult(record, score)

        assertEquals(record, result.record)
        assertEquals(score, result.score)
    }
}
