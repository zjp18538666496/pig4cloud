package com.pig4cloud.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityEntity {

    private Integer id;
    private String name;
    private String description;
}
