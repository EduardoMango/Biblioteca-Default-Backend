package com.EduardoMango.Biblioteca.feature.book;

import com.EduardoMango.Biblioteca.domain.book.dto.ExternalBookDto;
import com.EduardoMango.Biblioteca.domain.book.port.out.ExternalBookSearchPort;
import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.book.dto.ImportBookRequest;
import com.EduardoMango.Biblioteca.feature.book.repository.BookRepository;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class BookImportIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BookRepository bookRepository;

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
    @DisplayName("Escenario 1: Alta automática exitosa de un libro por ISBN y stock")
    void testImportBook_Success() throws Exception {
        String isbn = "9780134685991";
        bookRepository.deleteByIsbn(isbn);

        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Effective Java",
                List.of("Joshua Bloch"),
                "Addison-Wesley Professional",
                "Best practices guide",
                "https://books.google.com/cover.jpg",
                List.of("Computers"),
                412,
                "2018"
        );

        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));

        ImportBookRequest request = new ImportBookRequest(isbn, 5);

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/books/import")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.titulo").value("Effective Java"))
                .andExpect(jsonPath("$.stockTotal").value(5))
                .andExpect(jsonPath("$.stockDisponible").value(5))
                .andExpect(jsonPath("$.categoria.nombre").value("Computers"))
                .andExpect(jsonPath("$.autores", hasSize(1)))
                .andExpect(jsonPath("$.autores[0].nombre").value("Joshua"))
                .andExpect(jsonPath("$.autores[0].apellido").value("Bloch"));

        assertTrue(bookRepository.existsByIsbn(isbn));
    }

    @Test
    @DisplayName("Escenario 2: Intento de importación de un ISBN ya existente en la biblioteca")
    void testImportBook_DuplicateIsbn_Returns409Conflict() throws Exception {
        String isbn = "9780134685991";

        // Asegurar que existe en BD
        if (!bookRepository.existsByIsbn(isbn)) {
            ExternalBookDto externalDto = new ExternalBookDto(
                    isbn, "Existing Book", List.of("Author"), "Pub", "Desc", null, List.of("General")
            );
            when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));
            MvcResult res = mockMvc.perform(post("/api/v1/books/import")
                            .header("Authorization", "Bearer " + bibliotecarioToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ImportBookRequest(isbn, 3))))
                    .andReturn();
            if (res.getRequest().isAsyncStarted()) {
                mockMvc.perform(asyncDispatch(res));
            }
        }

        reset(externalBookSearchPort);

        ImportBookRequest duplicateRequest = new ImportBookRequest(isbn, 5);

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/books/import")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value(containsString("El ISBN ya pertenece al catálogo local")));

        verifyNoInteractions(externalBookSearchPort);
    }

    @Test
    @DisplayName("Escenario 3: Búsqueda/Previsualización sin persistencia")
    void testPreviewExternalBook_Success() throws Exception {
        String isbn = "9780134685992";
        bookRepository.deleteByIsbn(isbn);

        ExternalBookDto externalDto = new ExternalBookDto(
                isbn,
                "Clean Architecture",
                List.of("Robert C. Martin"),
                "Prentice Hall",
                "Architecture guide",
                "https://books.google.com/clean-arch.jpg",
                List.of("Software Engineering"),
                350,
                "2017"
        );

        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.just(externalDto));

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/books/external/isbn/" + isbn)
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(isbn))
                .andExpect(jsonPath("$.title").value("Clean Architecture"))
                .andExpect(jsonPath("$.publisher").value("Prentice Hall"))
                .andExpect(jsonPath("$.coverUrl").value("https://books.google.com/clean-arch.jpg"))
                .andExpect(jsonPath("$.alreadyExistsInLocalCatalog").value(false));

        // Verificar que no se guardó nada en la BD
        org.junit.jupiter.api.Assertions.assertFalse(bookRepository.existsByIsbn(isbn));
    }

    @Test
    @DisplayName("Escenario 4: Intentar importar un ISBN inexistente en Google Books retorna 404")
    void testImportBook_NotFoundExternal_Returns404() throws Exception {
        String isbn = "0000000000000";
        bookRepository.deleteByIsbn(isbn);

        when(externalBookSearchPort.findByIsbn(isbn)).thenReturn(Mono.empty());

        ImportBookRequest request = new ImportBookRequest(isbn, 2);

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/books/import")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value(containsString("no fue encontrado en el proveedor externo")));
    }

    @Test
    @DisplayName("Caso adicional: Socio no tiene permisos para importar libro (403 Forbidden)")
    void testImportBook_ForbiddenForSocio() throws Exception {
        ImportBookRequest request = new ImportBookRequest("9780134685991", 2);

        mockMvc.perform(post("/api/v1/books/import")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Caso adicional: Validación de request de importación con copias menores a 1")
    void testImportBook_InvalidCopies_Returns400() throws Exception {
        ImportBookRequest request = new ImportBookRequest("9780134685991", 0);

        mockMvc.perform(post("/api/v1/books/import")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations.totalCopies").exists());
    }
}

