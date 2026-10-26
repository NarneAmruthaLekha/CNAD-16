package com.impactflow.auth.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false,unique=true)
    private String username;

    @Column(nullable=false)
    private String password;

    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="user_roles",joinColumns=@JoinColumn(name="user_id"))
    @Column(name="role")
    private List<String> roles=new ArrayList<>();

    public User(){}

    public Long getId(){
        return id;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username=username;
    }

    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password=password;
    }

    public List<String> getRoles(){
        return roles;
    }

    public void setRoles(List<String> roles){
        this.roles=roles;
    }
}