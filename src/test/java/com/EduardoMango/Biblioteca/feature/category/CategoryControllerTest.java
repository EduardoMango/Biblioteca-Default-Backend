package com.EduardoMango.Biblioteca.feature.category;

import com.EduardoMango.Biblioteca.feature.auth.dto.AuthRequest;
import com.EduardoMango.Biblioteca.feature.category.dto.CategoryRequest;
import com.EduardoMango.Biblioteca.feature.category.service.CategoryDeletionValidator;
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
class CategoryControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private CategoryDeletionValidator categoryDeletionValidator;

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
    @DisplayName("Escenario 1: Crear categoría exitosamente con rol BIBLIOTECARIO")
    void testCrearCategoriaExitosamente() throws Exception {
        CategoryRequest request = new CategoryRequest("Ciencia Ficción", "Novelas y relatos del género de ciencia ficción");

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Ciencia Ficción"))
                .andExpect(jsonPath("$.descripcion").value("Novelas y relatos del género de ciencia ficción"));
    }

    @Test
    @DisplayName("Escenario 2: Intento de crear categoría con nombre duplicado")
    void testCrearCategoriaConNombreDuplicado() throws Exception {
        Category category = Category.builder()
                .nombre("Programación")
                .descripcion("Libros de código")
                .publicId(UUID.randomUUID())
                .build();
        categoryRepository.save(category);

        CategoryRequest duplicateRequest = new CategoryRequest("programación", "Otra descripción");

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Ya existe una categoría con el nombre especificado"));
    }

    @Test
    @DisplayName("Escenario 3: Modificar categoría existente")
    void testModificarCategoriaExistente() throws Exception {
        UUID publicId = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
        Category category = Category.builder()
                .nombre("Historia Antigua")
                .descripcion("Libros de historia antigua")
                .publicId(publicId)
                .build();
        categoryRepository.save(category);

        CategoryRequest updateRequest = new CategoryRequest("Historia Contemporánea", "Actualizado");

        mockMvc.perform(put("/api/categorias/" + publicId)
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicId").value(publicId.toString()))
                .andExpect(jsonPath("$.nombre").value("Historia Contemporánea"))
                .andExpect(jsonPath("$.descripcion").value("Actualizado"));
    }

    @Test
    @DisplayName("Escenario 4: Eliminar categoría con libros asociados rechaza la operación")
    void testEliminarCategoriaConLibrosAsociados() throws Exception {
        UUID publicId = UUID.fromString("b1fbc999-9c0b-4ef8-bb6d-6bb9bd380a22");
        Category category = Category.builder()
                .nombre("Biografías")
                .descripcion("Memorias")
                .publicId(publicId)
                .build();
        categoryRepository.save(category);

        when(categoryDeletionValidator.hasAssociatedBooks(publicId)).thenReturn(true);

        mockMvc.perform(delete("/api/categorias/" + publicId)
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No se puede eliminar la categoría porque tiene libros asociados"));
    }

    @Test
    @DisplayName("Escenario 5: Buscar categoría por publicId inexistente retorna 404")
    void testBuscarCategoriaPorPublicIdInexistente() throws Exception {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        mockMvc.perform(get("/api/categorias/" + nonExistentId)
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(containsString("Categoría no encontrada")));
    }

    @Test
    @DisplayName("Caso adicional: Socio no tiene permisos para crear categoría (403 Forbidden)")
    void testSocioNoPuedeCrearCategoria() throws Exception {
        CategoryRequest request = new CategoryRequest("Poesía", "Versos");

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", "Bearer " + socioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Caso adicional: Validar campos obligatorios al crear categoría")
    void testValidacionCamposObligatorios() throws Exception {
        CategoryRequest request = new CategoryRequest("", "Sin nombre");

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", "Bearer " + bibliotecarioToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Caso adicional: Listar categorías con paginación")
    void testListarCategoriasPaginado() throws Exception {
        mockMvc.perform(get("/api/categorias")
                        .header("Authorization", "Bearer " + socioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    @DisplayName("Caso adicional: Eliminar categoría sin libros asociados exitosamente")
    void testEliminarCategoriaSinLibrosExitoso() throws Exception {
        UUID publicId = UUID.randomUUID();
        Category category = Category.builder()
                .nombre("Misterio")
                .descripcion("Novelas de suspenso")
                .publicId(publicId)
                .build();
        categoryRepository.save(category);

        when(categoryDeletionValidator.hasAssociatedBooks(publicId)).thenReturn(false);

        mockMvc.perform(delete("/api/categorias/" + publicId)
                        .header("Authorization", "Bearer " + bibliotecarioToken))
                .andExpect(status().isNoContent());
    }
}
