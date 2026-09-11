package com.EduardoMango.Biblioteca.feature.book;

import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.author.Author;
import com.EduardoMango.Biblioteca.feature.author.AuthorRepository;
import com.EduardoMango.Biblioteca.feature.book.dto.StockAdjustmentRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BookStockAdjustmentTest {

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
    @DisplayName("Escenario 1: Incrementar stock por compra de nuevos ejemplares")
    void testIncrementarStockExitoso() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat StockInc " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Autor").apellido("StockInc " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String isbn = "978-INC-" + UUID.randomUUID().toString().substring(0, 8);
        // stockTotal = 10, stockDisponible = 4 (en préstamo = 6)
        Book book = Book.builder()
                .isbn(isbn)
                .titulo("Libro Para Incrementar")
                .stockTotal(10)
                .stockDisponible(4)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(author)))
                .build();
        bookRepository.save(book);

        StockAdjustmentRequest request = new StockAdjustmentRequest(15);

        mockMvc.perform(patch("/api/libros/" + isbn + "/stock")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockTotal").value(15))
                .andExpect(jsonPath("$.stockDisponible").value(9));

        Book updated = bookRepository.findByIsbn(isbn).orElseThrow();
        assertEquals(15, updated.getStockTotal());
        assertEquals(9, updated.getStockDisponible());
    }

    @Test
    @DisplayName("Escenario 2: Reducir stock por deterioro o pérdida de ejemplares")
    void testReducirStockExitoso() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat StockDec " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Autor").apellido("StockDec " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String isbn = "978-DEC-" + UUID.randomUUID().toString().substring(0, 8);
        // stockTotal = 5, stockDisponible = 3 (en préstamo = 2)
        Book book = Book.builder()
                .isbn(isbn)
                .titulo("Libro Para Reducir")
                .stockTotal(5)
                .stockDisponible(3)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(author)))
                .build();
        bookRepository.save(book);

        StockAdjustmentRequest request = new StockAdjustmentRequest(3);

        mockMvc.perform(patch("/api/libros/" + isbn + "/stock")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockTotal").value(3))
                .andExpect(jsonPath("$.stockDisponible").value(1));

        Book updated = bookRepository.findByIsbn(isbn).orElseThrow();
        assertEquals(3, updated.getStockTotal());
        assertEquals(1, updated.getStockDisponible());
    }

    @Test
    @DisplayName("Escenario 3: Intentar reducir stock por debajo de la cantidad prestada")
    void testReducirStockPorDebajoDePrestadosRechaza() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat StockErr " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author author = authorRepository.save(Author.builder().nombre("Autor").apellido("StockErr " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String isbn = "978-ERR-" + UUID.randomUUID().toString().substring(0, 8);
        // stockTotal = 10, stockDisponible = 2 (en préstamo = 8)
        Book book = Book.builder()
                .isbn(isbn)
                .titulo("Libro Mucho Prestamo")
                .stockTotal(10)
                .stockDisponible(2)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(author)))
                .build();
        bookRepository.save(book);

        StockAdjustmentRequest request = new StockAdjustmentRequest(5);

        mockMvc.perform(patch("/api/libros/" + isbn + "/stock")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El nuevo stock total no puede ser inferior a las 8 copias actualmente prestadas"));

        Book unchanged = bookRepository.findByIsbn(isbn).orElseThrow();
        assertEquals(10, unchanged.getStockTotal());
        assertEquals(2, unchanged.getStockDisponible());
    }

    @Test
    @DisplayName("Caso adicional: Socio no tiene permisos para ajustar stock (403 Forbidden)")
    void testSocioNoPuedeAjustarStock() throws Exception {
        StockAdjustmentRequest request = new StockAdjustmentRequest(10);

        mockMvc.perform(patch("/api/libros/978-0000000000/stock")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

