package io.asqio.sdk.network

import io.asqio.sdk.AsqioConfiguration
import io.asqio.sdk.error.ApiErrorCode
import io.asqio.sdk.error.AsqioError
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = ApiClient(
            configuration = AsqioConfiguration(
                tenantKey = "test-tenant",
                jwtProvider = { "test-jwt" },
                baseUrl = server.url("/").toString().trimEnd('/'),
            )
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `listTickets returns parsed tickets`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "tickets": [
                        {
                          "id": "ticket-1",
                          "title": "Hello",
                          "unread": true,
                          "created_at": "2026-01-01T00:00:00Z",
                          "updated_at": "2026-01-02T00:00:00Z"
                        }
                      ],
                      "meta": {
                        "current_page": 1,
                        "total_pages": 1,
                        "total_count": 1,
                        "per_page": 20
                      }
                    }
                    """.trimIndent()
                )
        )

        val response = client.request(
            ApiEndpoint.ListTickets(page = 1, perPage = 20),
            TicketListResponse.serializer(),
        )

        assertEquals(1, response.tickets.size)
        assertEquals("ticket-1", response.tickets.first().id)
        assertEquals("Hello", response.tickets.first().title)
        assertEquals(true, response.tickets.first().unread)

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/tickets?page=1&per_page=20", request.path)
        assertEquals("Bearer test-jwt", request.getHeader("Authorization"))
        assertEquals("test-tenant", request.getHeader("X-Tenant-Key"))
    }

    @Test
    fun `error response is mapped to AsqioError ApiError`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"Ticket not found","code":"NOT_FOUND"}""")
        )

        val error = try {
            client.request(
                ApiEndpoint.GetTicket("missing"),
                io.asqio.sdk.model.Ticket.serializer(),
            )
            null
        } catch (e: AsqioError.ApiError) {
            e
        }

        assertTrue("ApiError should be thrown", error != null)
        assertEquals(404, error!!.statusCode)
        assertEquals("Ticket not found", error.errorMessage)
        assertEquals("NOT_FOUND", error.code.raw)
        assertTrue(error.code is ApiErrorCode.KnownCode)
    }

    @Test
    fun `unread count endpoint decodes plain integer payload`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"unread_count": 7}""")
        )

        val response = client.request(
            ApiEndpoint.UnreadCount,
            UnreadCountResponse.serializer(),
        )

        assertEquals(7, response.unreadCount)
    }

    @Test
    fun `JWT provider failure is mapped to JwtProviderFailed`() = runTest {
        val failingClient = ApiClient(
            configuration = AsqioConfiguration(
                tenantKey = "test-tenant",
                jwtProvider = { throw IllegalStateException("no token") },
                baseUrl = server.url("/").toString().trimEnd('/'),
            )
        )

        val error = try {
            failingClient.request(ApiEndpoint.UnreadCount, UnreadCountResponse.serializer())
            null
        } catch (e: AsqioError.JwtProviderFailed) {
            e
        }

        assertTrue("JwtProviderFailed should be thrown", error != null)
    }
}
