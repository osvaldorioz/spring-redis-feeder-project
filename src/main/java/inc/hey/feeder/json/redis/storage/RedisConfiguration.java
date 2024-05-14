package inc.hey.feeder.json.redis.storage;

import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.transformers.TransformersEmbeddingClient;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.ai.vectorstore.RedisVectorStore.RedisVectorStoreConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RedisConfiguration {

    @Bean
    @Primary
    TransformersEmbeddingClient transformersEmbeddingClient() {
        return new TransformersEmbeddingClient(MetadataMode.EMBED);
    }

    @Bean
    VectorStore vectorStore(TransformersEmbeddingClient embeddingClient, RedisVectorStoreProperties properties) {
        var config = RedisVectorStoreConfig.builder().withURI(properties.getUri()).withIndexName(properties.getIndex())
                .withPrefix(properties.getPrefix()).build();
        RedisVectorStore vectorStore = new RedisVectorStore(config, embeddingClient);
        vectorStore.afterPropertiesSet();
        return vectorStore;
    }

}
