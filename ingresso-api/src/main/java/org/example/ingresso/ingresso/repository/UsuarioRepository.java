package org.example.ingresso.ingresso.repository;

import org.example.ingresso.ingresso.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = "perfis")
    Optional<Usuario> findByEmail(String email);

    @EntityGraph(attributePaths = "perfis")
    Optional<Usuario> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "perfis")
    Optional<Usuario> findWithPerfisById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findWithLockById(Long id);
}
