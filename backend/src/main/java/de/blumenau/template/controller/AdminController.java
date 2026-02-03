package de.blumenau.template.controller;

import de.blumenau.template.domain.Answer;
import de.blumenau.template.domain.Question;
import de.blumenau.template.dto.AnswerDTO;
import de.blumenau.template.dto.CreateAnswerRequest;
import de.blumenau.template.dto.CreateQuestionRequest;
import de.blumenau.template.dto.QuestionDTO;
import de.blumenau.template.repository.AnswerRepository;
import de.blumenau.template.repository.QuestionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Endpoints to manage questions and answers")
public class AdminController {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public AdminController(QuestionRepository questionRepository, AnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    @GetMapping("/questions")
    @Operation(summary = "List questions", description = "Returns all questions (id and text) for administration purposes")
    public ResponseEntity<java.util.List<QuestionDTO>> listQuestions() {
        java.util.List<QuestionDTO> list = questionRepository.findAll().stream()
                .map(q -> new QuestionDTO(q.getId(), q.getText(), java.util.Collections.emptyList()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/questions")
    @Operation(summary = "Create a question", description = "Creates a new question. Optionally mark it as root.")
    public ResponseEntity<QuestionDTO> createQuestion(@RequestBody CreateQuestionRequest req) {
        if (req == null || req.getText() == null || req.getText().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Question q = new Question();
        q.setText(req.getText().trim());
        q.setRoot(req.isRoot());
        Question saved = questionRepository.save(q);
        QuestionDTO dto = new QuestionDTO(saved.getId(), saved.getText(), java.util.Collections.emptyList());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/answers")
    @Operation(summary = "Create an answer", description = "Creates a new answer for an existing question. Optionally link to a next question.")
    public ResponseEntity<AnswerDTO> createAnswer(@RequestBody CreateAnswerRequest req) {
        if (req == null || req.getText() == null || req.getText().trim().isEmpty() || req.getQuestionId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Question parent = questionRepository.findById(req.getQuestionId()).orElse(null);
        if (parent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Question next = null;
        if (req.getNextQuestionId() != null) {
            next = questionRepository.findById(req.getNextQuestionId()).orElse(null);
            if (next == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
        Answer a = new Answer();
        a.setText(req.getText().trim());
        a.setQuestion(parent);
        a.setNextQuestion(next);
        Answer saved = answerRepository.save(a);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AnswerDTO(saved.getId(), saved.getText()));
    }
}
