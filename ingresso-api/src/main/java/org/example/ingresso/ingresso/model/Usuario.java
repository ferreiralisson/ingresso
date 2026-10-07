package org.example.ingresso.ingresso.model;

import org.example.ingresso.ingresso.model.enums.UsuarioPerfil;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    @ElementCollection
    @CollectionTable(name = "usuario_perfis", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "perfil", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Set<UsuarioPerfil> perfis = new HashSet<>();

    public Usuario() {
    }

    public Usuario(String nome, String email, String senha, UsuarioPerfil usuarioPerfil) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.perfis.add(usuarioPerfil);
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

    public Set<UsuarioPerfil> getPerfis() {
        return Collections.unmodifiableSet(perfis);
    }

    public void addPerfil(UsuarioPerfil usuarioPerfil) {
        perfis.add(usuarioPerfil);
    }

    public void removePerfil(UsuarioPerfil usuarioPerfil) {
        perfis.remove(usuarioPerfil);
    }
}
