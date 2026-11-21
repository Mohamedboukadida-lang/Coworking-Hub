package com.coworkinghub.coworkinghub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class News {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private byte[] images;
    private String title;
    private Date date;
    private String description;

    @ManyToOne
    @JoinColumn(name = "submitted_by_id")
    @JsonIgnore
    private Compte submittedBy;
}
