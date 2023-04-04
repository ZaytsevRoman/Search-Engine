package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Data
@Table(name = "`index`", indexes = {@javax.persistence.Index(
        name = "page_id_list", columnList = "page_id"),
        @javax.persistence.Index(name = "lemma_id_list", columnList = "lemma_id")})
@NoArgsConstructor
public class Index implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "page_id", referencedColumnName = "id")
    private Page page;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "lemma_id", referencedColumnName = "id")
    private Lemma lemma;
    @Column(nullable = false, name = "index_rank")
    private float rank;

    public Index(Page page, Lemma lemma, float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }

    @Override
    public String toString() {
        return "Index{" +
                "id=" + id +
                ", page=" + page +
                ", lemma=" + lemma +
                ", rank=" + rank +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Index that = (Index) o;
        return id == that.id && Float.compare(that.rank, rank) == 0 && page.equals(that.page)
                && lemma.equals(that.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, page, lemma, rank);
    }
}
