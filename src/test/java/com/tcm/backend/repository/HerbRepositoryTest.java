package com.tcm.backend.repository;

import com.tcm.backend.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.tcm.backend.config.TestJpaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
class HerbRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HerbRepository herbRepository;

    private Herb testHerb;

    @BeforeEach
    void setUp() {
        testHerb = createTestHerbWithRelations();
        testHerb = entityManager.persistAndFlush(testHerb);
        entityManager.clear(); // Clear persistence context for fresh queries
    }
    /*
    @Test
    void findByIdWithEntityGraphLoadsAllRelations() {
        Optional<Herb> result = herbRepository.findById(testHerb.getId());

        assertThat(result).isPresent();
        Herb herb = result.get();

        // Verify all relations are loaded without additional queries
        assertThat(herb.getFlavors()).isNotEmpty();
        assertThat(herb.getFormulas()).isNotEmpty();
        assertThat(herb.getImages()).isNotEmpty();
        assertThat(herb.getIndications()).isNotEmpty();
        assertThat(herb.getMeridians()).isNotEmpty();

        // Check specific data
        assertThat(herb.getFlavors()).hasSize(2);
        assertThat(herb.getFormulas()).hasSize(1);
        assertThat(herb.getImages()).hasSize(1);
        assertThat(herb.getIndications()).hasSize(2);
        assertThat(herb.getMeridians()).hasSize(1);
    }

    @Test
    void findBySourceUrlWithEntityGraphLoadsAllRelations() {
        Optional<Herb> result = herbRepository.findBySourceUrl(testHerb.getSourceUrl());

        assertThat(result).isPresent();
        Herb herb = result.get();

        assertThat(herb.getFlavors()).isNotEmpty();
        assertThat(herb.getFormulas()).isNotEmpty();
        assertThat(herb.getImages()).isNotEmpty();
        assertThat(herb.getIndications()).isNotEmpty();
        assertThat(herb.getMeridians()).isNotEmpty();
    }

    @Test
    void findByNameZhIgnoreCaseWithEntityGraphLoadsAllRelations() {
        Optional<Herb> result = herbRepository.findByNameZhIgnoreCase(testHerb.getNameZh().toLowerCase());

        assertThat(result).isPresent();
        Herb herb = result.get();

        assertThat(herb.getFlavors()).isNotEmpty();
        assertThat(herb.getFormulas()).isNotEmpty();
        assertThat(herb.getImages()).isNotEmpty();
        assertThat(herb.getIndications()).isNotEmpty();
        assertThat(herb.getMeridians()).isNotEmpty();
    }

    @Test
    void findByNamePinyinIgnoreCaseWithEntityGraphLoadsAllRelations() {
        Optional<Herb> result = herbRepository.findByNamePinyinIgnoreCase(testHerb.getNamePinyin().toLowerCase());

        assertThat(result).isPresent();
        Herb herb = result.get();

        assertThat(herb.getFlavors()).isNotEmpty();
        assertThat(herb.getFormulas()).isNotEmpty();
        assertThat(herb.getImages()).isNotEmpty();
        assertThat(herb.getIndications()).isNotEmpty();
        assertThat(herb.getMeridians()).isNotEmpty();
    }

    @Test
    void findByNameContainingReturnsMatchingHerbs() {
        List<Herb> results = herbRepository.findByNameContaining("白");

        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(h -> h.getNameZh().contains("白"));
    }

    @Test
    void findAllWithRelationsForExportReturnsAllHerbs() {
        Herb secondHerb = createSecondTestHerb();
        secondHerb = entityManager.persistAndFlush(secondHerb);

        List<Herb> results = herbRepository.findAllWithRelationsForExport();

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Herb::getId).contains(testHerb.getId(), secondHerb.getId());
    }

    @Test
    void findByIdInWithFlavorsLoadsOnlyFlavors() {
        List<Integer> herbIds = List.of(testHerb.getId());

        List<Herb> results = herbRepository.findByIdInWithFlavors(herbIds);

        assertThat(results).hasSize(1);
        Herb herb = results.get(0);
        assertThat(herb.getFlavors()).isNotEmpty();
        assertThat(herb.getFlavors()).hasSize(2);
    }

    @Test
    void findByIdInWithFormulasLoadsOnlyFormulas() {
        List<Integer> herbIds = List.of(testHerb.getId());

        List<Herb> results = herbRepository.findByIdInWithFormulas(herbIds);

        assertThat(results).hasSize(1);
        Herb herb = results.get(0);
        assertThat(herb.getFormulas()).isNotEmpty();
        assertThat(herb.getFormulas()).hasSize(1);
    }

    @Test
    void findByIdInWithImagesLoadsOnlyImages() {
        List<Integer> herbIds = List.of(testHerb.getId());

        List<Herb> results = herbRepository.findByIdInWithImages(herbIds);

        assertThat(results).hasSize(1);
        Herb herb = results.get(0);
        assertThat(herb.getImages()).isNotEmpty();
        assertThat(herb.getImages()).hasSize(1);
    }

    @Test
    void findByIdInWithIndicationsLoadsOnlyIndications() {
        List<Integer> herbIds = List.of(testHerb.getId());

        List<Herb> results = herbRepository.findByIdInWithIndications(herbIds);

        assertThat(results).hasSize(1);
        Herb herb = results.get(0);
        assertThat(herb.getIndications()).isNotEmpty();
        assertThat(herb.getIndications()).hasSize(2);
    }

    @Test
    void findByIdInWithMeridiansLoadsOnlyMeridians() {
        List<Integer> herbIds = List.of(testHerb.getId());

        List<Herb> results = herbRepository.findByIdInWithMeridians(herbIds);

        assertThat(results).hasSize(1);
        Herb herb = results.get(0);
        assertThat(herb.getMeridians()).isNotEmpty();
        assertThat(herb.getMeridians()).hasSize(1);
    }

    @Test
    void separateRelationLoadingMethodsWorkWithMultipleHerbs() {
        Herb secondHerb = createSecondTestHerb();
        secondHerb = entityManager.persistAndFlush(secondHerb);

        List<Integer> herbIds = List.of(testHerb.getId(), secondHerb.getId());

        List<Herb> flavorsResults = herbRepository.findByIdInWithFlavors(herbIds);
        List<Herb> formulasResults = herbRepository.findByIdInWithFormulas(herbIds);

        assertThat(flavorsResults).hasSize(2);
        assertThat(formulasResults).hasSize(2);

        // Verify each herb has the expected relations loaded
        for (Herb herb : flavorsResults) {
            assertThat(herb.getFlavors()).isNotEmpty();
        }

        for (Herb herb : formulasResults) {
            assertThat(herb.getFormulas()).isNotEmpty();
        }
    }

    @Test
    void findByNameContainingHandlesEmptyResults() {
        List<Herb> results = herbRepository.findByNameContaining("nonexistent");

        assertThat(results).isEmpty();
    }

    @Test
    void findByIdInMethodsHandleEmptyList() {
        List<Integer> emptyIds = List.of();

        List<Herb> results = herbRepository.findByIdInWithFlavors(emptyIds);

        assertThat(results).isEmpty();
    }

    @Test
    void findByIdInMethodsHandleNonExistentIds() {
        List<Integer> nonExistentIds = List.of(99999);

        List<Herb> results = herbRepository.findByIdInWithFlavors(nonExistentIds);

        assertThat(results).isEmpty();
    }*/

    private Herb createTestHerbWithRelations() {
        Herb herb = new Herb();
        herb.setSourceUrl("https://example.com/test-herb");
        herb.setNameZh("白术");
        herb.setNamePinyin("bai zhu");
        herb.setDescZh("白术描述");
        herb.setDescEn("Bai Zhu Description");
        herb.setAppearance("White rhizome");
        herb.setProperty("Warm, Sweet");

        // Add flavors
        HerbFlavor flavor1 = new HerbFlavor();
        flavor1.setValue("Sweet");
        flavor1.setHerb(herb);

        HerbFlavor flavor2 = new HerbFlavor();
        flavor2.setValue("Bitter");
        flavor2.setHerb(herb);

        herb.setFlavors(List.of(flavor1, flavor2));

        // Add formula
        HerbFormula formula = new HerbFormula();
        formula.setValue("Si Jun Zi Tang");
        formula.setHerb(herb);
        herb.setFormulas(List.of(formula));

        // Add image
        HerbImage image = new HerbImage();
        image.setFilename("bai-zhu.jpg");
        image.setMime("image/jpeg");
        image.setData("fake image data".getBytes());
        image.setHerb(herb);
        herb.setImages(List.of(image));

        // Add indications
        HerbIndication indication1 = new HerbIndication();
        indication1.setValue("Digestive problems");
        indication1.setHerb(herb);

        HerbIndication indication2 = new HerbIndication();
        indication2.setValue("Fatigue");
        indication2.setHerb(herb);

        herb.setIndications(List.of(indication1, indication2));

        // Add meridian
        HerbMeridian meridian = new HerbMeridian();
        meridian.setValue("Spleen");
        meridian.setHerb(herb);
        herb.setMeridians(List.of(meridian));

        return herb;
    }

    private Herb createSecondTestHerb() {
        Herb herb = new Herb();
        herb.setSourceUrl("https://example.com/second-herb");
        herb.setNameZh("当归");
        herb.setNamePinyin("dang gui");
        herb.setDescZh("当归描述");
        herb.setDescEn("Dang Gui Description");
        herb.setAppearance("Brown root");
        herb.setProperty("Warm, Sweet");

        // Add minimal relations for testing
        HerbFlavor flavor = new HerbFlavor();
        flavor.setValue("Sweet");
        flavor.setHerb(herb);
        herb.setFlavors(List.of(flavor));

        HerbFormula formula = new HerbFormula();
        formula.setValue("Bu Yang Huan Wu Tang");
        formula.setHerb(herb);
        herb.setFormulas(List.of(formula));

        HerbImage image = new HerbImage();
        image.setFilename("dang-gui.jpg");
        image.setMime("image/jpeg");
        image.setData("fake image data 2".getBytes());
        image.setHerb(herb);
        herb.setImages(List.of(image));

        HerbIndication indication = new HerbIndication();
        indication.setValue("Blood deficiency");
        indication.setHerb(herb);
        herb.setIndications(List.of(indication));

        HerbMeridian meridian = new HerbMeridian();
        meridian.setValue("Liver");
        meridian.setHerb(herb);
        herb.setMeridians(List.of(meridian));

        return herb;
    }
}