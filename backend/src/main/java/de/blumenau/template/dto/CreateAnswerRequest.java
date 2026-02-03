package de.blumenau.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreateAnswerRequest {
    @Schema(description = "Text of the answer", required = true, example = "Indoor")
    private String text;

    @Schema(description = "The question ID this answer belongs to", required = true, example = "1")
    private Long questionId;

    @Schema(description = "Optional next question ID when this answer is chosen", example = "2")
    private Long nextQuestionId;

    public CreateAnswerRequest() {}

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
