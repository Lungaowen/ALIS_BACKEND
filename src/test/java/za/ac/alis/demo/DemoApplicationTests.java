package za.ac.alis.demo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;

@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }

    @TestConfiguration
    static class TestFirebaseConfig {

        @Bean
        Bucket bucket() {
            Bucket bucket = mock(Bucket.class);
            when(bucket.getStorage()).thenReturn(mock(Storage.class));
            return bucket;
        }
    }
}
