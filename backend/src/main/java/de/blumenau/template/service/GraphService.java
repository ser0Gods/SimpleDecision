package de.blumenau.template.service;

import de.blumenau.template.domain.Answer;
import de.blumenau.template.domain.Question;
import de.blumenau.template.domain.AnswerRecord;
import de.blumenau.template.dto.AnswerDTO;
import de.blumenau.template.dto.QuestionDTO;
import de.blumenau.template.repository.AnswerRepository;
import de.blumenau.template.repository.AnswerRecordRepository;
import de.blumenau.template.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.sourceforge.plantuml.SourceStringReader;

@Service
public class GraphService {

    public static final String SESSION_KEY_ACTIVE_QS = "graph.activeQuestionIds";
    public static final String SESSION_KEY_HISTORY = "graph.answerHistory";
    public static final String SESSION_KEY_PROCESS = "graph.processId";

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final de.blumenau.template.repository.ProcessRepository processRepository;

    public GraphService(QuestionRepository questionRepository, AnswerRepository answerRepository, AnswerRecordRepository answerRecordRepository, de.blumenau.template.repository.ProcessRepository processRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.processRepository = processRepository;
    }

    @Transactional(readOnly = true)
    public List<QuestionDTO> getCurrentQuestions(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Long> active = (List<Long>) session.getAttribute(SESSION_KEY_ACTIVE_QS);
        Long processId = (Long) session.getAttribute(SESSION_KEY_PROCESS);

        if (processId == null) {
            // Auto-start if only one process exists
            List<de.blumenau.template.domain.Process> processes = processRepository.findAll();
            if (processes.size() == 1) {
                de.blumenau.template.domain.Process p = processes.get(0);
                startProcess(p.getId(), session);
                active = getOrInitActive(session);
            } else {
                return null; // let client choose a process
            }
        }
        if (active == null) return null;
        List<QuestionDTO> list = new ArrayList<>();
        for (Long qid : active) {
            questionRepository.findById(qid).ifPresent(q -> list.add(toDTO(q)));
        }
        return list;
    }

