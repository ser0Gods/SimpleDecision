package de.blumenau.template.config;

import de.blumenau.template.domain.Answer;
import de.blumenau.template.domain.Question;
import de.blumenau.template.domain.Process;
import de.blumenau.template.repository.AnswerRepository;
import de.blumenau.template.repository.QuestionRepository;
import de.blumenau.template.repository.ProcessRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(QuestionRepository questionRepository, AnswerRepository answerRepository, ProcessRepository processRepository) {
        return args -> {
            if (questionRepository.count() > 0) return;

            // PET DECISION TREE (3-5 steps) - terminal answers are recommendations
            // Q1 (root)
            Question q1 = new Question();
            q1.setText("How much time can you dedicate to daily pet care?");
            q1.setRoot(true);
            questionRepository.save(q1);

            // Q2
            Question q2 = new Question();
            q2.setText("How much living space do you have?");
            questionRepository.save(q2);

            // Q3
            Question q3 = new Question();
            q3.setText("Do you prefer a pet that's hypoallergenic/low shedding?");
            questionRepository.save(q3);

            // Q4
            Question q4 = new Question();
            q4.setText("How active would you like your pet to be?");
            questionRepository.save(q4);

            // Q5
            Question q5 = new Question();
            q5.setText("Are you comfortable with regular training and socialization?");
            questionRepository.save(q5);

            // Answers for Q1 -> Q2 or terminal small/low-maintenance
            Answer a11 = new Answer();
            a11.setText("A lot (1+ hour per day)");
            a11.setQuestion(q1);
            a11.setNextQuestion(q2);
            answerRepository.save(a11);

            Answer a12 = new Answer();
            a12.setText("Some (15–60 minutes)");
            a12.setQuestion(q1);
            a12.setNextQuestion(q2);
            answerRepository.save(a12);

            Answer a13 = new Answer();
            a13.setText("Very little (< 15 minutes)");
            a13.setQuestion(q1);
            a13.setNextQuestion(q3); // steer to low-maintenance choices
            answerRepository.save(a13);

            // Answers for Q2 -> Q3
            Answer a21 = new Answer();
            a21.setText("Large home/yard");
            a21.setQuestion(q2);
            a21.setNextQuestion(q4);
            answerRepository.save(a21);

            Answer a22 = new Answer();
            a22.setText("Medium apartment/house");
            a22.setQuestion(q2);
            a22.setNextQuestion(q3);
            answerRepository.save(a22);

            Answer a23 = new Answer();
            a23.setText("Small studio/limited space");
            a23.setQuestion(q2);
            a23.setNextQuestion(q3);
            answerRepository.save(a23);

            // Answers for Q3 -> Q4 or terminal small pets
            Answer a31 = new Answer();
            a31.setText("Yes, hypoallergenic preferred");
            a31.setQuestion(q3);
            a31.setNextQuestion(q4);
            answerRepository.save(a31);

            Answer a32 = new Answer();
            a32.setText("No preference");
            a32.setQuestion(q3);
            a32.setNextQuestion(q4);
            answerRepository.save(a32);

            // Answers for Q4 -> Q5 or terminal
            Answer a41 = new Answer();
            a41.setText("Very active");
            a41.setQuestion(q4);
            a41.setNextQuestion(q5);
            answerRepository.save(a41);

            Answer a42 = new Answer();
            a42.setText("Moderately active");
            a42.setQuestion(q4);
            a42.setNextQuestion(q5);
            answerRepository.save(a42);

            Answer a43 = new Answer();
            a43.setText("Low activity");
            a43.setQuestion(q4);
            a43.setNextQuestion(q5);
            answerRepository.save(a43);

            // Answers for Q5 -> terminal recommendations
            Answer a51 = new Answer();
            a51.setText("Yes — consider a Dog (active breed)");
            a51.setQuestion(q5);
            a51.setNextQuestion(null);
            answerRepository.save(a51);

            Answer a52 = new Answer();
            a52.setText("Somewhat — consider a Cat");
            a52.setQuestion(q5);
            a52.setNextQuestion(null);
            answerRepository.save(a52);

            Answer a53 = new Answer();
            a53.setText("Prefer minimal — consider a Fish or Hamster");
            a53.setQuestion(q5);
            a53.setNextQuestion(null);
            answerRepository.save(a53);

            // Create a default process with starting questions
            Process p = new Process();
            p.setName("Pet Recommendation");
            p.getStartingQuestions().add(q1); // start with the root question
            processRepository.save(p);
        };
    }
}
