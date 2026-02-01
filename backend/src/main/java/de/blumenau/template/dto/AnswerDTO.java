package de.blumenau.template.dto;

public class AnswerDTO {
    private Long id;
    private String text;

    public AnswerDTO() {}

    public AnswerDTO(Long id, String text) {
        this.id = id;
        this.text = text;
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
}
