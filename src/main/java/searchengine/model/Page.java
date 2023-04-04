package searchengine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "page", indexes = {@javax.persistence.Index(name = "path_list", columnList = "path")})
@Data
@NoArgsConstructor
public class Page implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "site_id", referencedColumnName = "id")
    private Site site;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci", nullable = false)
    private String content;
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
    private List<Index> indexesList = new ArrayList<>();

    public Page(Site site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    @Override
    public String toString() {
        return "Page{" +
                "id=" + id +
                ", siteId=" + site +
                ", path='" + path + '\'' +
                ", code=" + code +
                ", content='" + content + '\'' +
                ", indexesList=" + indexesList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page that = (Page) o;
        return id == that.id && code == that.code &&
                site.equals(that.site) &&
                path.equals(that.path) &&
                content.equals(that.content) &&
                indexesList.equals(that.indexesList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, site, path, code, content, indexesList);
    }
}
