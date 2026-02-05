package de.blumenau.template.controller;

import de.blumenau.template.dto.AnswerDTO;
import de.blumenau.template.dto.QuestionDTO;
import de.blumenau.template.dto.ProcessDTO;
import de.blumenau.template.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/graph")
@Tag(name = "Decision Graph", description = "Endpoints to navigate the decision graph and manage session history")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current questions", description = "Returns the current active questions with possible answers for the caller's session. Returns null if no process selected and more than one exists.")
    public ResponseEntity<java.util.List<QuestionDTO>> current(HttpSession session) {
        java.util.List<QuestionDTO> dto = graphService.getCurrentQuestions(session);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/answer/{answerId}")
    @Operation(summary = "Select an answer", description = "Selects an answer and returns the updated list of active questions; empty list when finished")
    public ResponseEntity<java.util.List<QuestionDTO>> choose(@PathVariable long answerId, HttpSession session) {
        java.util.List<QuestionDTO> dto = graphService.selectAnswer(answerId, session);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/processes")
    @Operation(summary = "List processes", description = "Returns the list of available processes to start")
    public ResponseEntity<List<ProcessDTO>> processes() {
        return ResponseEntity.ok(graphService.listProcesses());
    }

    @PostMapping("/process/{processId}/start")
    @Operation(summary = "Start process", description = "Resets the session and starts the selected process; returns list of starting questions")
    public ResponseEntity<java.util.List<QuestionDTO>> start(@PathVariable long processId, HttpSession session) {
        return ResponseEntity.ok(graphService.startProcess(processId, session));
    }

    @GetMapping("/history")
    @Operation(summary = "Get answer history", description = "Returns the list of answers selected in order for the current session")
    public ResponseEntity<List<AnswerDTO>> history(HttpSession session) {
        return ResponseEntity.ok(graphService.getHistory(session));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset session", description = "Clears the current session's progress and history")
    public ResponseEntity<Void> reset(HttpSession session) {
        graphService.reset(session);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/result.png")
    @Operation(summary = "Get generated result image", description = "Returns the generated PNG of the finished process for the current session, if available")
    public ResponseEntity<byte[]> getResultPng(HttpSession session) {
        Long processId = (Long) session.getAttribute(GraphService.SESSION_KEY_PROCESS);
        if (processId == null) {
            return ResponseEntity.notFound().build();
        }

        // Resolve data folder path similar to GraphService
        Path cwd = Paths.get("").toAbsolutePath();
        Path dataDir;
        if (cwd.getFileName() != null && "backend".equalsIgnoreCase(cwd.getFileName().toString())) {
            dataDir = cwd.getParent().resolve("data");
        } else {
            dataDir = cwd.resolve("data");
        }

        Path pngPath = dataDir.resolve("Process_" + processId + "_" + session.getId() + ".png");
        try {
            if (!Files.exists(pngPath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] bytes = Files.readAllBytes(pngPath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pngPath.getFileName().toString() + "\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .contentLength(bytes.length)
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
