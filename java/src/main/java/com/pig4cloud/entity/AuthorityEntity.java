package com.pig4cloud.entity;

import lombok.Data;

@Data
public class AuthorityEntity {
    private Integer id;
    private String name;
    private String description;
    public AuthorityEntity(Integer id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
