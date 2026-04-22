package com.derwin.prepforge.behavioral.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "behavioral_questions")
public class BehavioralQuestion {

    @Id
    private UUID id;

    @Column(nullable = false, length = 1_000)
    private String prompt;

    @Column(nullable = false)
    private String category;
}
