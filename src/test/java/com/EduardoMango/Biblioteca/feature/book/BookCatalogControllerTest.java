package com.EduardoMango.Biblioteca.feature.book;

import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.author.Author;
import com.EduardoMango.Biblioteca.feature.author.AuthorRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BookCatalogControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String socioToken;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Login socio
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
    @DisplayName("Escenario 1: Consultar catálogo completo paginado")
    void testCatalogoCompletoPaginado() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat Page " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Autor").apellido("Page " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        for (int i = 1; i <= 15; i++) {
            Book book = Book.builder()
                    .isbn("ISBN-PAG-" + UUID.randomUUID())
                    .titulo("Libro Paginado " + i)
                    .stockTotal(5)
                    .stockDisponible(5)
                    .categoria(cat)
                    .autores(new ArrayList<>(List.of(author)))
                    .build();
            bookRepository.save(book);
        }

        mockMvc.perform(get("/api/libros?page=0&size=10")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(15)))
                .andExpect(jsonPath("$.page.totalPages").value(greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("Escenario 2: Filtrar libros solo con stock disponible")
    void testFiltrarLibrosSoloConStockDisponible() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat Stock " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Autor").apellido("Stock " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String prefix = "STOCKTEST_" + UUID.randomUUID() + "_";
        Book bookA = Book.builder().isbn(UUID.randomUUID().toString()).titulo(prefix + "Libro A").stockTotal(2).stockDisponible(2).categoria(cat).autores(new ArrayList<>(List.of(author))).build();
        Book bookB = Book.builder().isbn(UUID.randomUUID().toString()).titulo(prefix + "Libro B").stockTotal(1).stockDisponible(0).categoria(cat).autores(new ArrayList<>(List.of(author))).build();
        Book bookC = Book.builder().isbn(UUID.randomUUID().toString()).titulo(prefix + "Libro C").stockTotal(1).stockDisponible(1).categoria(cat).autores(new ArrayList<>(List.of(author))).build();
        bookRepository.saveAll(List.of(bookA, bookB, bookC));

        mockMvc.perform(get("/api/libros?titulo=" + prefix + "&soloDisponibles=true")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].titulo", containsInAnyOrder(prefix + "Libro A", prefix + "Libro C")));
    }

    @Test
    @DisplayName("Escenario 3: Búsqueda combinada por título y categoría")
    void testBusquedaCombinadaPorTituloYCategoria() throws Exception {
        Category catProg = categoryRepository.save(Category.builder().nombre("Programación " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Category catOther = categoryRepository.save(Category.builder().nombre("Otra " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Autor").apellido("Prog " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String tag = "TAG_" + UUID.randomUUID();
        Book b1 = Book.builder().isbn(UUID.randomUUID().toString()).titulo(tag + " Clean Code").stockTotal(3).stockDisponible(3).categoria(catProg).autores(new ArrayList<>(List.of(author))).build();
        Book b2 = Book.builder().isbn(UUID.randomUUID().toString()).titulo(tag + " Clean Architecture").stockTotal(3).stockDisponible(3).categoria(catProg).autores(new ArrayList<>(List.of(author))).build();
        Book b3 = Book.builder().isbn(UUID.randomUUID().toString()).titulo(tag + " Clean Cooking").stockTotal(3).stockDisponible(3).categoria(catOther).autores(new ArrayList<>(List.of(author))).build();
        Book b4 = Book.builder().isbn(UUID.randomUUID().toString()).titulo(tag + " Java Concurrency").stockTotal(3).stockDisponible(3).categoria(catProg).autores(new ArrayList<>(List.of(author))).build();
        bookRepository.saveAll(List.of(b1, b2, b3, b4));

        mockMvc.perform(get("/api/libros?titulo=clean&categoriaPublicId=" + catProg.getPublicId())
                        .header("Authorization", "Bearer " + socioToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].titulo", containsInAnyOrder(tag + " Clean Code", tag + " Clean Architecture")));
    }

    @Test
    @DisplayName("Escenario 4: Búsqueda sin coincidencias")
    void testBusquedaSinCoincidencias() throws Exception {
        mockMvc.perform(get("/api/libros?titulo=NON_EXISTENT_TITULO_XYZ_99999")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    @DisplayName("Escenario 5: Filtrar libros por ISBN")
    void testFiltrarLibrosPorIsbn() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat ISBN " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Autor").apellido("ISBN " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String targetIsbn = "978-9876543210";
        Book bookTarget = Book.builder().isbn(targetIsbn).titulo("Libro Buscado Por ISBN").stockTotal(2).stockDisponible(2).categoria(cat).autores(new ArrayList<>(List.of(author))).build();
        Book bookOther = Book.builder().isbn("978-1111111111").titulo("Otro Libro").stockTotal(2).stockDisponible(2).categoria(cat).autores(new ArrayList<>(List.of(author))).build();
        bookRepository.saveAll(List.of(bookTarget, bookOther));

        mockMvc.perform(get("/api/libros?isbn=" + targetIsbn)
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].isbn").value(targetIsbn))
                .andExpect(jsonPath("$.content[0].titulo").value("Libro Buscado Por ISBN"));
    }
}
