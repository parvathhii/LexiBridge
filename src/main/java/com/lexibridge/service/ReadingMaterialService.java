package com.lexibridge.service;

import com.lexibridge.entity.ReadingMaterial;

import java.util.List;
import java.util.Optional;

public interface ReadingMaterialService {
    List<ReadingMaterial> getAllMaterials();
    Optional<ReadingMaterial> getMaterialById(Long id);
    List<ReadingMaterial> getMaterialsByLevel(Integer level);
    List<ReadingMaterial> getMaterialsByCategory(String category);
    List<ReadingMaterial> searchMaterials(String query);
    List<ReadingMaterial> filterMaterials(Integer level, String category, String query);
    List<String> getCategories();
    ReadingMaterial saveMaterial(ReadingMaterial material);
    void deleteMaterial(Long id);
}
