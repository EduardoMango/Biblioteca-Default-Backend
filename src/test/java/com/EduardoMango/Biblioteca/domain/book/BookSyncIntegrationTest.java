package com.EduardoMango.Biblioteca.domain.book;

import com.EduardoMango.Biblioteca.domain.book.dto.ExternalBookDto;
import com.EduardoMango.Biblioteca.domain.book.port.out.ExternalBookSearchPort;
import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.author.Author;
import com.EduardoMango.Biblioteca.feature.author.AuthorRepository;
import com.EduardoMango.Biblioteca.feature.book.Book;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.Category;
import com.EduardoMango.Biblioteca.feature.category.CategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class BookSyncIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @MockitoBean
    private ExternalBookSearchPort externalBookSearchPort;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String bibliotecarioToken;
    private String socioToken;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        AuthRequest biblioRequest = new AuthRequest("bibliotecario", "admin123");
        MvcResult biblioResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(biblioRequest)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode biblioJson = objectMapper.readTree(biblioResult.getResponse().getContentAsString());
        bibliotecarioToken = biblioJson.get("accessToken").asText();

        AuthRequest socioRequest = new AuthRequest("socio_juan", "socio123");
        MvcResult socioResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(socioRequest)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode socioJson = objectMapper.readTree(socioResult.getResponse().getContentAsString());
        socioToken = socioJson.get("accessToken").asText();
    }

    @Test
    @DisplayName("Escenario 1: Enriquecimiento exitoso de campos vacíos")
    void testEnriquecimientoCamposVacios() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("CatSync-" + UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Autor").apellido("Sync").build());

        String isbn = "9781" + String.format("%09d", Math.abs(UUID.randomUUID().hashCode() % 1000000000));
        Book book = bookRepository.save(Book.builder()
                .isbn(isbn)
                .titulo("Título Local Fijo")
                .urlPortada(null)
                .descripcion(null)
                .editorial(null)
                .stockTotal(5)
                .stockDisponible(3)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(author)))
                .publicId(UUID.randomUUID())
                .build());

        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Effective Java 3rd Ed",
                List.of("Joshua Bloch"),
                "Addison-Wesley",
                "Best practices Java guide",
                "https://books.google.com/cover.jpg",
                List.of("Computers")
        );

        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/books/" + book.getPublicId() + "/sync-google-books")
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Título Local Fijo"))
                .andExpect(jsonPath("$.urlPortada").value("https://books.google.com/cover.jpg"))
                .andExpect(jsonPath("$.descripcion").value("Best practices Java guide"))
                .andExpect(jsonPath("$.editorial").value("Addison-Wesley"))
                .andExpect(jsonPath("$.stockTotal").value(5))
                .andExpect(jsonPath("$.stockDisponible").value(3));

        Book updated = bookRepository.findByPublicId(book.getPublicId()).orElseThrow();
        assertEquals("Título Local Fijo", updated.getTitulo());
        assertEquals("https://books.google.com/cover.jpg", updated.getUrlPortada());
        assertEquals(5, updated.getStockTotal());
        assertEquals(3, updated.getStockDisponible());
    }

    @Test
    @DisplayName("Escenario 2: Sobrescritura forzada de metadatos (force = true)")
    void testSobrescrituraForzadaMetadatos() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("CatForce-" + UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Autor").apellido("Force").build());

        String isbn = "9782" + String.format("%09d", Math.abs(UUID.randomUUID().hashCode() % 1000000000));
        Book book = bookRepository.save(Book.builder()
                .isbn(isbn)
                .titulo("Título Antiguo")
                .urlPortada("https://example.com/old.jpg")
                .descripcion("Descripción Antigua")
                .editorial("Editorial Antigua")
                .stockTotal(10)
                .stockDisponible(4)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(author)))
                .publicId(UUID.randomUUID())
                .build());

        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Título Más Reciente",
                List.of("Joshua Bloch"),
                "Editorial Nueva",
                "Descripción Nueva",
                "https://books.google.com/new.jpg",
                List.of("Computers")
        );

        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/books/" + book.getPublicId() + "/sync-google-books?force=true")
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Título Más Reciente"))
                .andExpect(jsonPath("$.editorial").value("Editorial Nueva"))
                .andExpect(jsonPath("$.descripcion").value("Descripción Nueva"))
                .andExpect(jsonPath("$.urlPortada").value("https://books.google.com/new.jpg"))
                .andExpect(jsonPath("$.stockTotal").value(10))
                .andExpect(jsonPath("$.stockDisponible").value(4));

        Book updated = bookRepository.findByPublicId(book.getPublicId()).orElseThrow();
        assertEquals("Título Más Reciente", updated.getTitulo());
        assertEquals("https://books.google.com/new.jpg", updated.getUrlPortada());
        assertEquals(10, updated.getStockTotal());
        assertEquals(4, updated.getStockDisponible());
    }

    @Test
    @DisplayName("Escenario 3: Intento de sincronización de un libro sin ISBN registrado")
    void testSincronizarLibroSinIsbn() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("CatNoIsbn-" + UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Autor").apellido("NoIsbn").build());

        Book bookWithoutIsbn = bookRepository.save(Book.builder()
                .isbn(null)
                .titulo("Libro Sin ISBN")
                .stockTotal(2)
                .stockDisponible(2)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(author)))
                .publicId(UUID.randomUUID())
                .build());

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/books/" + bookWithoutIsbn.getPublicId() + "/sync-google-books")
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("No se puede sincronizar un libro sin ISBN asociado")));
    }

    @Test
    @DisplayName("Caso adicional: Socio no tiene permisos para sincronizar con Google Books (403 Forbidden)")
    void testSocioNoPuedeSincronizar() throws Exception {
        mockMvc.perform(post("/api/v1/books/" + UUID.randomUUID() + "/sync-google-books")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isForbidden());
    }
}

