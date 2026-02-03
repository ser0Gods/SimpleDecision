package de.blumenau.template.controller;

import de.blumenau.template.dto.AnswerDTO;
import de.blumenau.template.dto.QuestionDTO;
import de.blumenau.template.service.GraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
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
    @Operation(summary = "Get current question", description = "Returns the current question with possible answers for the caller's session")
    public ResponseEntity<QuestionDTO> current(HttpSession session) {
        QuestionDTO dto = graphService.getCurrentQuestion(session);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/answer/{answerId}")
    @Operation(summary = "Select an answer", description = "Selects an answer and returns the next question if available; null when finished")
    public ResponseEntity<QuestionDTO> choose(@PathVariable long answerId, HttpSession session) {
        QuestionDTO dto = graphService.selectAnswer(answerId, session);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/processes")
    @Operation(summary = "List processes", description = "Returns the list of available processes (root questions)")
    public ResponseEntity<List<QuestionDTO>> processes() {
        return ResponseEntity.ok(graphService.listProcesses());
    }

    @PostMapping("/process/{rootId}/start")
    @Operation(summary = "Start process", description = "Resets the session and starts the selected process (root question)")
    public ResponseEntity<QuestionDTO> start(@PathVariable long rootId, HttpSession session) {
        return ResponseEntity.ok(graphService.startProcess(rootId, session));
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
}
