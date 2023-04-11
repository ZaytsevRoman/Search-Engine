package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    @Query(value = "SELECT i.* FROM `index` i WHERE i.lemma_id IN :lemmas AND i.page_id IN :pages", nativeQuery = true)
    List<Index> findIndexListByPagesAndLemmas(@Param("lemmas") List<Lemma> lemmaList,
                                              @Param("pages") List<Page> pageList);
    @Query(value = "SELECT i.* FROM `index` i WHERE i.lemma_id = :lemma AND i.page_id = :page", nativeQuery = true)
    List<Index> findIndexListByPageAndLemma(@Param("lemma") Lemma lemma,
                                            @Param("page") Page page);
}
