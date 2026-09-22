package com.EduardoMango.Biblioteca.infrastructure.googlebooks;

import com.EduardoMango.Biblioteca.domain.book.dto.ExternalBookDto;
import com.EduardoMango.Biblioteca.infrastructure.googlebooks.adapter.out.GoogleBooksWebClientAdapter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GoogleBooksWebClientAdapterTest {

    private MockWebServer mockWebServer;
    private GoogleBooksWebClientAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder builder = WebClient.builder();
        adapter = new GoogleBooksWebClientAdapter(builder, baseUrl, "test-api-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Escenario 1: Búsqueda exitosa de libro por ISBN retorna ExternalBookDto completo")
    void testFindByIsbn_Success() throws InterruptedException {
        String jsonBody = """
                {
                  "kind": "books#volumes",
                  "totalItems": 1,
                  "items": [
                    {
                      "id": "abc123",
                      "volumeInfo": {
                        "title": "Effective Java",
                        "authors": ["Joshua Bloch"],
                        "publisher": "Addison-Wesley",
                        "publishedDate": "2018",
                        "description": "Best practices for Java",
                        "pageCount": 412,
                        "categories": ["Computers"],
                        "imageLinks": {
                          "smallThumbnail": "http://books.google.com/small.jpg",
                          "thumbnail": "http://books.google.com/thumb.jpg"
                        },
                        "industryIdentifiers": [
                          {
                            "type": "ISBN_13",
                            "identifier": "9780134685991"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonBody));

        StepVerifier.create(adapter.findByIsbn("9780134685991"))
                .assertNext(dto -> {
                    assertEquals("9780134685991", dto.isbn());
                    assertEquals("Effective Java", dto.title());
                    assertEquals(1, dto.authors().size());
                    assertEquals("Joshua Bloch", dto.authors().get(0));
                    assertEquals("Addison-Wesley", dto.publisher());
                    assertEquals("Best practices for Java", dto.description());
                    assertEquals("https://books.google.com/thumb.jpg", dto.coverUrl());
                    assertEquals(1, dto.categories().size());
                    assertEquals("Computers", dto.categories().get(0));
                })
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertTrue(recordedRequest.getPath().contains("/volumes?q=isbn:9780134685991&key=test-api-key"));
    }

    @Test
    @DisplayName("Escenario 4: Búsqueda de ISBN inexistente en Google Books retorna Mono.empty()")
    void testFindByIsbn_NotFoundReturnsEmpty() {
        String jsonBody = """
                {
                  "kind": "books#volumes",
                  "totalItems": 0
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonBody));

        StepVerifier.create(adapter.findByIsbn("0000000000000"))
                .verifyComplete();
    }

    @Test
    @DisplayName("Búsqueda con respuesta 404 del servidor externo retorna Mono.empty()")
    void testFindByIsbn_Http404ReturnsEmpty() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(adapter.findByIsbn("1111111111111"))
                .verifyComplete();
    }
}

