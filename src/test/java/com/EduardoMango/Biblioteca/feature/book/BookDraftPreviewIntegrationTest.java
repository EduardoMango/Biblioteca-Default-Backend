package com.EduardoMango.Biblioteca.feature.book;

import com.EduardoMango.Biblioteca.domain.book.dto.ExternalBookDto;
import com.EduardoMango.Biblioteca.domain.book.port.out.ExternalBookSearchPort;
import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.author.Author;
import com.EduardoMango.Biblioteca.feature.author.AuthorRepository;
import com.EduardoMango.Biblioteca.feature.book.dto.BookCreateRequest;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BookDraftPreviewIntegrationTest {

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
    }

    @Test
    @DisplayName("Escenario 1: Obtención exitosa del borrador para pre-llenado (no existe en BD local)")
    void testPreviewDraft_Success_NotInLocalCatalog() throws Exception {
        String isbn = "9780" + String.format("%09d", Math.abs(UUID.randomUUID().hashCode() % 1000000000));

        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Domain-Driven Design",
                List.of("Eric Evans"),
                "Addison-Wesley",
                "Tackling Complexity in the Heart of Software",
                "https://books.google.com/ddd-cover.jpg",
                List.of("Computers", "Software Engineering"),
                560,
                "2003"
        );

        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/books/external/isbn/" + isbn)
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.title").value("Domain-Driven Design"))
                .andExpect(jsonPath("$.authors", hasSize(1)))
                .andExpect(jsonPath("$.authors[0]").value("Eric Evans"))
                .andExpect(jsonPath("$.publisher").value("Addison-Wesley"))
                .andExpect(jsonPath("$.description").value("Tackling Complexity in the Heart of Software"))
                .andExpect(jsonPath("$.coverUrl").value("https://books.google.com/ddd-cover.jpg"))
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0]").value("Computers"))
                .andExpect(jsonPath("$.categories[1]").value("Software Engineering"))
                .andExpect(jsonPath("$.alreadyExistsInLocalCatalog").value(false));

        // Comprobación de que no se guardó ningún registro en la base de datos local
        assertFalse(bookRepository.existsByIsbn(isbn));
    }

    @Test
    @DisplayName("Escenario 2: Advertencia de duplicado en borrador (ya existe en BD local)")
    void testPreviewDraft_Warning_AlreadyExistsInLocalCatalog() throws Exception {
        Category category = categoryRepository.save(Category.builder().nombre("CatPreview-" + UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Eric").apellido("Evans").build());

        String isbn = "9781" + String.format("%09d", Math.abs(UUID.randomUUID().hashCode() % 1000000000));

        bookRepository.save(Book.builder()
                .isbn(isbn)
                .titulo("DDD Existente")
                .stockTotal(3)
                .stockDisponible(3)
                .categoria(category)
                .autores(new ArrayList<>(List.of(author)))
                .publicId(UUID.randomUUID())
                .build());

        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Domain-Driven Design",
                List.of("Eric Evans"),
                "Addison-Wesley",
                "Tackling Complexity",
                "https://books.google.com/ddd-cover.jpg",
                List.of("Software Architecture")
        );

        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/books/external/isbn/" + isbn)
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.title").value("Domain-Driven Design"))
                .andExpect(jsonPath("$.alreadyExistsInLocalCatalog").value(true));
    }

    @Test
    @DisplayName("Escenario 3: Alta manual curada posterior al pre-fill")
    void testCuratedManualCreationAfterPrefill() throws Exception {
        Category category = categoryRepository.save(Category.builder().nombre("Arquitectura-" + UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Martin").apellido("Fowler").build());

        String isbn = "9783" + String.format("%09d", Math.abs(UUID.randomUUID().hashCode() % 1000000000));

        // 1. Pre-fill: Consulta del borrador desde Google Books
        ExternalBookDto draftDto = new ExternalBookDto(
                isbn,
                "Patterns of Enterprise Application Architecture (Draft)",
                List.of("Martin Fowler"),
                "Addison-Wesley",
                "Enterprise patterns reference guide",
                "https://books.google.com/poeaa.jpg",
                List.of("Software Design")
        );
        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(draftDto));

        MvcResult draftResult = mockMvc.perform(get("/api/v1/books/external/isbn/" + isbn)
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(draftResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Patterns of Enterprise Application Architecture (Draft)"))
                .andExpect(jsonPath("$.alreadyExistsInLocalCatalog").value(false));

        // 2. Curaduría: El bibliotecario edita el título y detalles para el alta manual POST /api/v1/books
        String curatedTitle = "Patterns of Enterprise Application Architecture - Edición Curada";
        BookCreateRequest createRequest = new BookCreateRequest(
                isbn,
                curatedTitle,
                "https://books.google.com/poeaa.jpg",
                7,
                category.getPublicId(),
                List.of(author.getPublicId()),
                "Addison-Wesley Professional",
                "Guía de patrones empresariales revisada por bibliotecario"
        );

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.titulo").value(curatedTitle))
                .andExpect(jsonPath("$.stockTotal").value(7))
                .andExpect(jsonPath("$.stockDisponible").value(7))
                .andExpect(jsonPath("$.editorial").value("Addison-Wesley Professional"))
                .andExpect(jsonPath("$.descripcion").value("Guía de patrones empresariales revisada por bibliotecario"));

        // 3. Verificar persistencia exacta en base de datos
        Book persisted = bookRepository.findByIsbn(isbn).orElseThrow();
        assertEquals(curatedTitle, persisted.getTitulo());
        assertEquals(7, persisted.getStockTotal());
        assertEquals(7, persisted.getStockDisponible());
        assertEquals("Addison-Wesley Professional", persisted.getEditorial());
        assertEquals("Guía de patrones empresariales revisada por bibliotecario", persisted.getDescripcion());
    }

    @Test
    @DisplayName("Previsualización de ISBN inexistente en Google Books retorna 404")
    void testPreviewDraft_NotFoundExternal_Returns404() throws Exception {
        String notFoundIsbn = "0000000000000";
        when(externalBookSearchPort.findByIsbn(notFoundIsbn)).thenReturn(Mono.empty());

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/books/external/isbn/" + notFoundIsbn)
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Libro no encontrado")));
    }
}

