package com.example.bankingproject.account.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity //Marks this class as a database table
@Getter
@Setter
@Table(name = "accounts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Setter(AccessLevel.NONE)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false,unique=true)
    private Long userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance; //BigDecimal makes exact decimal representation rather than double n float that might round off

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist // runs just before the entity is saved to the db , thus we use the createdAt here to save the exact time when the entity was added to the db
    public void prePersist(){
        this.createdAt = LocalDateTime.now();

        if(this.balance == null){
            this.balance = BigDecimal.ZERO;
        }
    }

}
