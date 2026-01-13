package com.supervisorservice.config;

import com.commonlibrary.dto.coupon.CouponCancelledEvent;
import com.commonlibrary.dto.coupon.CouponDistributionEvent;
import com.commonlibrary.dto.coupon.CouponExpiredEvent;
import com.commonlibrary.dto.coupon.CouponUsedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for coupon event consumers.
 * Configures deserializers for coupon-related event DTOs.
 */
@Configuration
@EnableKafka
public class CouponKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:supervisor-service-coupon-group}")
    private String groupId;

    /**
     * Consumer factory for CouponDistributionEvent
     */
    @Bean
    public ConsumerFactory<String, CouponDistributionEvent> couponDistributionConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        
        JsonDeserializer<CouponDistributionEvent> deserializer = new JsonDeserializer<>(CouponDistributionEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(false);
        
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    /**
     * Consumer factory for CouponUsedEvent
     */
    @Bean
    public ConsumerFactory<String, CouponUsedEvent> couponUsedConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        
        JsonDeserializer<CouponUsedEvent> deserializer = new JsonDeserializer<>(CouponUsedEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(false);
        
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    /**
     * Consumer factory for CouponCancelledEvent
     */
    @Bean
    public ConsumerFactory<String, CouponCancelledEvent> couponCancelledConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        
        JsonDeserializer<CouponCancelledEvent> deserializer = new JsonDeserializer<>(CouponCancelledEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(false);
        
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    /**
     * Consumer factory for CouponExpiredEvent
     */
    @Bean
    public ConsumerFactory<String, CouponExpiredEvent> couponExpiredConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        
        JsonDeserializer<CouponExpiredEvent> deserializer = new JsonDeserializer<>(CouponExpiredEvent.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(false);
        
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    /**
     * Generic consumer factory for all coupon events
     * Uses Object type to handle any coupon event
     */
    @Bean
    public ConsumerFactory<String, Object> couponEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>(Object.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(false);
        
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    /**
     * Kafka listener container factory for coupon events
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> couponKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(couponEventConsumerFactory());
        factory.setConcurrency(3);
        return factory;
    }

    /**
     * Kafka listener container factory for distribution events
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CouponDistributionEvent> 
            couponDistributionKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CouponDistributionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(couponDistributionConsumerFactory());
        return factory;
    }

    /**
     * Kafka listener container factory for used events
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CouponUsedEvent> 
            couponUsedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CouponUsedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(couponUsedConsumerFactory());
        return factory;
    }

    /**
     * Kafka listener container factory for cancelled events
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CouponCancelledEvent> 
            couponCancelledKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CouponCancelledEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(couponCancelledConsumerFactory());
        return factory;
    }

    /**
     * Kafka listener container factory for expired events
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CouponExpiredEvent> 
            couponExpiredKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CouponExpiredEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(couponExpiredConsumerFactory());
        return factory;
    }
}