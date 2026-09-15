package com.mricotta.cqrs.command.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mricotta.cqrs.command.dto.ProductRequest;
import com.mricotta.cqrs.command.dto.ProductResponse;
import com.mricotta.cqrs.command.exception.ProductNotFoundException;
import com.mricotta.cqrs.command.service.ProductService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String VALID_BODY = """
            {"name":"Mouse","description":"Wireless mouse","price":19.99,"stock":10}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createProduct_returnsCreatedWithLocationAndBody() throws Exception {
        given(productService.createProduct(any(ProductRequest.class))).willReturn(response(1L));

        mockMvc.perform(post("/v1/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/v1/products/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mouse"))
                .andExpect(jsonPath("$.price").value(19.99));
    }

    @Test
    void createProduct_withInvalidBody_returnsBadRequestWithFieldErrors() throws Exception {
        var invalidBody = """
                {"name":"","price":-1,"stock":null}
                """;

        mockMvc.perform(post("/v1/products").contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists())
                .andExpect(jsonPath("$.fieldErrors.stock").exists());

        verifyNoInteractions(productService);
    }

    @Test
    void createProduct_withMalformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/v1/products").contentType(MediaType.APPLICATION_JSON).content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void updateProduct_returnsOk() throws Exception {
        given(productService.updateProduct(eq(1L), any(ProductRequest.class))).willReturn(response(1L));

        mockMvc.perform(put("/v1/products/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    void updateProduct_whenMissing_returnsNotFound() throws Exception {
        given(productService.updateProduct(eq(99L), any(ProductRequest.class)))
                .willThrow(new ProductNotFoundException(99L));

        mockMvc.perform(put("/v1/products/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product with id 99 not found"))
                .andExpect(jsonPath("$.path").value("/v1/products/99"));
    }

    @Test
    void preflight_fromAllowedOrigin_allowsPostWithJsonBody() throws Exception {
        mockMvc.perform(options("/v1/products")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));

        verifyNoInteractions(productService);
    }

    @Test
    void preflight_fromUnknownOrigin_isRejected() throws Exception {
        mockMvc.perform(options("/v1/products")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private static ProductResponse response(Long id) {
        var now = Instant.now();
        return new ProductResponse(id, "Mouse", "Wireless mouse", new BigDecimal("19.99"), 10, now, now);
    }
}
