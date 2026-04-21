package com.yutu.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        List<String> resourceLocations = resolveResourceLocations();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocations.toArray(new String[0]));
    }

    private List<String> resolveResourceLocations() {
        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(Paths.get(uploadDir).toAbsolutePath().normalize());
        candidates.add(Paths.get("yutu-admin", uploadDir).toAbsolutePath().normalize());

        List<String> resourceLocations = new ArrayList<>();
        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) {
                continue;
            }
            String resourceLocation = candidate.toUri().toString();
            if (!resourceLocation.endsWith("/")) {
                resourceLocation = resourceLocation + "/";
            }
            resourceLocations.add(resourceLocation);
        }

        if (resourceLocations.isEmpty()) {
            String fallback = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
            if (!fallback.endsWith("/")) {
                fallback = fallback + "/";
            }
            resourceLocations.add(fallback);
        }
        return resourceLocations;
    }
}
