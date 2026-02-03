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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GraphService {

    public static final String SESSION_KEY_CURRENT_Q = "graph.currentQuestionId";
    public static final String SESSION_KEY_HISTORY = "graph.answerHistory";

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AnswerRecordRepository answerRecordRepository;

    public GraphService(QuestionRepository questionRepository, AnswerRepository answerRepository, AnswerRecordRepository answerRecordRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.answerRecordRepository = answerRecordRepository;
    }

    @Transactional(readOnly = true)
    public QuestionDTO getCurrentQuestion(HttpSession session) {
        Long currentId = (Long) session.getAttribute(SESSION_KEY_CURRENT_Q);
        if (currentId == null) {
            // determine root questions (processes)
            List<Question> roots = questionRepository.findByRootTrue();
            if (roots.isEmpty()) {
                return null; // no processes available
            }
            if (roots.size() == 1) {
                // auto-select the only available process
                Question root = roots.get(0);
                session.setAttribute(SESSION_KEY_CURRENT_Q, root.getId());
                return toDTO(root);
            }
            // multiple processes available; let the client choose a process first
            return null;
        }
        return questionRepository.findById(currentId).map(this::toDTO).orElse(null);
    }

    @Transactional
    public QuestionDTO selectAnswer(long answerId, HttpSession session) {
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

        Question next = answer.getNextQuestion();
        if (next == null) {
            session.setAttribute(SESSION_KEY_CURRENT_Q, null);
            return null;
        }
        session.setAttribute(SESSION_KEY_CURRENT_Q, next.getId());
        return toDTO(next);
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
        session.removeAttribute(SESSION_KEY_CURRENT_Q);
        session.removeAttribute(SESSION_KEY_HISTORY);
    }

    @Transactional(readOnly = true)
    public List<QuestionDTO> listProcesses() {
        return questionRepository.findByRootTrue().stream()
                .map(q -> new QuestionDTO(q.getId(), q.getText(), new ArrayList<>()))
                .collect(Collectors.toList());
    }

    @Transactional
    public QuestionDTO startProcess(long rootId, HttpSession session) {
        Optional<Question> qOpt = questionRepository.findById(rootId);
        if (!qOpt.isPresent() || !qOpt.get().isRoot()) {
            return null;
        }
        // reset previous progress and set new root
        reset(session);
        Question root = qOpt.get();
        session.setAttribute(SESSION_KEY_CURRENT_Q, root.getId());
        return toDTO(root);
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

    private QuestionDTO toDTO(Question q) {
        List<AnswerDTO> answers = q.getAnswers().stream()
                .map(a -> new AnswerDTO(a.getId(), a.getText()))
                .collect(Collectors.toList());
        return new QuestionDTO(q.getId(), q.getText(), answers);
    }
}
