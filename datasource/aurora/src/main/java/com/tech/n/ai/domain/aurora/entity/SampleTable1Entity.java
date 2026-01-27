package com.tech.n.ai.datasource.mariadb.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

//@Entity
@Table(name = "sample_table1")
@Getter
@Setter
public class SampleTable1Entity {

    @Id
    private Long id;

    private String name;

    private String description;
    
}
