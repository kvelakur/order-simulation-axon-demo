package com.cloudkitchens.enginesim;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class SimulationUtils {

    public String readClasspathFileAsString(final String fileName) {
        final ClassPathResource classPathResource = new ClassPathResource(fileName);
        try {
            final byte[] binaryData = FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            return new String(binaryData, StandardCharsets.UTF_8);
        } catch (final Exception e) {
            log.error("Could not read json file " + fileName, e);
            return null;
        }
    }

    public ObjectMapper buildObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    public <T> T readJson(final String fileName, final TypeReference<T> typeReference) {
        final ObjectMapper reader = buildObjectMapper();
        try {
            final String json = readClasspathFileAsString(fileName);
            return reader.readValue(json, typeReference);
        } catch (final IOException e) {
            log.error("Could not read json file " + fileName, e);
            return null;
        }
    }
}