    @Transactional
    public List<QuestionDTO> selectAnswer(long answerId, HttpSession session) {
        Answer answer = answerRepository.findById(answerId).orElse(null);
        if (answer == null) return null;

        // Persist selection to DB and keep session cache ids as well
        String sessionId = session.getId();
        AnswerRecord record = new AnswerRecord();
        record.setSessionId(sessionId);
        record.setAnswer(answer);
        answerRecordRepository.save(record);

        List<Long> history = getOrInitHistory(session);
        history.add(answer.getId());
        session.setAttribute(SESSION_KEY_HISTORY, history);

        // Update active questions list: replace the answered question with its follow-up (if any)
        List<Long> active = getOrInitActive(session);
        Long currentQuestionId = answer.getQuestion() != null ? answer.getQuestion().getId() : null;
        int idx = currentQuestionId != null ? active.indexOf(currentQuestionId) : -1;
        Question next = answer.getNextQuestion();
        if (idx >= 0) {
            if (next == null) {
                active.remove(idx);
            } else {
                active.set(idx, next.getId());
            }
        } else {
            // If not found, and there is a next, append it; otherwise ignore
            if (next != null) active.add(next.getId());
        }
        session.setAttribute(SESSION_KEY_ACTIVE_QS, active);

        // If no more active questions, process ended
        if (active.isEmpty()) {
            // Process finished: generate PUML and PNG export
            Long processId = (Long) session.getAttribute(SESSION_KEY_PROCESS);
            try {
                generateAndExportPuml(processId, session.getId());
            } catch (Exception e) {
                // swallow exception to not break flow; ideally add logging
                e.printStackTrace();
            }
            return new ArrayList<>();
        }
        // Return updated list
        List<QuestionDTO> list = new ArrayList<>();
        for (Long qid : active) {
            questionRepository.findById(qid).ifPresent(q -> list.add(toDTO(q)));
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<AnswerDTO> getHistory(HttpSession session) {
        // Read from DB to ensure persistence beyond session cache
        String sessionId = session.getId();
        List<AnswerRecord> records = answerRecordRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return records.stream()
                .map(AnswerRecord::getAnswer)
                .map(a -> new AnswerDTO(a.getId(), a.getText()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void reset(HttpSession session) {
        String sessionId = session.getId();
        answerRecordRepository.deleteBySessionId(sessionId);
        session.removeAttribute(SESSION_KEY_ACTIVE_QS);
        session.removeAttribute(SESSION_KEY_HISTORY);
        session.removeAttribute(SESSION_KEY_PROCESS);
    }

    @Transactional(readOnly = true)
    public List<de.blumenau.template.dto.ProcessDTO> listProcesses() {
        return processRepository.findAll().stream()
                .map(p -> new de.blumenau.template.dto.ProcessDTO(p.getId(), p.getName()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<QuestionDTO> startProcess(long processId, HttpSession session) {
        Optional<de.blumenau.template.domain.Process> pOpt = processRepository.findById(processId);
        if (!pOpt.isPresent()) return null;
        reset(session);
        de.blumenau.template.domain.Process p = pOpt.get();
        session.setAttribute(SESSION_KEY_PROCESS, p.getId());
        List<Long> active = new ArrayList<>();
        for (Question q : p.getStartingQuestions()) {
            active.add(q.getId());
        }
        session.setAttribute(SESSION_KEY_ACTIVE_QS, active);
        List<QuestionDTO> list = new ArrayList<>();
        for (Question q : p.getStartingQuestions()) {
            list.add(toDTO(q));
        }
        return list;
    }

    private List<Long> getOrInitHistory(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Long> history = (List<Long>) session.getAttribute(SESSION_KEY_HISTORY);
        if (history == null) {
            history = new ArrayList<>();
            session.setAttribute(SESSION_KEY_HISTORY, history);
        }
        return history;
    }

    private List<Long> getOrInitActive(HttpSession session) {
        @SuppressWarnings("unchecked")
        List<Long> active = (List<Long>) session.getAttribute(SESSION_KEY_ACTIVE_QS);
        if (active == null) {
            active = new ArrayList<>();
            session.setAttribute(SESSION_KEY_ACTIVE_QS, active);
        }
        return active;
    }

    private QuestionDTO toDTO(Question q) {
        List<AnswerDTO> answers = q.getAnswers().stream()
                .map(a -> new AnswerDTO(a.getId(), a.getText()))
                .collect(Collectors.toList());
        return new QuestionDTO(q.getId(), q.getText(), answers);
    }

    // Build a PlantUML activity diagram for the whole process and export to data folder
    private void generateAndExportPuml(Long processId, String sessionId) throws IOException {
        if (processId == null) return;
        Optional<de.blumenau.template.domain.Process> pOpt = processRepository.findById(processId);
        if (!pOpt.isPresent()) return;
        de.blumenau.template.domain.Process process = pOpt.get();

        String puml = buildPumlForSession(process, sessionId);

        // Resolve data folder path relative to runtime working dir
        Path cwd = Paths.get("").toAbsolutePath();
        Path dataDir;
        if (cwd.getFileName() != null && "backend".equalsIgnoreCase(cwd.getFileName().toString())) {
            dataDir = cwd.getParent().resolve("data");
        } else {
            dataDir = cwd.resolve("data");
        }
        Files.createDirectories(dataDir);
        Path pumlPath = dataDir.resolve("test.puml");
        Path pngPath = dataDir.resolve("test.png");

        // Write PUML file
        Files.write(pumlPath, puml.getBytes(StandardCharsets.UTF_8));

        // Generate PNG using PlantUML
        SourceStringReader reader = new SourceStringReader(puml);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            reader.outputImage(os);
            Files.write(pngPath, os.toByteArray());
        }
    }

    private String buildPumlForSession(de.blumenau.template.domain.Process process, String sessionId) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("(Start) as start\n");
        sb.append("(End) as ende\n");

        // Load selected answers for this session
        List<AnswerRecord> records = answerRecordRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // Collect involved questions in order of appearance
        List<Question> orderedQuestions = new ArrayList<>();
        Set<Long> questionIds = new HashSet<>();

        for (AnswerRecord rec : records) {
            Answer a = rec.getAnswer();
            if (a == null) continue;
            Question q = a.getQuestion();
            if (q != null && questionIds.add(q.getId())) {
                orderedQuestions.add(q);
            }
            Question next = a.getNextQuestion();
            if (next != null && questionIds.add(next.getId())) {
                orderedQuestions.add(next);
            }
        }

        // Define nodes only for involved questions
        for (Question q : orderedQuestions) {
            sb.append("(")
              .append(escape(q.getText()))
              .append(") as ")
              .append(q.getId())
              .append("\n");
        }

        // Start edges only for starting questions that appear in records
        Set<Long> startLinked = new HashSet<>();
        Set<Long> startIds = process.getStartingQuestions().stream().map(Question::getId).collect(java.util.stream.Collectors.toSet());
        for (AnswerRecord rec : records) {
            Answer a = rec.getAnswer();
            if (a == null) continue;
            Question q = a.getQuestion();
            if (q != null && startIds.contains(q.getId()) && startLinked.add(q.getId())) {
                sb.append("start --> ").append(q.getId()).append("\n");
            }
        }

        // Selected edges only
        for (AnswerRecord rec : records) {
            Answer a = rec.getAnswer();
            if (a == null) continue;
            Question q = a.getQuestion();
            if (q == null) continue;
            String label = escape(a.getText());
            Question next = a.getNextQuestion();
            if (next == null) {
                sb.append(q.getId()).append(" --> ende : ").append(label).append("\n");
            } else {
                sb.append(q.getId()).append(" --> ").append(next.getId()).append(" : ").append(label).append("\n");
            }
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        // Replace newlines and simple escaping for quotes and parentheses
        return s.replace("\\", "\\\\")
                .replace("\r", " ")
                .replace("\n", " ")
                .replace(":", "\\:");
    }
}
