package com.team.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void notFoundExceptionGives404() throws Exception {
        mvc.perform(get("/t/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Team not found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void conflictExceptionGives409() throws Exception {
        mvc.perform(get("/t/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void forbiddenExceptionGives403() throws Exception {
        mvc.perform(get("/t/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_A_MEMBER"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void validationExceptionGives400() throws Exception {
        mvc.perform(get("/t/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void beanValidationGives400WithFieldErrors() throws Exception {
        mvc.perform(post("/t/bean").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void malformedJsonGives400() throws Exception {
        mvc.perform(post("/t/bean").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void wrongPathVariableTypeGives400() throws Exception {
        mvc.perform(get("/t/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unexpectedErrorGives500WithoutLeakingDetails() throws Exception {
        mvc.perform(get("/t/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected error"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(result -> Assertions.assertFalse(
                        result.getResponse().getContentAsString().contains("secret db password")));
    }

    @RestController
    @RequestMapping("/t")
    static class ThrowingController {

        record Body(@NotBlank String name) { }

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException(ErrorCode.TEAM_NOT_FOUND, "Team not found");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException(ErrorCode.EMAIL_TAKEN, "Email already registered");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException(ErrorCode.NOT_A_MEMBER, "Not a member");
        }

        @GetMapping("/validation")
        void validation() {
            throw new ValidationException("Invalid input");
        }

        @PostMapping("/bean")
        void bean(@Valid @RequestBody Body body) { }

        @GetMapping("/typed/{id}")
        void typed(@PathVariable Long id) { }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("secret db password");
        }
    }
}