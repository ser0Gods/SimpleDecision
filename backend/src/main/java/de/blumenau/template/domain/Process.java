package de.blumenau.template.domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Process {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany
    @JoinTable(
            name = "process_start_questions",
            joinColumns = @JoinColumn(name = "process_id"),
            inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private List<Question> startingQuestions = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Question> getStartingQuestions() { return startingQuestions; }
    public void setStartingQuestions(List<Question> startingQuestions) { this.startingQuestions = startingQuestions; }
}
