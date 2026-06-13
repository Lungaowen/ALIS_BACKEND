package za.ac.alis.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import za.ac.alis.entities.LawRul;

public interface LawRuleRepository extends JpaRepository<LawRul, Long> {

    // Fetch all rules with act name eagerly (avoiding lazy issues)
    @Query("SELECT r FROM LawRul r JOIN FETCH r.act ORDER BY r.ruleId")
    List<LawRul> findAllWithAct();

    // Fetch a single rule by id with act eagerly
    @Query("SELECT r FROM LawRul r JOIN FETCH r.act WHERE r.ruleId = :id")
    Optional<LawRul> findByIdWithAct(@Param("id") Long id);
}