package com.careerfit.service;

import com.careerfit.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Private file storage on local disk. File names are random UUIDs generated here, so user input never
 * reaches the file system path. On hosts with ephemeral disks, replace this with object storage.
 */
@Component
@Slf4j
public class ResumeStorage {

    private final Path root;

    public ResumeStorage(@Value("${app.storage.resume-dir}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    public String store(byte[] bytes) {
        try {
            Files.createDirectories(root);
            String name = UUID.randomUUID() + ".pdf";
            Files.write(resolve(name), bytes);
            return name;
        } catch (IOException e) {
            log.error("Could not store resume file", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the uploaded file");
        }
    }

    public byte[] read(String name) {
        try {
            return Files.readAllBytes(resolve(name));
        } catch (IOException e) {
            log.error("Could not read resume file {}", name);
            throw new ApiException(HttpStatus.NOT_FOUND, "The stored file is no longer available. Please upload it again.");
        }
    }

    public void delete(String name) {
        try {
            Files.deleteIfExists(resolve(name));
        } catch (IOException e) {
            log.warn("Could not delete resume file {}", name);
        }
    }

    private Path resolve(String name) {
        Path p = root.resolve(name).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("Invalid storage path");
        return p;
    }
}
