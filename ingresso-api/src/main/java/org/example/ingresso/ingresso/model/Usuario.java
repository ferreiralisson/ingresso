package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UsuarioPerfil usuarioPerfil;

    public Usuario() {
    }

    public Usuario(String nome, String email, String senha, UsuarioPerfil usuarioPerfil) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.usuarioPerfil = usuarioPerfil;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public UsuarioPerfil getUsuarioPerfil() {
        return usuarioPerfil;
    }

    public void setUsuarioPerfil(UsuarioPerfil usuarioPerfil) {
        this.usuarioPerfil = usuarioPerfil;
    }
}
