package de.blumenau.template.repository;

import de.blumenau.template.domain.Process;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessRepository extends JpaRepository<Process, Long> {
}
