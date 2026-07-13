package com.edunexus.repository;

import com.edunexus.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
    Optional<Transaction> findByTransactionCode(String transactionCode);
    List<Transaction> findByStudentId(Integer studentId);
    List<Transaction> findByCourseId(Integer courseId);
    List<Transaction> findByStatus(String status);
}
