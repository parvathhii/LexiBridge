package com.lexibridge.repository;

import com.lexibridge.entity.ReadingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ReadingMaterialRepository extends JpaRepository<ReadingMaterial, Long> {
    List<ReadingMaterial> findByReadingLevel(Integer readingLevel);
    List<ReadingMaterial> findByCategory(String category);
    List<ReadingMaterial> findByReadingLevelAndCategory(Integer readingLevel, String category);
    List<ReadingMaterial> findByTitleContainingIgnoreCase(String query);

    @Query(value = "select m.* from reading_materials m " +
            "where (:level is null or m.reading_level = :level) " +
            "and (:category is null or lower(m.category) = lower(:category)) " +
            "and (:query is null or lower(m.title) like concat('%', lower(:query), '%') " +
            "or lower(coalesce(m.description, '')) like concat('%', lower(:query), '%') " +
            "or lower(cast(m.content as char)) like concat('%', lower(:query), '%') " +
            "or lower(coalesce(m.target_phonemes, '')) like concat('%', lower(:query), '%')) " +
            "order by m.reading_level, m.title", nativeQuery = true)
    List<ReadingMaterial> filterMaterials(@Param("level") Integer level,
                                         @Param("category") String category,
                                         @Param("query") String query);

    @Query("select distinct m.category from ReadingMaterial m where m.category is not null order by m.category")
    List<String> findDistinctCategories();
}
