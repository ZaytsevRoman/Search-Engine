package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    int countBySite(Site site);
    Iterable<Page> findBySite(Site site);
    @Query(value = "SELECT p.* FROM Page p JOIN `index` i ON p.id = i.page_id WHERE i.lemma_id = :lemma", nativeQuery = true)
    List<Page> findByLemma(@Param("lemma") Lemma lemma);
}
