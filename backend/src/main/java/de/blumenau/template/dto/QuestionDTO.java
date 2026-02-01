package de.blumenau.template.dto;

import java.util.List;

public class QuestionDTO {
    private Long id;
    private String text;
    private List<AnswerDTO> answers;

    public QuestionDTO() {}

    public QuestionDTO(Long id, String text, List<AnswerDTO> answers) {
        this.id = id;
        this.text = text;
        this.answers = answers;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<AnswerDTO> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerDTO> answers) {
        this.answers = answers;
    }
}
