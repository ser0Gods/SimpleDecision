package de.blumenau.template.dto;

public class AdminAnswerDTO {
    private Long id;
    private String text;
    private Long questionId;
    private Long nextQuestionId; // may be null

    public AdminAnswerDTO() {}

    public AdminAnswerDTO(Long id, String text, Long questionId, Long nextQuestionId) {
        this.id = id;
        this.text = text;
        this.questionId = questionId;
        this.nextQuestionId = nextQuestionId;
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

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getNextQuestionId() {
        return nextQuestionId;
    }

    public void setNextQuestionId(Long nextQuestionId) {
        this.nextQuestionId = nextQuestionId;
    }
}
