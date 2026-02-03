package de.blumenau.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class CreateQuestionRequest {
    @Schema(description = "Question text", required = true, example = "Do you prefer indoor or outdoor pets?")
    private String text;

    @Schema(description = "Whether this question is the root of the graph", example = "false")
    private boolean root;

    public CreateQuestionRequest() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }
}
