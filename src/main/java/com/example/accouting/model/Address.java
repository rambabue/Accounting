package com.example.accouting.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "address")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String orgID;
    private String aicGroupID;
    private String accountID;
    private String tempID;
    
    // Constructor without id for easier creation
    public Address(String orgID, String aicGroupID, String accountID, String tempID) {
        this.orgID = orgID;
        this.aicGroupID = aicGroupID;
        this.accountID = accountID;
        this.tempID = tempID;
    }
    
    // Constructor without tempID for initial data loading
    public Address(String orgID, String aicGroupID, String accountID) {
        this.orgID = orgID;
        this.aicGroupID = aicGroupID;
        this.accountID = accountID;
        this.tempID = null;
    }
    
    @Override
    public String toString() {
        return "OrgID: " + orgID + ", AICGroupID: " + aicGroupID + 
               ", AccountID: " + accountID + ", TempID: " + tempID;
    }
}