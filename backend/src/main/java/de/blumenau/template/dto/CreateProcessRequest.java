package de.blumenau.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public class CreateProcessRequest {
    @Schema(description = "Process name", required = true, example = "Pet Advisor")
    private String name;

    @Schema(description = "IDs of starting questions for this process", example = "[1,2]")
    private List<Long> startingQuestionIds;

    public CreateProcessRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Long> getStartingQuestionIds() {
        return startingQuestionIds;
    }

    public void setStartingQuestionIds(List<Long> startingQuestionIds) {
        this.startingQuestionIds = startingQuestionIds;
    }
}
