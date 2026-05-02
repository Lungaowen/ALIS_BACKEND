package za.ac.alis.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.stream()
                .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .forEach(this::supportTextPlainJson);
    }

    private void supportTextPlainJson(MappingJackson2HttpMessageConverter converter) {
        List<MediaType> supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        if (!supportedMediaTypes.contains(MediaType.TEXT_PLAIN)) {
            supportedMediaTypes.add(MediaType.TEXT_PLAIN);
            converter.setSupportedMediaTypes(supportedMediaTypes);
        }
    }
}
