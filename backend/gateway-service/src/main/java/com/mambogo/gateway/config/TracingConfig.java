package com.mambogo.gateway.config;

import brave.sampler.Sampler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingReceiverTracingObservationHandler;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    @Bean
    public Sampler defaultSampler() {
        return Sampler.ALWAYS_SAMPLE;
    }

    @Bean
    public DefaultTracingObservationHandler defaultTracingObservationHandler(Tracer tracer) {
        return new DefaultTracingObservationHandler(tracer);
    }

    @Bean
    @SuppressWarnings("rawtypes")
    public PropagatingSenderTracingObservationHandler propagatingSenderTracingObservationHandler(
            Tracer tracer, Propagator propagator) {
        return new PropagatingSenderTracingObservationHandler(tracer, propagator);
    }

    @Bean
    @SuppressWarnings("rawtypes")
    public PropagatingReceiverTracingObservationHandler propagatingReceiverTracingObservationHandler(
            Tracer tracer, Propagator propagator) {
        return new PropagatingReceiverTracingObservationHandler(tracer, propagator);
    }
}
