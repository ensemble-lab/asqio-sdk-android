package io.asqio.sdk.model

import io.asqio.sdk.network.asqioJson
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelSerializationTest {

    @Test
    fun `Ticket decodes ISO8601 dates as Instant`() {
        val json = """
            {
              "id": "t-1",
              "title": "Hello",
              "topic": {"id":"top-1","name":"Billing"},
              "context": {"screen":"payment"},
              "unread": false,
              "created_at": "2026-01-01T12:00:00Z",
              "updated_at": "2026-01-02T12:00:00Z"
            }
        """.trimIndent()

        val ticket = asqioJson.decodeFromString(Ticket.serializer(), json)

        assertEquals("t-1", ticket.id)
        assertEquals("Hello", ticket.title)
        assertEquals("Billing", ticket.topic?.name)
        assertEquals("payment", ticket.context?.get("screen"))
        assertEquals(false, ticket.unread)
        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), ticket.createdAt)
        assertEquals(Instant.parse("2026-01-02T12:00:00Z"), ticket.updatedAt)
    }

    @Test
    fun `Message decodes sender_type enum`() {
        val json = """
            {
              "id": "m-1",
              "sender_type": "operator",
              "sender_id": "op-1",
              "body": "返信です",
              "created_at": "2026-01-01T12:00:00Z"
            }
        """.trimIndent()

        val message = asqioJson.decodeFromString(Message.serializer(), json)

        assertEquals(SenderType.OPERATOR, message.senderType)
        assertEquals("op-1", message.senderId)
        assertEquals("返信です", message.body)
    }

    @Test
    fun `optional fields default to null when absent`() {
        val json = """
            {
              "id": "t-2",
              "unread": true,
              "created_at": "2026-01-01T00:00:00Z",
              "updated_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        val ticket = asqioJson.decodeFromString(Ticket.serializer(), json)

        assertNull(ticket.title)
        assertNull(ticket.topic)
        assertNull(ticket.context)
        assertNull(ticket.deviceInfo)
        assertNull(ticket.messages)
    }
}
