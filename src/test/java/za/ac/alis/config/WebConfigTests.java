package za.ac.alis.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

import za.ac.alis.dto.LoginRequest;

class WebConfigTests {

    @Test
    void jacksonCanReadJsonSentAsTextPlain() throws Exception {
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(new ObjectMapper());
        new WebConfig().extendMessageConverters(List.of(converter));

        MockHttpInputMessage input = new MockHttpInputMessage(
                "{\"email\":\"user@example.com\",\"password\":\"secret\"}".getBytes(UTF_8));
        input.getHeaders().setContentType(MediaType.TEXT_PLAIN);

        Object body = converter.read(LoginRequest.class, input);

        assertThat(converter.canRead(LoginRequest.class, MediaType.TEXT_PLAIN)).isTrue();
        assertThat(body).isInstanceOf(LoginRequest.class);
        LoginRequest request = (LoginRequest) body;
        assertThat(request.getEmail()).isEqualTo("user@example.com");
        assertThat(request.getPassword()).isEqualTo("secret");
    }
}
