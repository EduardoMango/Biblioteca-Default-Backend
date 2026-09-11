package com.EduardoMango.Biblioteca.feature.book;

import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.author.Author;
import com.EduardoMango.Biblioteca.feature.author.AuthorRepository;
import com.EduardoMango.Biblioteca.feature.book.dto.BookCoverUpdateRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookCreateRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookPatchRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.BookUpdateRequest;
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
        Author a1 = authorRepository.save(Author.builder().nombre("Robert").apellido("Cecil " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
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
    @DisplayName("Consultar libro por ISBN exitosamente")
    void testConsultarLibroPorIsbn() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat Get " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a1 = authorRepository.save(Author.builder().nombre("Autor").apellido("Get " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String isbn = "978-1234567890";
        Book book = Book.builder()
                .isbn(isbn)
                .titulo("Libro Consulta")
                .stockTotal(3)
                .stockDisponible(3)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(a1)))
                .build();
        bookRepository.save(book);

        mockMvc.perform(get("/api/libros/" + isbn)
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.titulo").value("Libro Consulta"));
    }

    @Test
    @DisplayName("Escenario 3: Edición de datos del libro")
    void testEditarLibroExitosamente() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat Edit " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a1 = authorRepository.save(Author.builder().nombre("Autor").apellido("Uno " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a2 = authorRepository.save(Author.builder().nombre("Autor").apellido("Dos " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String isbn = "978-8888888888";
        Book book = Book.builder()
                .isbn(isbn)
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

        mockMvc.perform(put("/api/libros/" + isbn)
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn))
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

        String isbn = "978-7777777777";
        // stockTotal = 5, stockDisponible = 2 -> prestados = 3
        Book book = Book.builder()
                .isbn(isbn)
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

        mockMvc.perform(put("/api/libros/" + isbn)
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

    @Test
    @DisplayName("Escenario 5: Crear libro con urlPortada y verificar que se retorne en GET")
    void testCrearLibroConUrlPortadaYConsultar() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat Cover " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a1 = authorRepository.save(Author.builder().nombre("Autor").apellido("Cover " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String isbn = "978-COVER-" + UUID.randomUUID().toString().substring(0, 8);
        String urlPortada = "https://images.example.com/books/cover.jpg";

        BookCreateRequest request = new BookCreateRequest(
                isbn,
                "Libro Con Portada",
                urlPortada,
                3,
                cat.getPublicId(),
                List.of(a1.getPublicId())
        );

        mockMvc.perform(post("/api/libros")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.urlPortada").value(urlPortada));

        mockMvc.perform(get("/api/libros/" + isbn)
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.urlPortada").value(urlPortada));
    }

    @Test
    @DisplayName("Escenario 6: Actualizar portada mediante PATCH /api/libros/{isbn}/portada")
    void testActualizarPortadaViaPatchSubrecurso() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat Patch " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a1 = authorRepository.save(Author.builder().nombre("Autor").apellido("Patch " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String isbn = "978-PATCH-" + UUID.randomUUID().toString().substring(0, 8);
        Book book = Book.builder()
                .isbn(isbn)
                .titulo("Libro Para Patch Portada")
                .stockTotal(4)
                .stockDisponible(4)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(a1)))
                .build();
        bookRepository.save(book);

        String nuevaPortada = "https://images.example.com/books/new-cover.jpg";
        BookCoverUpdateRequest patchRequest = new BookCoverUpdateRequest(nuevaPortada);

        mockMvc.perform(patch("/api/libros/" + isbn + "/portada")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.urlPortada").value(nuevaPortada));
    }

    @Test
    @DisplayName("Escenario 7: Actualizar portada mediante PATCH general /api/libros/{isbn}")
    void testActualizarPortadaViaPatchGeneral() throws Exception {
        Category cat = categoryRepository.save(Category.builder().nombre("Cat GenPatch " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());
        Author a1 = authorRepository.save(Author.builder().nombre("Autor").apellido("GenPatch " + UUID.randomUUID()).publicId(UUID.randomUUID()).build());

        String isbn = "978-GEN-" + UUID.randomUUID().toString().substring(0, 8);
        Book book = Book.builder()
                .isbn(isbn)
                .titulo("Libro Para General Patch")
                .stockTotal(4)
                .stockDisponible(4)
                .categoria(cat)
                .autores(new ArrayList<>(List.of(a1)))
                .build();
        bookRepository.save(book);

        String nuevaPortada = "https://images.example.com/books/gen-patch-cover.jpg";
        BookPatchRequest patchRequest = new BookPatchRequest(nuevaPortada);

        mockMvc.perform(patch("/api/libros/" + isbn)
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.urlPortada").value(nuevaPortada));
    }

    @Test
    @DisplayName("Caso adicional: Socio no tiene permisos para actualizar portada (403 Forbidden)")
    void testSocioNoPuedeActualizarPortada() throws Exception {
        BookCoverUpdateRequest patchRequest = new BookCoverUpdateRequest("https://images.example.com/cover.jpg");

        mockMvc.perform(patch("/api/libros/978-0000000000/portada")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Caso adicional: Validar que URL de portada mayor a 1000 caracteres retorna 400 Bad Request")
    void testValidacionUrlPortadaExcediendoTamanio() throws Exception {
        String longUrl = "https://images.example.com/" + "a".repeat(1000);
        BookCoverUpdateRequest patchRequest = new BookCoverUpdateRequest(longUrl);

        mockMvc.perform(patch("/api/libros/978-0000000000/portada")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isBadRequest());
    }
}

