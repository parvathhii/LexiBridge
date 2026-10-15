package com.lexibridge.service;

import com.lexibridge.entity.ReadingMaterial;
import com.lexibridge.repository.ReadingMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ReadingMaterialServiceImpl implements ReadingMaterialService {

    private final ReadingMaterialRepository readingMaterialRepository;

    public ReadingMaterialServiceImpl(ReadingMaterialRepository readingMaterialRepository) {
        this.readingMaterialRepository = readingMaterialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingMaterial> getAllMaterials() {
        return readingMaterialRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReadingMaterial> getMaterialById(Long id) {
        return readingMaterialRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingMaterial> getMaterialsByLevel(Integer level) {
        return readingMaterialRepository.findByReadingLevel(level);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingMaterial> getMaterialsByCategory(String category) {
        return readingMaterialRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingMaterial> searchMaterials(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllMaterials();
        }
        return readingMaterialRepository.findByTitleContainingIgnoreCase(query.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReadingMaterial> filterMaterials(Integer level, String category, String query) {
        Integer selectedLevel = level != null && level >= 1 && level <= 5 ? level : null;
        String selectedCategory = category == null || category.isBlank() ? null : category.trim();
        String selectedQuery = query == null || query.isBlank() ? null : query.trim();
        return readingMaterialRepository.filterMaterials(selectedLevel, selectedCategory, selectedQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return readingMaterialRepository.findDistinctCategories();
    }

    @Override
    @Transactional
    public ReadingMaterial saveMaterial(ReadingMaterial material) {
        return readingMaterialRepository.save(material);
    }

    @Override
    @Transactional
    public void deleteMaterial(Long id) {
        readingMaterialRepository.deleteById(id);
    }
}
