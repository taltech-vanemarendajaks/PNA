package crawler

import com.pna.backend.crawler.Crawler
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CrawlerTest {
    private lateinit var server: MockWebServer

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
    }

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `search returns result when page contains normalized number only`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.startsWith("/html/") == true -> {
                        MockResponse().setBody(
                            """
                            <html><body>
                              <div class="result">
                                <a class="result__a" href="${server.url("/result")}">result</a>
                              </div>
                            </body></html>
                            """.trimIndent()
                        )
                    }

                    request.path == "/result" -> {
                        MockResponse().setBody(
                            """
                            <html><head><title>Directory Entry</title></head><body>
                              Contact us at <strong>3726160245</strong>
                            </body></html>
                            """.trimIndent()
                        )
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()

        val crawler = Crawler(searchBaseUrl = server.url("/html/").toString())

        val result = crawler.search("+372 616 0245")

        assertEquals("Directory Entry — ${server.url("/result")}", result)
    }

    @Test
    fun `search tries multiple query variants`() {
        val requestedQueries = mutableListOf<String>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath
                return when {
                    path == "/html/" -> {
                        val query = request.requestUrl?.queryParameter("q").orEmpty()
                        requestedQueries += query
                        if (query == "\"6160245\"") {
                            MockResponse().setBody(
                                """
                                <html><body>
                                  <div class="result">
                                    <a class="result__a" href="${server.url("/result")}">result</a>
                                  </div>
                                </body></html>
                                """.trimIndent()
                            )
                        } else {
                            MockResponse().setBody("<html><body>No results</body></html>")
                        }
                    }

                    path == "/result" -> {
                        MockResponse().setBody(
                            """
                            <html><head><title>Local Listing</title></head><body>
                              Phone: 6160245
                            </body></html>
                            """.trimIndent()
                        )
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()

        val crawler = Crawler(searchBaseUrl = server.url("/html/").toString())

        val result = crawler.search("6160245")

        assertEquals("Local Listing — ${server.url("/result")}", result)
        assertTrue(requestedQueries.size >= 2)
        assertTrue("\"6160245\"" in requestedQueries)
    }

    @Test
    fun `search uses configured region for local numbers`() {
        val requestedQueries = mutableListOf<String>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath
                return when {
                    path == "/html/" -> {
                        val query = request.requestUrl?.queryParameter("q").orEmpty()
                        requestedQueries += query
                        if (query.contains("+49")) {
                            MockResponse().setBody(
                                """
                                <html><body>
                                  <div class="result">
                                    <a class="result__a" href="${server.url("/result")}">result</a>
                                  </div>
                                </body></html>
                                """.trimIndent()
                            )
                        } else {
                            MockResponse().setBody("<html><body>No results</body></html>")
                        }
                    }

                    path == "/result" -> {
                        MockResponse().setBody(
                            """
                            <html><head><title>Berlin Listing</title></head><body>
                              Contact: +49 30 123 456
                            </body></html>
                            """.trimIndent()
                        )
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()

        val crawler = Crawler(searchBaseUrl = server.url("/html/").toString(), defaultRegion = "DE")

        val result = crawler.search("030123456")

        assertEquals("Berlin Listing — ${server.url("/result")}", result)
        assertTrue(requestedQueries.any { it.contains("+49") })
        assertTrue(requestedQueries.none { it.contains("372") })
    }

    @Test
    fun `search returns result when snippet matches before fetching page`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.startsWith("/html/") == true -> {
                        MockResponse().setBody(
                            """
                            <html><body>
                              <div class="result">
                                <a class="result__a" href="${server.url("/result")}">Public directory</a>
                                <a class="result__snippet">Contact number +372 616 0245</a>
                              </div>
                            </body></html>
                            """.trimIndent()
                        )
                    }

                    request.path == "/result" -> {
                        MockResponse().setResponseCode(500)
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()

        val crawler = Crawler(searchBaseUrl = server.url("/html/").toString())

        val result = crawler.search("+372 616 0245")

        assertEquals("Public directory — ${server.url("/result")}", result)
    }

    @Test
    fun `search falls back to first search result when no match is found`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.path?.startsWith("/html/") == true -> {
                        MockResponse().setBody(
                            """
                            <html><body>
                              <div class="result">
                                <a class="result__a" href="${server.url("/result")}">Fallback listing</a>
                                <a class="result__snippet">General contact page</a>
                              </div>
                            </body></html>
                            """.trimIndent()
                        )
                    }

                    request.path == "/result" -> {
                        MockResponse().setBody(
                            """
                            <html><head><title>Fallback listing</title></head><body>
                              Generic page without the phone number
                            </body></html>
                            """.trimIndent()
                        )
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()

        val crawler = Crawler(searchBaseUrl = server.url("/html/").toString())

        val result = crawler.search("+372 616 0245")

        assertEquals(Crawler.NO_MATCH_FOUND, result)
    }

    @Test
    fun `search returns no match found when search yields no results`() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setBody("<html><body>No results</body></html>")
            }
        }
        server.start()

        val crawler = Crawler(searchBaseUrl = server.url("/html/").toString())

        val result = crawler.search("+372 616 0245")

        assertEquals(Crawler.NO_MATCH_FOUND, result)
    }
}
