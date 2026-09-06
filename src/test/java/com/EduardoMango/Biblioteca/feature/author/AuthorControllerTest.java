package com.EduardoMango.Biblioteca.feature.author;

import com.EduardoMango.Biblioteca.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.author.domain.Author;
import com.EduardoMango.Biblioteca.feature.author.dto.AuthorRequest;
import com.EduardoMango.Biblioteca.feature.author.repository.AuthorRepository;
import com.EduardoMango.Biblioteca.feature.author.service.AuthorDeletionValidator;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthorControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuthorRepository authorRepository;

    @MockitoBean
    private AuthorDeletionValidator authorDeletionValidator;

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
    @DisplayName("Escenario 1: Crear autor exitosamente con rol BIBLIOTECARIO")
    void testCrearAutorExitosamente() throws Exception {
        AuthorRequest request = new AuthorRequest("Gabriel", "García Márquez", "Colombiana", null);

        mockMvc.perform(post("/api/autores")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Gabriel"))
                .andExpect(jsonPath("$.apellido").value("García Márquez"))
                .andExpect(jsonPath("$.nacionalidad").value("Colombiana"));
    }

    @Test
    @DisplayName("Escenario 2: Validaciones de campos obligatorios en Request")
    void testValidacionCamposObligatorios() throws Exception {
        AuthorRequest request = new AuthorRequest("", " ", null, null);

        mockMvc.perform(post("/api/autores")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.violations.nombre").isNotEmpty())
                .andExpect(jsonPath("$.violations.apellido").isNotEmpty());
    }

    @Test
    @DisplayName("Escenario 3: Filtrado de autores por nombre/apellido")
    void testFiltradoAutoresPorNombreApellido() throws Exception {
        Author a1 = Author.builder().nombre("Robert").apellido("Martin").nacionalidad("Estadounidense").publicId(UUID.randomUUID()).build();
        Author a2 = Author.builder().nombre("Martin").apellido("Fowler").nacionalidad("Británica").publicId(UUID.randomUUID()).build();
        Author a3 = Author.builder().nombre("Kent").apellido("Beck").nacionalidad("Estadounidense").publicId(UUID.randomUUID()).build();
        authorRepository.saveAll(java.util.List.of(a1, a2, a3));

        mockMvc.perform(get("/api/autores?q=Martin")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nombre", containsInAnyOrder("Robert", "Martin")))
                .andExpect(jsonPath("$[*].apellido", containsInAnyOrder("Martin", "Fowler")));
    }

    @Test
    @DisplayName("Escenario 4: Intentar borrar autor vinculado a libros")
    void testBorrarAutorVinculadoALibrosRechaza() throws Exception {
        UUID publicId = UUID.fromString("c2fbc999-9c0b-4ef8-bb6d-6bb9bd380a33");
        Author author = Author.builder()
                .nombre("Julio")
                .apellido("Cortázar")
                .publicId(publicId)
                .build();
        authorRepository.save(author);

        when(authorDeletionValidator.hasAssociatedBooks(publicId)).thenReturn(true);

        mockMvc.perform(delete("/api/autores/" + publicId)
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El autor no puede ser eliminado por tener obras asociadas"));
    }

    @Test
    @DisplayName("Caso adicional: Socio no tiene permisos para registrar autor (403 Forbidden)")
    void testSocioNoPuedeCrearAutor() throws Exception {
        AuthorRequest request = new AuthorRequest("Julio", "Verne", "Francesa", null);

        mockMvc.perform(post("/api/autores")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Caso adicional: Obtener autor por publicId inexistente retorna 404")
    void testGetAutorInexistenteRetorna404() throws Exception {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        mockMvc.perform(get("/api/autores/" + nonExistentId)
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("Autor no encontrado")));
    }

    @Test
    @DisplayName("Caso adicional: Borrar autor sin obras vinculadas")
    void testBorrarAutorSinObrasExitoso() throws Exception {
        UUID publicId = UUID.randomUUID();
        Author author = Author.builder()
                .nombre("Edgar")
                .apellido("Poe")
                .publicId(publicId)
                .build();
        authorRepository.save(author);

        when(authorDeletionValidator.hasAssociatedBooks(publicId)).thenReturn(false);

        mockMvc.perform(delete("/api/autores/" + publicId)
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(status().isNoContent());
    }
}

