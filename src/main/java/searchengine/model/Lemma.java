package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Data
@Table(name = "lemma", indexes = {@javax.persistence.Index(name = "lemma_list", columnList = "lemma")})
@NoArgsConstructor
public class Lemma implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "site_id", referencedColumnName = "id")
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;
    @Column(nullable = false)
    private int frequency;
    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<Index> indexesList = new ArrayList<>();

    public Lemma(String lemma, int frequency, Site site) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.site = site;
    }

    @Override
    public String toString() {
        return "Lemma{" +
                "id=" + id +
                ", site=" + site +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                ", indexesList=" + indexesList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lemma that = (Lemma) o;
        return id == that.id && frequency == that.frequency &&
                site.equals(that.site) &&
                lemma.equals(that.lemma) &&
                indexesList.equals(that.indexesList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, lemma, frequency, indexesList);
    }
}
