package com.example.DiagramGenerator.Controller;

import com.example.DiagramGenerator.Service.BpmnService;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/api/bpmn")
public class BPMNController {

    @Autowired
    private BpmnService bpmnService;

    @GetMapping("/diagram")
    public String renderDiagramPage() {
        return "diagram";
    }

    @PostMapping(value = "/generate-web", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public ResponseEntity<String> generateBpmnDiagramFromWeb(
            @RequestParam String processId,
            @RequestBody List<String> taskNames) {

        BpmnModelInstance modelInstance = bpmnService.generateBpmn(taskNames);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Bpmn.writeModelToStream(outputStream, modelInstance);

        String bpmnXml = outputStream.toString(StandardCharsets.UTF_8);

        return ResponseEntity.ok(bpmnXml);
    }
}
