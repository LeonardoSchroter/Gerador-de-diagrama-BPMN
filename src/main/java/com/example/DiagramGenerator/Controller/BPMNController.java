package com.example.DiagramGenerator.Controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;

@RestController
public class BPMNController {

    @GetMapping("/diagram")
    public ResponseEntity<String> getDiagram() {
        try {
            var resource = new ClassPathResource("processes/sample-process.bpmn");
            var xml = Files.readString(resource.getFile().toPath());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, "application/xml");

            return new ResponseEntity<>(xml, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to load BPMN diagram.");
        }
    }
}
