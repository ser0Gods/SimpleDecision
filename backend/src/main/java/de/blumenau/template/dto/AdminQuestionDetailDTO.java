package de.blumenau.template.dto;

import java.util.List;

public class AdminQuestionDetailDTO {
    private Long id;
    private String text;
    private List<AdminAnswerDTO> answers;

    public AdminQuestionDetailDTO() {}

    public AdminQuestionDetailDTO(Long id, String text, List<AdminAnswerDTO> answers) {
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

    public List<AdminAnswerDTO> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AdminAnswerDTO> answers) {
        this.answers = answers;
    }
}
