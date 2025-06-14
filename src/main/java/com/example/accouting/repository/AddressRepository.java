package com.example.accouting.repository;

import com.example.accouting.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {


    // Find all addresses
    List<Address> findAll();

    // Find all addresses by account ID
    List<Address> findByAccountID(String accountID);

    // Find all distinct account IDs
    @Query("SELECT DISTINCT a.accountID FROM Address a")
    Set<String> findAllDistinctAccountIDs();

    // Find all distinct AIC group IDs
    @Query("SELECT DISTINCT a.aicGroupID FROM Address a")
    Set<String> findAllDistinctAicGroupIDs();

    // Find all account IDs for a specific AIC group ID
    @Query("SELECT DISTINCT a.accountID FROM Address a WHERE a.aicGroupID = :aicGroupID")
    Set<String> findAccountIDsByAicGroupID(@Param("aicGroupID") String aicGroupID);

    // Update tempID for all addresses with specific account IDs
    @Modifying
    @Transactional
    @Query("UPDATE Address a SET a.tempID = :tempID WHERE a.accountID IN :accountIDs")
    void updateTempIDForAccountIDs(@Param("tempID") String tempID, @Param("accountIDs") Set<String> accountIDs);
}
