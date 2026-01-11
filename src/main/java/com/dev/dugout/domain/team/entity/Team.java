package com.dev.dugout.domain.team.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//팀 정보(id,팀 이름, 지역, 경기장)
public class Team {

    @Id
    private long id;

    private String name;
    private String city;
    private String stadiumName;


}
