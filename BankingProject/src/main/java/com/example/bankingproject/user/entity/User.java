package com.example.bankingproject.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity //marks class as a db table
@Table(name = "users") //setting table's name to users
@Getter
@Setter
@Builder //clean object creation
@NoArgsConstructor //req by JPA
@AllArgsConstructor
public class User {

    @Id //marks the id field as primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) //auto-generated the key , auto-increment
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
