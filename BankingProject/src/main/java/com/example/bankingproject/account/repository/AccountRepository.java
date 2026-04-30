package com.example.bankingproject.account.repository;

import com.example.bankingproject.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {

    Optional<Account> findByUserId (long userId);
    //Custom query method
    //Spring automatically generates SQL:
    //SELECT * FROM accounts WHERE user_id = ?
    //Optional is used because Account may or may not exist

}
