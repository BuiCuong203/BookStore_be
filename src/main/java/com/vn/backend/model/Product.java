package com.vn.backend.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(name = "name", nullable = false, length = 255)
    String name;

    @Column(name = "short_description", columnDefinition = "TEXT")
    String shortDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

    @Column(name = "dimension", nullable = false, length = 255)
    String dimension;

    @Column(name = "number_of_pages")
    Integer numberOfPages;

    @Column(name = "isbn", nullable = false, length = 100)
    String isbn;

    @Column(name = "stock_quantity", nullable = false)
    Integer stockQuanity;

    @Column(name = "price", nullable = false)
    Long price;

    @Column(name = "discount", nullable = false)
    Integer discount;

    @Column(name = "publisher", nullable = false, length = 255)
    String publisher;

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Author> authors = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    @Column(name = "publisher_date", nullable = false)
    LocalDateTime publisherDate;

    @Column(name = "rating_avg")
    Double ratingAvg;

    @Column(name = "rating_count")
    int ratingCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
