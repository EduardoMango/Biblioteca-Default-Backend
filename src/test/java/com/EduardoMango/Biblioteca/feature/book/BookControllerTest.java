package com.EduardoMango.Biblioteca.feature.book;

import com.EduardoMango.Biblioteca.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.author.domain.Author;
import com.EduardoMango.Biblioteca.feature.author.repository.AuthorRepository;
import com.EduardoMango.Biblioteca.feature.book.domain.Book;
import com.EduardoMango.Biblioteca.feature.book.dto.BookCreateRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookUpdateRequest;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
import com.EduardoMango.Biblioteca.feature.category.domain.Category;
import com.EduardoMango.Biblioteca.feature.category.repository.CategoryRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class BookControllerTest {

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
    private String bibliotecarioToken;
    private String socioToken;

    @BeforeEach
    void setUp() throws Exception {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Login bibliotecario
        AuthRequest biblioRequest = new AuthRequest("bibliotecario", "admin123");
        MvcResult biblioResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(biblioRequest)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode biblioJson = objectMapper.readTree(biblioResult.getResponse().getContentAsString());
        bibliotecarioToken = biblioJson.get("accessToken").asText();

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
    @DisplayName("Escenario 1: Crear un libro exitosamente con rol BIBLIOTECARIO")
    void testCrearLibroExitosamente() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Ingeniería de Software " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a1 = authorRepository.save(Author.builder().nombre("Robert").apellido("Martin " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a2 = authorRepository.save(Author.builder().nombre("Dean").apellido("Wampler " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        BookCreateRequest request = new BookCreateRequest(
                "978-0132350884",
                "Clean Code",
                5,
                cat.getPublicId(),
                List.of(a1.getPublicId(), a2.getPublicId())
        );

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.isbn").value("978-0132350884"))
                .andExpect(jsonPath("$.titulo").value("Clean Code"))
                .andExpect(jsonPath("$.stockTotal").value(5))
                .andExpect(jsonPath("$.stockDisponible").value(5))
                .andExpect(jsonPath("$.categoria.publicId").value(cat.getPublicId().toString()))
                .andExpect(jsonPath("$.autores", hasSize(2)));
    }

    @Test
    @DisplayName("Escenario 2: Intento de registro con ISBN duplicado")
    void testCrearLibroConIsbnDuplicado() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a1 = authorRepository.save(Author.builder().nombre("Autor").apellido("Test " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        Book existing = Book.builder()
                .isbn("978-9999999999")
                .titulo("Libro Existente")
                .stockTotal(3)
                .stockDisponible(3)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(a1)))
                .publicId(UUID.randomUUID())
                .build();
        bookRepository.save(existing);

        BookCreateRequest duplicate = new BookCreateRequest(
                "978-9999999999",
                "Otro Título",
                2,
                cat.getPublicId(),
                List.of(a1.getPublicId())
        );

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Ya existe un libro registrado con el ISBN ingresado"));
    }

    @Test
    @DisplayName("Escenario 3: Edición de datos del libro")
    void testEditarLibroExitosamente() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat Edit " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a1 = authorRepository.save(Author.builder().nombre("Autor").apellido("Uno " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a2 = authorRepository.save(Author.builder().nombre("Autor").apellido("Dos " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        UUID bookId = UUID.randomUUID();
        Book book = Book.builder()
                .publicId(bookId)
                .isbn("978-8888888888")
                .titulo("Título Original")
                .stockTotal(4)
                .stockDisponible(4)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(a1)))
                .build();
        bookRepository.save(book);

        BookUpdateRequest updateRequest = new BookUpdateRequest(
                "978-8888888888",
                "Título Editado",
                6,
                cat.getPublicId(),
                List.of(a1.getPublicId(), a2.getPublicId())
        );

        mockMvc.perform(put("/api/libros/" + bookId)
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(bookId.toString()))
                .andExpect(jsonPath("$.titulo").value("Título Editado"))
                .andExpect(jsonPath("$.stockTotal").value(6))
                .andExpect(jsonPath("$.stockDisponible").value(6))
                .andExpect(jsonPath("$.autores", hasSize(2)));
    }

    @Test
    @DisplayName("Escenario 4: Intento de reducción de stock por debajo de lo prestado en edición")
    void testReduccionStockPorDebajoDePrestadosRechaza() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat Stock " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a1 = authorRepository.save(Author.builder().nombre("Autor").apellido("Stock " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        UUID bookId = UUID.randomUUID();
        // stockTotal = 5, stockDisponible = 2 -> prestados = 3
        Book book = Book.builder()
                .publicId(bookId)
                .isbn("978-7777777777")
                .titulo("Libro con Prestamos")
                .stockTotal(5)
                .stockDisponible(2)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(a1)))
                .build();
        bookRepository.save(book);

        // Intenta fijar stockTotal en 2 (< 3 prestados)
        BookUpdateRequest updateRequest = new BookUpdateRequest(
                "978-7777777777",
                "Libro con Prestamos",
                2,
                cat.getPublicId(),
                List.of(a1.getPublicId())
        );

        mockMvc.perform(put("/api/libros/" + bookId)
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No se puede reducir el stock total por debajo del número de copias prestadas"));
    }

    @Test
    @DisplayName("Caso adicional: Socio no tiene permisos para crear libro (403 Forbidden)")
    void testSocioNoPuedeCrearLibro() throws Exception {
        BookCreateRequest request = new BookCreateRequest("978-111", "T", 1, UUID.randomUUID(), List.of(UUID.randomUUID()));

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

