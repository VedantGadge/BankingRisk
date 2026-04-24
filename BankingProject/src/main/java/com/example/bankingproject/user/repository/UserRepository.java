package com.example.bankingproject.user.repository;

import com.example.bankingproject.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// no need of using @Repository as we are alr using the JpaRepository so spring automatically considers this as a Respository bean
public interface UserRepository extends JpaRepository<User,Long>{

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

// here we are just checking if the user exists in the db or not and fetching details if exists
// no need to create a fn for adding/saving a user in db as Jpa alr provides us with in-built save() fn