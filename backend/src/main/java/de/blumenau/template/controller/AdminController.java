package de.blumenau.template.controller;

import de.blumenau.template.domain.Answer;
import de.blumenau.template.domain.AnswerRecord;
import de.blumenau.template.domain.Process;
import de.blumenau.template.domain.Question;
import de.blumenau.template.dto.*;
import de.blumenau.template.repository.AnswerRecordRepository;
import de.blumenau.template.repository.AnswerRepository;
import de.blumenau.template.repository.ProcessRepository;
import de.blumenau.template.repository.QuestionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Endpoints to manage processes, questions, answers, and answer records")
public class AdminController {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ProcessRepository processRepository;
    private final AnswerRecordRepository answerRecordRepository;

    public AdminController(QuestionRepository questionRepository,
                           AnswerRepository answerRepository,
                           ProcessRepository processRepository,
                           AnswerRecordRepository answerRecordRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.processRepository = processRepository;
        this.answerRecordRepository = answerRecordRepository;
    }

    // Questions CRUD
    @GetMapping("/questions")
    @Operation(summary = "List questions", description = "Returns all questions (id and text) for administration purposes")
    public ResponseEntity<List<QuestionDTO>> listQuestions() {
        List<QuestionDTO> list = questionRepository.findAll().stream()
                .map(q -> new QuestionDTO(q.getId(), q.getText(), java.util.Collections.emptyList()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/questions/{id}")
    @Operation(summary = "Get a question", description = "Returns a question by id")
    public ResponseEntity<QuestionDTO> getQuestion(@PathVariable Long id) {
        return questionRepository.findById(id)
                .map(q -> new QuestionDTO(q.getId(), q.getText(), java.util.Collections.emptyList()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
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

    @PutMapping("/questions/{id}")
    @Operation(summary = "Update a question", description = "Updates text and root flag of a question")
    public ResponseEntity<QuestionDTO> updateQuestion(@PathVariable Long id, @RequestBody CreateQuestionRequest req) {
        return questionRepository.findById(id)
                .map(q -> {
                    if (req == null || req.getText() == null || req.getText().trim().isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).<QuestionDTO>build();
                    }
                    q.setText(req.getText().trim());
                    q.setRoot(req.isRoot());
                    Question saved = questionRepository.save(q);
                    return ResponseEntity.ok(new QuestionDTO(saved.getId(), saved.getText(), java.util.Collections.emptyList()));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/questions/{id}")
    @Operation(summary = "Delete a question", description = "Deletes a question by id. Cascades to its answers.")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        if (!questionRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        questionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Answers CRUD
    @GetMapping("/answers")
    @Operation(summary = "List answers", description = "Returns all answers (id and text)")
    public ResponseEntity<List<AnswerDTO>> listAnswers() {
        List<AnswerDTO> list = answerRepository.findAll().stream()
                .map(a -> new AnswerDTO(a.getId(), a.getText()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/answers/{id}")
    @Operation(summary = "Get an answer", description = "Returns an answer by id")
    public ResponseEntity<AnswerDTO> getAnswer(@PathVariable Long id) {
        return answerRepository.findById(id)
                .map(a -> new AnswerDTO(a.getId(), a.getText()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
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

    @PutMapping("/answers/{id}")
    @Operation(summary = "Update an answer", description = "Updates text, owning question, and optional next question")
    public ResponseEntity<AnswerDTO> updateAnswer(@PathVariable Long id, @RequestBody CreateAnswerRequest req) {
        return answerRepository.findById(id)
                .map(a -> {
                    if (req == null || req.getText() == null || req.getText().trim().isEmpty() || req.getQuestionId() == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).<AnswerDTO>build();
                    }
                    Question parent = questionRepository.findById(req.getQuestionId()).orElse(null);
                    if (parent == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).<AnswerDTO>build();
                    }
                    Question next = null;
                    if (req.getNextQuestionId() != null) {
                        next = questionRepository.findById(req.getNextQuestionId()).orElse(null);
                        if (next == null) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).<AnswerDTO>build();
                        }
                    }
                    a.setText(req.getText().trim());
                    a.setQuestion(parent);
                    a.setNextQuestion(next);
                    Answer saved = answerRepository.save(a);
                    return ResponseEntity.ok(new AnswerDTO(saved.getId(), saved.getText()));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/answers/{id}")
    @Operation(summary = "Delete an answer", description = "Deletes an answer by id")
    public ResponseEntity<Void> deleteAnswer(@PathVariable Long id) {
        if (!answerRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        answerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Processes CRUD
    @GetMapping("/processes")
    @Operation(summary = "List processes", description = "Returns all processes (id and name)")
    public ResponseEntity<List<ProcessDTO>> listProcesses() {
        List<ProcessDTO> list = processRepository.findAll().stream()
                .map(p -> new ProcessDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/processes/{id}")
    @Operation(summary = "Get a process", description = "Returns a process by id")
    public ResponseEntity<ProcessDTO> getProcess(@PathVariable Long id) {
        return processRepository.findById(id)
                .map(p -> new ProcessDTO(p.getId(), p.getName()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/processes")
    @Operation(summary = "Create a process", description = "Creates a new process with optional starting questions")
    public ResponseEntity<ProcessDTO> createProcess(@RequestBody CreateProcessRequest req) {
        if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Process p = new Process();
        p.setName(req.getName().trim());
        if (req.getStartingQuestionIds() != null && !req.getStartingQuestionIds().isEmpty()) {
            List<Question> starts = questionRepository.findAllById(req.getStartingQuestionIds());
            p.setStartingQuestions(starts);
        }
        Process saved = processRepository.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ProcessDTO(saved.getId(), saved.getName()));
    }

    @PutMapping("/processes/{id}")
    @Operation(summary = "Update a process", description = "Updates process name and starting questions")
    public ResponseEntity<ProcessDTO> updateProcess(@PathVariable Long id, @RequestBody CreateProcessRequest req) {
        return processRepository.findById(id)
                .map(p -> {
                    if (req == null || req.getName() == null || req.getName().trim().isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).<ProcessDTO>build();
                    }
                    p.setName(req.getName().trim());
                    if (req.getStartingQuestionIds() != null) {
                        List<Question> starts = questionRepository.findAllById(req.getStartingQuestionIds());
                        p.setStartingQuestions(starts);
                    }
                    Process saved = processRepository.save(p);
                    return ResponseEntity.ok(new ProcessDTO(saved.getId(), saved.getName()));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/processes/{id}")
    @Operation(summary = "Delete a process", description = "Deletes a process by id")
    public ResponseEntity<Void> deleteProcess(@PathVariable Long id) {
        if (!processRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        processRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // AnswerRecord read/delete
    @GetMapping("/answer-records")
    @Operation(summary = "List answer records", description = "Returns all answer records")
    public ResponseEntity<List<AnswerRecordDTO>> listAnswerRecords() {
        List<AnswerRecordDTO> list = answerRecordRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/answer-records/{id}")
    @Operation(summary = "Get an answer record", description = "Returns an answer record by id")
    public ResponseEntity<AnswerRecordDTO> getAnswerRecord(@PathVariable Long id) {
        return answerRecordRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @DeleteMapping("/answer-records/{id}")
    @Operation(summary = "Delete an answer record", description = "Deletes an answer record by id")
    public ResponseEntity<Void> deleteAnswerRecord(@PathVariable Long id) {
        if (!answerRecordRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        answerRecordRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private AnswerRecordDTO toDto(AnswerRecord ar) {
        Long answerId = ar.getAnswer() != null ? ar.getAnswer().getId() : null;
        Instant createdAt = ar.getCreatedAt();
        AnswerRecordDTO dto = new AnswerRecordDTO();
        dto.setId(ar.getId());
        dto.setSessionId(ar.getSessionId());
        dto.setAnswerId(answerId);
        dto.setCreatedAt(createdAt);
        return dto;
    }
}
